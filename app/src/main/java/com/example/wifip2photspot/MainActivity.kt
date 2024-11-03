package com.example.wifip2photspot

import HotspotViewModelFactory
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    private val Context.dataStore by preferencesDataStore(name = "settings")


    private var isWifiP2pEnabled = false

    // Make isHotspotEnabled observable by using mutable state
    private var isHotspotEnabled by mutableStateOf(false)

    private lateinit var manager: WifiP2pManager
    private lateinit var channel: WifiP2pManager.Channel

    private lateinit var intentFilter: IntentFilter
    private var isProcessing by mutableStateOf(false)

    private lateinit var wifiP2pManager: WifiP2pManager


    private lateinit var viewModel: HotspotViewModel

    private lateinit var receiver: WifiDirectBroadcastReceiver


    // Initialize DataStore

    private lateinit var hotspotStatusReceiver: WifiDirectBroadcastReceiver

    // Permission request launcher
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        var allGranted = true
        permissions.entries.forEach {
            if (!it.value) {
                allGranted = false
            }
        }
        if (!allGranted) {
            // Handle the case where permissions are not granted
            Log.e("Permissions", "Required permissions not granted.")
        } else {
            // Permissions granted, proceed with Wi-Fi P2P operations
            Log.d("Permissions", "All required permissions granted.")
        }
    }


    // Use MutableStateList to track connected devices
    private val connectedDevices = mutableStateListOf<WifiP2pDevice>()

    private var updateLogCallback: ((String) -> Unit)? = null

    @RequiresApi(Build.VERSION_CODES.Q) // Ensure compatibility with required Android version
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // Initialize ViewModel
        viewModel = ViewModelProvider(this).get(HotspotViewModel::class.java)

        // Initialize Wi-Fi P2P Manager
        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = wifiP2pManager.initialize(this, mainLooper, null)

        // Setup BroadcastReceiver
        receiver = WifiDirectBroadcastReceiver(wifiP2pManager, channel, this, viewModel)
        intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }

        // Initialize WifiP2pManager
        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager.initialize(this, mainLooper, null)

        // Initialize ViewModel using ViewModelProvider
        viewModel = ViewModelProvider(this).get(HotspotViewModel::class.java)

        // Initialize BroadcastReceiver with the ViewModel instance
        receiver = WifiDirectBroadcastReceiver(manager, channel, this, viewModel)

        // Set up IntentFilter
        intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }

        // Register the BroadcastReceiver
        registerReceiver(receiver, intentFilter)

//
        // Request necessary permissions
        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ), 0
        )

        // Provide the ViewModel with DataStore
        setContent {
            val viewModel: HotspotViewModel = ViewModelProvider(
                this,
                HotspotViewModelFactory(dataStore)
            ).get(HotspotViewModel::class.java)

            WiFiP2PHotspotApp(viewModel = viewModel)
        }

        // Assuming you have a list to manage connected devices
        val connectedDevices = mutableStateListOf<WifiP2pDevice>()
    // Check and request necessary permissions
//        if (!hasPermissions()) {
//            requestPermissionsLauncher.launch(requiredPermissions)
//        }

//        registerReceiver(hotspotStatusReceiver, intentFilter)
}


//    // Function to check if all required permissions are granted
//    private fun hasPermissions(): Boolean {
//        return requiredPermissions.all {
//            ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
//        }
//    }
//
//    companion object {
//        private val requiredPermissions = arrayOf(
//            Manifest.permission.ACCESS_WIFI_STATE,
//            Manifest.permission.CHANGE_WIFI_STATE,
//            Manifest.permission.ACCESS_FINE_LOCATION,
//            Manifest.permission.ACCESS_COARSE_LOCATION,
//            Manifest.permission.INTERNET
//        )
//    }

