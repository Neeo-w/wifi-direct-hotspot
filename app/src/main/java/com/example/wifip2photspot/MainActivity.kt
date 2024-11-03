package com.example.wifip2photspot

import HotspotViewModelFactory
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat

import android.net.wifi.p2p.WifiP2pDevice
import android.util.Log
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {

    private lateinit var viewModel: HotspotViewModel
    private val Context.dataStore by preferencesDataStore(name = "settings")

    private var isWifiP2pEnabled = false

    // Make isHotspotEnabled observable by using mutable state
    private var isHotspotEnabled by mutableStateOf(false)

    private lateinit var manager: WifiP2pManager
    private lateinit var channel: WifiP2pManager.Channel

    private lateinit var receiver: BroadcastReceiver
    private lateinit var intentFilter: IntentFilter
    private var isProcessing by mutableStateOf(false)



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
        val ssid by viewModel.ssid.collectAsState()
        val password by viewModel.password.collectAsState()
        val uploadSpeed by viewModel.uploadSpeed.collectAsState()
        val downloadSpeed by viewModel.downloadSpeed.collectAsState()
        val totalDownload by viewModel.totalDownload.collectAsState()

        var isHotspotEnabled by remember { mutableStateOf(false) }
        var ssidInput by remember { mutableStateOf("") }
        var passwordInput by remember { mutableStateOf("") }
        var logMessages by remember { mutableStateOf("") }
        var selectedBand by remember { mutableStateOf("Auto") }
        var ssidError by remember { mutableStateOf(false) }
        var passwordError by remember { mutableStateOf(false) }
        var passwordVisible by remember { mutableStateOf(false) }
        val snackbarHostState = remember { SnackbarHostState() }
        val coroutineScope = rememberCoroutineScope()
        var isProcessing by remember { mutableStateOf(false) }

        val mainActivity = LocalContext.current as MainActivity
        val devices = mainActivity.connectedDevices

        val previousDeviceCount = remember { mutableIntStateOf(devices.size) }


        val bands = listOf("Auto", "2.4GHz", "5GHz")
        // Convert connectedDevices to a stable list for recomposition
//        val devices by remember { derivedStateOf { connectedDevices.toList() } }

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

                    // Observe changes in connectedDevices
                    LaunchedEffect(connectedDevices.size) {
                        if (connectedDevices.size > previousDeviceCount.value) {
                            // A device has connected
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("A device has connected.")
                            }
                        } else if (connectedDevices.size < previousDeviceCount.value) {
                            // A device has disconnected
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("A device has disconnected.")
                            }
                        }
                        previousDeviceCount.value = connectedDevices.size
                    }


            }
        )
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
                this,
                "Password must be between 8 and 63 characters.",
                Toast.LENGTH_SHORT
            ).show()
            // Reset the switch to off
            isHotspotEnabled = false
            return
        }

        val ssid = "DIRECT-hs-$ssidTrimmed"

        val band = when (selectedBand) {
            "2.4GHz" -> WifiP2pConfig.GROUP_OWNER_BAND_2GHZ
            "5GHz" -> WifiP2pConfig.GROUP_OWNER_BAND_5GHZ
            else -> WifiP2pConfig.GROUP_OWNER_BAND_AUTO
        }

        val config = WifiP2pConfig.Builder()
            .setNetworkName(ssid)
            .setPassphrase(passwordTrimmed)
            .enablePersistentMode(false)
            .setGroupOperatingBand(band)
            .build()

        try {
            manager.createGroup(channel, config, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    outputLog("Hotspot started successfully.\n")
                    Toast.makeText(
                        this@MainActivity,
                        "Hotspot started successfully.",
                        Toast.LENGTH_SHORT
                    ).show()
                    isHotspotEnabled = true
                    outputLog("------------------- Hotspot Info -------------------\n")
                    outputLog("SSID: $ssid\n")
                    outputLog("Password: $passwordTrimmed\n")
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
                        this@MainActivity,
                        "Failed to start hotspot: $reasonStr",
                        Toast.LENGTH_SHORT
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
            isHotspotEnabled = false
        }
    }


    private fun onButtonStopTapped(outputLog: (String) -> Unit) {
        if (!isHotspotEnabled) {
            outputLog("Error: Hotspot is not enabled.\n")
            Toast.makeText(this, "Hotspot is not enabled.", Toast.LENGTH_SHORT).show()
            return
        }
        isProcessing = true // Start processing


        try {
            manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    outputLog("Hotspot stopped successfully.\n")
                    Toast.makeText(
                        this@MainActivity,
                        "Hotspot stopped successfully.",
                        Toast.LENGTH_SHORT
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
                    isHotspotEnabled = true
                }
            })
        } catch (e: Exception) {
            outputLog("Exception: ${e.message}\n")
            Toast.makeText(this, "Exception occurred: ${e.message}", Toast.LENGTH_SHORT).show()
            // Set the switch back to on since we failed to stop the hotspot
            isProcessing = false
            isHotspotEnabled = true
            e.printStackTrace()
        }
    }
}
//
//@Composable
//fun ConnectedDevicesSection(
//    devices: List<WifiP2pDevice>,
//    onDeviceClick: (WifiP2pDevice) -> Unit = {}
//) {
//    Text(
//        text = "Connected Devices (${devices.size}):",
//        style = MaterialTheme.typography.titleMedium,
//        modifier = Modifier.padding(vertical = 8.dp)
//    )
//
//    if (devices.isEmpty()) {
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(32.dp),
//            contentAlignment = Alignment.Center
//        ) {
//            Text(
//                text = "No devices connected.",
//                style = MaterialTheme.typography.bodyLarge,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//            )
//        }
//    } else {
//        LazyColumn(
//            modifier = Modifier
//                .fillMaxWidth()
//                .heightIn(max = 300.dp)
//        ) {
//            items(devices) { device ->
//                DeviceItem(device = device, onClick = onDeviceClick)
//            }
//        }
//    }
//}


