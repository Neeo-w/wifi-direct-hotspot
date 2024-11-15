package com.example.wifip2photspot.ui.screen


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.wifip2photspot.BatteryStatusSection
import com.example.wifip2photspot.ClientMonitoringSection
import com.example.wifip2photspot.ConnectionInfoSection
import com.example.wifip2photspot.ConnectionStatusBar
import com.example.wifip2photspot.HotspotControlSection
import com.example.wifip2photspot.HotspotViewModel
import com.example.wifip2photspot.ImprovedHeader
import com.example.wifip2photspot.InputFieldsSection
import com.example.wifip2photspot.LogSection
//import com.example.wifip2photspot.Proxy.ProxyService
import com.example.wifip2photspot.SpeedGraphSection
import com.example.wifip2photspot.blockedDevicesSection
import com.example.wifip2photspot.connectedDevicesSection
import com.example.wifip2photspot.isLocationEnabled
import com.example.wifip2photspot.isWifiEnabled
import com.example.wifip2photspot.ui.IdleCountdownDisplay
import com.example.wifip2photspot.ui.SettingsScreen

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun MainScreen(navController: NavHostController, viewModel: HotspotViewModel, onHelpClick: () -> Unit) {
    val context = LocalContext.current
    val ssid by viewModel.ssid.collectAsState()
    val password by viewModel.password.collectAsState()
    val selectedBand by viewModel.selectedBand.collectAsState()
//    val socksPort by viewModel.socksPort.collectAsState()
//
//    val socksPortInput by viewModel.socksPortInput.collectAsState()
//    val onSocksPortChange by viewModel.onSocksPortChange.collectAsState()
    val isHotspotEnabled by viewModel.isHotspotEnabled.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val uploadSpeed by viewModel.uploadSpeed.collectAsState()
    val downloadSpeed by viewModel.downloadSpeed.collectAsState()
    val connectedDevices by viewModel.connectedDevices.collectAsState()
    val logEntries by viewModel.logEntries.collectAsState()
    val remainingIdleTime by viewModel.remainingIdleTime.collectAsState()
    var isDarkTheme by rememberSaveable { mutableStateOf(false) }

    // Local state for TextFieldValue
    var ssidFieldState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(ssid))
    }
    var passwordFieldState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(password))
    }

    // Dialog State
    var showServiceEnableDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val connectedDeviceInfos by viewModel.connectedDeviceInfos.collectAsState()
    // Collect the blocked devices from the ViewModel
    val blockedDeviceInfos by viewModel.blockedDeviceInfos.collectAsState()

    val (sessionRxBytes, sessionTxBytes) = viewModel.getSessionDataUsage()
    val uploadSpeedEntries by viewModel.uploadSpeedEntries.collectAsState()
    val downloadSpeedEntries by viewModel.downloadSpeedEntries.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()

    // Collect the server IP address and socks port
    val serverIpAddress by viewModel.serverIpAddress.collectAsState()
    val socksPort by viewModel.socksPort.collectAsState()

    val connectedClientsInfo by viewModel.connectedClients.collectAsState()
//    val scaffoldState = rememberScaffoldState()



    // Update ViewModel when text changes
    LaunchedEffect(ssidFieldState.text) {
        viewModel.updateSSID(ssidFieldState.text)
    }
    LaunchedEffect(passwordFieldState.text) {
        viewModel.updatePassword(passwordFieldState.text)
    }
    LaunchedEffect(connectedDeviceInfos) {
        Log.d(
            "WiFiP2PHotspotApp",
            "ConnectedDeviceInfos updated: ${connectedDeviceInfos.size} devices"
        )
    }
    // Start idle monitoring when the hotspot is enabled
    LaunchedEffect(isHotspotEnabled) {
        if (isHotspotEnabled) {
            viewModel.startIdleMonitoring()
        }
    }
    // Handle UI Events