override fun onDestroy() {
    super.onDestroy()
    unregisterReceiver(hotspotStatusReceiver)
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

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun WiFiP2PHotspotApp(viewModel: HotspotViewModel) {
    // Collect SSID and Password from ViewModel
    val ssid by viewModel.ssid.collectAsState()
    val password by viewModel.password.collectAsState()


    var logMessages by remember { mutableStateOf("") }
    var selectedBand by remember { mutableStateOf("Auto") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var isHotspotEnabled by remember { mutableStateOf(false) }


    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val mainActivity = LocalContext.current as MainActivity
    val devices = mainActivity.connectedDevices
//    val errorMessage by viewModel.errorMessage.collectAsState()


    val context = LocalContext.current
    val connectedDevices = (context as? MainActivity)?.connectedDevices ?: emptyList()

//        val context = LocalContext.current
//        val connectedDevices = viewModel.connectedDevices.collectAsState().value

    // Collect upload, download speed and total download from ViewModel
    val uploadSpeed by viewModel.uploadSpeed.collectAsState()
    val downloadSpeed by viewModel.downloadSpeed.collectAsState()
    val totalDownload by viewModel.totalDownload.collectAsState()
    val previousDeviceCount = remember { mutableIntStateOf(devices.size) }

    // Collect hotspot status from ViewModel
//        val isHotspotEnabled by viewModel.isHotspotEnabledState.collectAsState()

    // Function to update logs
    val updateLog: (String) -> Unit = { message ->
        logMessages += "$message\n"
    }




    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { ImprovedHeader(isHotspotEnabled = isHotspotEnabled) },
        content = { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                if (connectedDevices.isEmpty()) {
                    item {
                        // Input Fields Section
                        InputFieldsSection(
                            ssidInput = ssid,
                            onSsidChange = { newSSID ->
                                viewModel.updateSSID(newSSID)
                            },
                            passwordInput = password,
                            onPasswordChange = { newPassword ->
                                viewModel.updatePassword(newPassword)
                            },
                            passwordVisible = passwordVisible,
                            onPasswordVisibilityChange = { passwordVisible = it },
                            isHotspotEnabled = isHotspotEnabled
                        )
                    }

                    item {
                        // Band Selection Section
                        BandSelection(
                            selectedBand = selectedBand,
                            onBandSelected = { selectedBand = it },
                            bands = listOf("Auto", "2.4GHz", "5GHz"),
                            isHotspotEnabled = isHotspotEnabled
                        )
                    }
                } else {
                    item {
                        // Connection Status Bar with Updated Parameters
                        ConnectionStatusBar(
                            uploadSpeed = uploadSpeed,
                            downloadSpeed = downloadSpeed,
                            totalDownload = totalDownload,
                            connectedDevicesCount = connectedDevices.size
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    // Hotspot Control Section
                    HotspotControlSection(
                        isHotspotEnabled = isHotspotEnabled,
                        isProcessing = isProcessing,
                        ssidInput = ssid,
                        passwordInput = password,
                        selectedBand = selectedBand,
                        onStartTapped = {
                            coroutineScope.launch {
                                onButtonStartTapped(
                                    ssid,
                                    password,
                                    selectedBand,
                                    outputLog = { message ->
                                        updateLog(message)
                                        isHotspotEnabled = true
                                        isProcessing = false
                                    }
                                )
                            }
                        },
                        onStopTapped = {
                            isProcessing = true
                            coroutineScope.launch {
                                onButtonStopTapped { message ->
                                    updateLog(message)
                                    isHotspotEnabled = false
                                    isProcessing = false
                                }

                            }
                        }
                    )
                }

                if (connectedDevices.isNotEmpty()) {
                    item {
                        // Connected Devices Section
                        ConnectedDevicesSection(
                            devices = connectedDevices,
                            onDeviceClick = { device ->
                                // Handle device click (e.g., show details or disconnect)
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    // Logs Section
                    LogSection(logMessages = logMessages)
                }
            }
        }

    )
    // Observe changes in connectedDevices and show snackbars
    LaunchedEffect(devices.size) {
        if (devices.size > previousDeviceCount.value) {
            // A device has connected
            coroutineScope.launch {
                snackbarHostState.showSnackbar("A device has connected.")
                updateLog("Device connected. Total devices: ${devices.size}")
            }
        } else if (devices.size < previousDeviceCount.value) {
            // A device has disconnected
            coroutineScope.launch {
                snackbarHostState.showSnackbar("A device has disconnected.")
                updateLog("Device disconnected. Total devices: ${devices.size}")
            }
        }
        previousDeviceCount.value = devices.size
    }

    // Observe error messages and show snackbars
//    LaunchedEffect(errorMessage) {
//        errorMessage?.let { message ->
//            coroutineScope.launch {
//                snackbarHostState.showSnackbar(message)
//                viewModel.clearError()
//            }
//        }
//    }
}


fun onDevicesChanged(deviceList: Collection<WifiP2pDevice>) {
    connectedDevices.clear()
    connectedDevices.addAll(deviceList)
    // Log for debugging
    val deviceInfo = deviceList.joinToString(separator = "\n") { device ->
        "Device Name: ${device.deviceName}, Address: ${device.deviceAddress}"
    }
    Log.d("ConnectedDevices", "Connected Devices:\n$deviceInfo")
}


private fun updateLog(message: String) {
    updateLogCallback?.invoke(message)
}

@RequiresApi(Build.VERSION_CODES.Q)
fun onButtonStartTapped(
    ssidInput: String, passwordInput: String, selectedBand: String, outputLog: (String) -> Unit
) {

    if (!isWifiP2pEnabled) {
        outputLog("Error: Cannot start hotspot. Wi-Fi P2P is not enabled.\n")
        Toast.makeText(this, "Wi-Fi P2P is not enabled.", Toast.LENGTH_SHORT).show()
        // Reset the switch to off
        isHotspotEnabled = false
        return

    }
    isProcessing = true // Start processing

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
        Toast.makeText(
            this, "Password must be between 8 and 63 characters.", Toast.LENGTH_SHORT
        ).show()
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

    val config = WifiP2pConfig.Builder().setNetworkName(ssid).setPassphrase(password)
        .enablePersistentMode(false).setGroupOperatingBand(band).build()

    try {
        manager.createGroup(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                outputLog("Hotspot started successfully.\n")
                Toast.makeText(
                    this@MainActivity, "Hotspot started successfully.", Toast.LENGTH_SHORT
                ).show()
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
                Toast.makeText(
                    this@MainActivity, "Failed to start hotspot: $reasonStr", Toast.LENGTH_SHORT
                ).show()
                // Reset the switch to off
                isProcessing = false
                isHotspotEnabled = false
            }
        })
    } catch (e: Exception) {
        outputLog("Exception: ${e.message}\n")
        Toast.makeText(this, "Exception occurred: ${e.message}", Toast.LENGTH_SHORT).show()
        // Reset the switch to off
        isProcessing = false
        isHotspotEnabled = true
        outputLog("Hotspot started.")
    }

}


private fun onButtonStopTapped(outputLog: (String) -> Unit) {
    if (!isHotspotEnabled) {
        outputLog("Error: Hotspot is not enabled.\n")
        Toast.makeText(this, "Hotspot is not enabled.", Toast.LENGTH_SHORT).show()
        return
    }
    isProcessing = true // Start processing

    CoroutineScope(Dispatchers.IO).launch {

        try {
            manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    outputLog("Hotspot stopped successfully.\n")
                    Toast.makeText(
                        this@MainActivity, "Hotspot stopped successfully.", Toast.LENGTH_SHORT
                    ).show()
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
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to stop hotspot: $reasonStr",
                        Toast.LENGTH_SHORT
                    ).show()
                    // Set the switch back to on since we failed to stop the hotspot
                    isProcessing = false
                    isHotspotEnabled = false
                    outputLog("Hotspot stopped.")
                }
            })
        } catch (e: Exception) {
//                outputLog("Exception: ${e.message}\n")
            outputLog("Failed to start hotspot: ${e.message}")

//                Toast.makeText(this, "Exception occurred: ${e.message}", Toast.LENGTH_SHORT).show()
            // Set the switch back to on since we failed to stop the hotspot
            isProcessing = false
            isHotspotEnabled = true
            e.printStackTrace()
        }
    }
}
}