//
//@Composable
//fun DeviceItem(
//    device: WifiP2pDevice,
//    onClick: (WifiP2pDevice) -> Unit = {}
//) {
//    Card(
//        elevation = CardDefaults.cardElevation(2.dp),
//        shape = MaterialTheme.shapes.medium,
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(vertical = 4.dp)
//            .clickable { onClick(device) }
//    ) {
//        Row(
//            verticalAlignment = Alignment.CenterVertically,
//            modifier = Modifier.padding(12.dp)
//        ) {
//            Icon(
//                imageVector = Icons.Default.Smartphone,
//                contentDescription = "Device Icon",
//                tint = MaterialTheme.colorScheme.primary,
//                modifier = Modifier.size(40.dp)
//            )
//            Spacer(modifier = Modifier.width(16.dp))
//            Column(
//                verticalArrangement = Arrangement.Center,
//                modifier = Modifier.weight(1f)
//            ) {
//                Text(
//                    text = device.deviceName.ifBlank { "Unknown Device" },
//                    style = MaterialTheme.typography.titleMedium,
//                    color = MaterialTheme.colorScheme.onSurface
//                )
//                Text(
//                    text = "Address: ${device.deviceAddress}",
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//            }
//        }
//    }
//}


//
//fun getDeviceStatus(status: Int): String {
//    return when (status) {
//        WifiP2pDevice.AVAILABLE -> "Available"
//        WifiP2pDevice.INVITED -> "Invited"
//        WifiP2pDevice.CONNECTED -> "Connected"
//        WifiP2pDevice.FAILED -> "Failed"
//        WifiP2pDevice.UNAVAILABLE -> "Unavailable"
//        else -> "Unknown"
//    }
//}




