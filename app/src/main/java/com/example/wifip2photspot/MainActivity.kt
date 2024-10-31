package com.example.wifip2photspot

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat

import android.net.wifi.p2p.WifiP2pDevice
import androidx.compose.material3.Switch


class MainActivity : ComponentActivity() {

    private var isWifiP2pEnabled = false

    // Make isHotspotEnabled observable by using mutable state
    private var isHotspotEnabled by mutableStateOf(false)

    private lateinit var manager: WifiP2pManager
    private lateinit var channel: WifiP2pManager.Channel

    private lateinit var receiver: BroadcastReceiver
    private lateinit var intentFilter: IntentFilter

    // Use MutableStateList to track connected devices
    private val connectedDevices = mutableStateListOf<WifiP2pDevice>()

    private var updateLogCallback: ((String) -> Unit)? = null

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Wi-Fi P2P manager and channel
        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager.initialize(this, mainLooper, null)

        // Register the broadcast receiver
        intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            // Add other actions if needed
        }
        receiver = WifiDirectBroadcastReceiver(manager, channel, this)

        // Request necessary permissions
        ActivityCompat.requestPermissions(this, arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.NEARBY_WIFI_DEVICES
        ), 0)

        setContent {
            WiFiP2PHotspotApp()
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(receiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }

    fun setIsWifiP2pEnabled(enabled: Boolean) {
        isWifiP2pEnabled = enabled
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    @RequiresApi(Build.VERSION_CODES.Q)
    @Composable
    fun WiFiP2PHotspotApp() {
        var ssidInput by remember { mutableStateOf("") }
        var passwordInput by remember { mutableStateOf("") }
        var logMessages by remember { mutableStateOf("") }
        var selectedBand by remember { mutableStateOf("Auto") }

        val bands = listOf("Auto", "2.4GHz", "5GHz")

        // Convert connectedDevices to a stable list for recomposition
        val devices by remember { derivedStateOf { connectedDevices.toList() } }

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Wi-Fi P2P Hotspot") })
            },
            content = {
                Column(modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)) {

                    // Input fields are only enabled when the hotspot is not enabled
                    OutlinedTextField(
                        value = ssidInput,
                        onValueChange = { ssidInput = it },
                        label = { Text("SSID") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isHotspotEnabled
                    )

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        enabled = !isHotspotEnabled
                    )

                    Text("Select Band:")
                    bands.forEach { band ->
                        Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedBand == band,
                                onClick = { selectedBand = band },
                                enabled = !isHotspotEnabled
                            )
                            Text(text = band)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Toggle Switch
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Start Wi-Fi Hotspot")
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(
                            checked = isHotspotEnabled,
                            onCheckedChange = { isChecked ->
                                if (isChecked && !isHotspotEnabled) {
                                    // Start the hotspot
                                    onButtonStartTapped(
                                        ssidInput,
                                        passwordInput,
                                        selectedBand,
                                        { message -> updateLog(message) }
                                    )
                                } else if (!isChecked && isHotspotEnabled) {
                                    // Stop the hotspot
                                    onButtonStopTapped { message -> updateLog(message) }
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Connected Devices:")
                    devices.forEach { device ->
                        Text("Device Name: ${device.deviceName}, Address: ${device.deviceAddress}")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Log:")
                    Text(logMessages)
                }
            }
        )

        // Set the callback to update the log
        DisposableEffect(Unit) {
            updateLogCallback = { message ->
                logMessages += message
            }
            onDispose {
                updateLogCallback = null
            }
        }
    }

    private fun updateLog(message: String) {
        updateLogCallback?.invoke(message)
    }

    fun onDevicesChanged(deviceList: Collection<WifiP2pDevice>) {
        // Update the list of connected devices
        connectedDevices.clear()
        connectedDevices.addAll(deviceList)

        // Update the log
        val deviceInfo = if (deviceList.isNotEmpty()) {
            deviceList.joinToString(separator = "\n") { device ->
                "Device Name: ${device.deviceName}, Address: ${device.deviceAddress}"
            }
        } else {
            "No devices connected."
        }

        val logMessage = "Connected Devices:\n$deviceInfo\n"

        // Update the log on the main thread
        runOnUiThread {
            updateLog(logMessage)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun onButtonStartTapped(
        ssidInput: String,
        passwordInput: String,
        selectedBand: String,
        outputLog: (String) -> Unit
    ) {
        if (!isWifiP2pEnabled) {
            outputLog("Error: Cannot start hotspot. Wi-Fi P2P is not enabled.\n")
            Toast.makeText(this, "Wi-Fi P2P is not enabled.", Toast.LENGTH_SHORT).show()
            // Reset the switch to off
            isHotspotEnabled = false
            return
        }

        val ssidTrimmed = ssidInput.trim()
        val passwordTrimmed = passwordInput.trim()

        if (ssidTrimmed.isEmpty()) {
            outputLog("Error: SSID cannot be empty.\n")
            Toast.makeText(this, "SSID cannot be empty.", Toast.LENGTH_SHORT).show()
            // Reset the switch to off
            isHotspotEnabled = false
            return
        }

        if (passwordTrimmed.length !in 8..63) {
            outputLog("Error: The length of a passphrase must be between 8 and 63.\n")
            Toast.makeText(this, "Password must be between 8 and 63 characters.", Toast.LENGTH_SHORT).show()
            // Reset the switch to off
            isHotspotEnabled = false
            return
        }

        val ssid = "DIRECT-hs-$ssidTrimmed"
        val password = passwordTrimmed

        val band = when (selectedBand) {
            "2.4GHz" -> WifiP2pConfig.GROUP_OWNER_BAND_2GHZ
            "5GHz" -> WifiP2pConfig.GROUP_OWNER_BAND_5GHZ
            else -> WifiP2pConfig.GROUP_OWNER_BAND_AUTO
        }

        val config = WifiP2pConfig.Builder()
            .setNetworkName(ssid)
            .setPassphrase(password)
            .enablePersistentMode(false)
            .setGroupOperatingBand(band)
            .build()

        try {
            manager.createGroup(channel, config, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    outputLog("Hotspot started successfully.\n")
                    Toast.makeText(this@MainActivity, "Hotspot started successfully.", Toast.LENGTH_SHORT).show()
                    isHotspotEnabled = true
                    outputLog("------------------- Hotspot Info -------------------\n")
                    outputLog("SSID: $ssid\n")
                    outputLog("Password: $password\n")
                    val bandStr = when (band) {
                        WifiP2pConfig.GROUP_OWNER_BAND_2GHZ -> "2.4GHz"
                        WifiP2pConfig.GROUP_OWNER_BAND_5GHZ -> "5GHz"
                        else -> "Auto"
                    }
                    outputLog("Band: $bandStr\n")
                    outputLog("---------------------------------------------------\n")
                }

                override fun onFailure(reason: Int) {
                    val reasonStr = when (reason) {
                        WifiP2pManager.ERROR -> "General error"
                        WifiP2pManager.P2P_UNSUPPORTED -> "P2P Unsupported"
                        WifiP2pManager.BUSY -> "System is busy"
                        else -> "Unknown error"
                    }
                    outputLog("Failed to start hotspot. Reason: $reasonStr\n")
                    Toast.makeText(this@MainActivity, "Failed to start hotspot: $reasonStr", Toast.LENGTH_SHORT).show()
                    // Reset the switch to off
                    isHotspotEnabled = false
                }
            })
        } catch (e: Exception) {
            outputLog("Exception: ${e.message}\n")
            Toast.makeText(this, "Exception occurred: ${e.message}", Toast.LENGTH_SHORT).show()
            // Reset the switch to off
            isHotspotEnabled = false
        }
    }

    fun onButtonStopTapped(outputLog: (String) -> Unit) {
        if (!isHotspotEnabled) {
            outputLog("Error: Hotspot is not enabled.\n")
            Toast.makeText(this, "Hotspot is not enabled.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    outputLog("Hotspot stopped successfully.\n")
                    Toast.makeText(this@MainActivity, "Hotspot stopped successfully.", Toast.LENGTH_SHORT).show()
                    isHotspotEnabled = false
                    // Clear the connected devices list
                    connectedDevices.clear()
                }

                override fun onFailure(reason: Int) {
                    val reasonStr = when (reason) {
                        WifiP2pManager.ERROR -> "General error"
                        WifiP2pManager.P2P_UNSUPPORTED -> "P2P Unsupported"
                        WifiP2pManager.BUSY -> "System is busy"
                        else -> "Unknown error"
                    }
                    outputLog("Failed to stop hotspot. Reason: $reasonStr\n")
                    Toast.makeText(this@MainActivity, "Failed to stop hotspot: $reasonStr", Toast.LENGTH_SHORT).show()
                    // Set the switch back to on since we failed to stop the hotspot
                    isHotspotEnabled = true
                }
            })
        } catch (e: Exception) {
            outputLog("Exception: ${e.message}\n")
            Toast.makeText(this, "Exception occurred: ${e.message}", Toast.LENGTH_SHORT).show()
            // Set the switch back to on since we failed to stop the hotspot
            isHotspotEnabled = true
        }
    }
}