// Collect events from the ViewModel
// Collect events from the ViewModel
    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is HotspotViewModel.UiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }

                // No other events to handle
                is HotspotViewModel.UiEvent.ShowSnackbar -> TODO()
            }
        }
    }

    // Scaffold for overall layout
    Scaffold(
//        scaffoldState = scaffoldState,
        topBar = {
            ImprovedHeader(
                isHotspotEnabled = isHotspotEnabled,
                viewModel = viewModel,
                onSettingsClick = { navController.navigate("settings_screen") },
                onHelpClick = onHelpClick

            )
        },
        content = { paddingValues ->
            LazyColumn(
                contentPadding = paddingValues,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                // Display Idle Countdown if applicable
                item {
                    IdleCountdownDisplay(remainingIdleTime = remainingIdleTime)
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                // Inside your LazyColumn or Column
                if (isHotspotEnabled) {
                    item {
                        ConnectionInfoSection(
                            serverIpAddress = serverIpAddress,
                            socksPort = socksPort
                        )
                    }
                }

//                if (connectedDeviceInfos.isNotEmpty()) {
//                    item {
//                        SpeedGraphSection(
//                            uploadSpeeds = uploadSpeedEntries,
//                            downloadSpeeds = downloadSpeedEntries
//                        )
//                    }
//                }
                // Inside your LazyColumn or Column
                if (connectedClientsInfo.isNotEmpty()) {
                    item {
                        ClientMonitoringSection(clients = connectedClientsInfo)
                    }
                }

                // Input Fields and Band Selection
                if (connectedDeviceInfos.isEmpty()) {
                    item {
                        InputFieldsSection(
                            ssidInput = ssidFieldState,
                            onSsidChange = { newValue ->
                                ssidFieldState = newValue
                            },
                            passwordInput = passwordFieldState,
                            onPasswordChange = { newValue ->
                                passwordFieldState = newValue
                            },
                            socksPortInput = socksPort,
                            onSocksPortChange = { viewModel.updateSocksPort(it) },
                            isHotspotEnabled = isHotspotEnabled,


                            )
                    }
                } else {
                    item {
                        ConnectionStatusBar(
                            uploadSpeed = uploadSpeed,
                            downloadSpeed = downloadSpeed,
                            totalDownload = downloadSpeed, // Adjust if you have a separate totalDownload
                            connectedDevicesCount = connectedDevices.size
                        )
                    }
                }

                item {
                    BatteryStatusSection(batteryLevel = batteryLevel)
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                // Hotspot Control Section
                item {
                    if (isWifiEnabled(context) && isLocationEnabled(context)) {
                        HotspotControlSection(
                            isHotspotEnabled = isHotspotEnabled,
                            isProcessing = isProcessing,
                            ssidInput = ssidFieldState.text,
                            passwordInput = passwordFieldState.text,
                            selectedBand = selectedBand,
                            socksPortInput = socksPort,
                            onStartTapped = {
                                viewModel.onButtonStartTapped(
                                    ssidInput = ssidFieldState.text.ifBlank { "TetherGuard" },
                                    passwordInput = passwordFieldState.text.ifBlank { "00000000" },
                                    socksPortInput = if (socksPort.isBlank()) "1080" else socksPort,
                                    selectedBand = selectedBand,


                                    )
                            },
                            onStopTapped = {
                                viewModel.onButtonStopTapped()
                            }
                        )

                    } else {
                        showServiceEnableDialog = true
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                if (isHotspotEnabled) {
                    // Connected Devices Section
                    connectedDevicesSection(
                        devices = connectedDeviceInfos,
                        onDeviceAliasChange = { deviceAddress, alias ->
                            viewModel.updateDeviceAlias(deviceAddress, alias)
                        },
                        onBlockUnblock = { deviceAddress ->
                            val deviceInfo =
                                connectedDeviceInfos.find { it.device.deviceAddress == deviceAddress }
                            if (deviceInfo != null) {
                                if (deviceInfo.isBlocked) {
                                    viewModel.unblockDevice(deviceAddress)
                                } else {
                                    viewModel.blockDevice(deviceAddress)
                                }
                            }
                        },
                        onDisconnect = { deviceAddress ->
                            val deviceInfo =
                                connectedDeviceInfos.find { it.device.deviceAddress == deviceAddress }
                            if (deviceInfo != null) {
                                viewModel.disconnectDevice(deviceInfo)
                            }
                        }
                    )
                    if (blockedDeviceInfos.isNotEmpty()) {
                        blockedDevicesSection(
                            devices = blockedDeviceInfos,
                            onUnblock = { deviceAddress ->
                                viewModel.unblockDevice(deviceAddress)
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                // Log Section
                item {
                    LogSection(logEntries = logEntries)
                }

            }
        }
    )
}