//@Composable
//fun HotspotControlSection(
//    isHotspotEnabled: Boolean,
//    isProcessing: Boolean,
//    ssidInput: String,
//    passwordInput: String,
//    selectedBand: String,
//    onStartTapped: () -> Unit,
//    onStopTapped: () -> Unit
//) {
//    Spacer(modifier = Modifier.height(16.dp))
//
//    // Hotspot Control Section
//    Card(
//        elevation = CardDefaults.cardElevation(4.dp),
//        shape = MaterialTheme.shapes.medium,
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(vertical = 8.dp)
//    ) {
//        Column(
//            modifier = Modifier.padding(16.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            // Status Text with Icon
//            Row(
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.Center
//            ) {
//                val statusIcon = when {
//                    isProcessing -> Icons.Default.Sync
//                    isHotspotEnabled -> Icons.Default.Wifi
//                    else -> Icons.Default.WifiOff
//                }
//                val statusText = when {
//                    isProcessing -> if (isHotspotEnabled) "Stopping hotspot..." else "Starting hotspot..."
//                    isHotspotEnabled -> "Hotspot is active"
//                    else -> "Hotspot is inactive"
//                }
//                Icon(
//                    imageVector = statusIcon,
//                    contentDescription = statusText,
//                    tint = if (isHotspotEnabled) Color(0xFF4CAF50) else Color(0xFFF44336) // Green or Red
//                )
//                Spacer(modifier = Modifier.width(8.dp))
//                Text(
//                    text = statusText,
//                    style = MaterialTheme.typography.bodyMedium,
//                    color = MaterialTheme.colorScheme.onSurface
//                )
//            }
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            // Start/Stop Button
//            Button(
//                onClick = {
//                    if (isHotspotEnabled) {
//                        onStopTapped()
//                    } else {
//                        onStartTapped()
//                    }
//                },
//                enabled = !isProcessing,
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                if (isProcessing) {
//                    CircularProgressIndicator(
//                        color = MaterialTheme.colorScheme.onPrimary,
//                        strokeWidth = 2.dp,
//                        modifier = Modifier.size(24.dp)
//                    )
//                } else {
//                    Text(if (isHotspotEnabled) "Stop Hotspot" else "Start Hotspot")
//                }
//            }
//        }
//    }
//}
//
//
//@Composable
//fun BandSelection(
//    selectedBand: String,
//    onBandSelected: (String) -> Unit,
//    bands: List<String>,
//    isHotspotEnabled: Boolean
//) {
//    Text(
//        text = "Select Band:",
//        style = MaterialTheme.typography.bodyMedium,
//        modifier = Modifier.padding(top = 16.dp)
//    )
//
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(vertical = 8.dp)
//            .border(
//                width = 1.dp,
//                color = MaterialTheme.colorScheme.primary,
//                shape = RoundedCornerShape(8.dp)
//            ),
//        horizontalArrangement = Arrangement.SpaceBetween
//    ) {
//        bands.forEachIndexed { index, band ->
//            val isSelected = selectedBand == band
//            Box(
//                modifier = Modifier
//                    .weight(1f)
//                    .clickable(
//                        enabled = !isHotspotEnabled,
//                        onClick = { onBandSelected(band) }
//                    )
//                    .background(
//                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
//                        shape = if (index == 0) {
//                            RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
//                        } else if (index == bands.lastIndex) {
//                            RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
//                        } else {
//                            RoundedCornerShape(0.dp)
//                        }
//                    )
//                    .padding(vertical = 12.dp),
//                contentAlignment = Alignment.Center
//            ) {
//                Text(
//                    text = band,
//                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
//                    style = MaterialTheme.typography.bodySmall
//                )
//            }
//        }
//    }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ImprovedHeader() {
//    var showMenu by remember { mutableStateOf(false) }
//
//    TopAppBar(
//        title = { Text("Asol") },
//        navigationIcon = {
//            IconButton(onClick = { /* Open navigation drawer */ }) {
//                Icon(Icons.Filled.Menu, contentDescription = "Menu")
//            }
//        },
//        actions = {
//            IconButton(onClick = { /* Open settings */ }) {
//                Icon(Icons.Filled.Settings, contentDescription = "Settings")
//            }
//            IconButton(onClick = { showMenu = !showMenu }) {
//                Icon(Icons.Filled.MoreVert, contentDescription = "More options")
//            }
//            DropdownMenu(
//                expanded = showMenu,
//                onDismissRequest = { showMenu = false }
//            ) {
//                DropdownMenuItem(
//                    text = { Text("Help") },
//                    onClick = { /* Navigate to help */ }
//                )
//                DropdownMenuItem(
//                    text = { Text("About") },
//                    onClick = { /* Navigate to about */ }
//                )
//            }
//        },
//        colors = TopAppBarDefaults.topAppBarColors(
//            containerColor = MaterialTheme.colorScheme.primary,
//            titleContentColor = MaterialTheme.colorScheme.onPrimary,
//            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
//        ),
////        elevation = 4.dp
//    )
//}

