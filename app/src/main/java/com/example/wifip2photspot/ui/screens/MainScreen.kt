package com.example.wifip2photspot.ui.screens


import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollSource.Companion.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getSystemService
import androidx.navigation.NavHostController
import com.example.wifip2photspot.BatteryStatusSection
import com.example.wifip2photspot.ConnectionStatusBar
import com.example.wifip2photspot.HotspotControlSection
import com.example.wifip2photspot.viewModel.HotspotViewModel
import com.example.wifip2photspot.ImprovedHeader
import com.example.wifip2photspot.InputFieldsSection
import com.example.wifip2photspot.LogSection
//import com.example.wifip2photspot.Proxy.ProxyService
import com.example.wifip2photspot.SpeedGraphSection
import com.example.wifip2photspot.VPN.VpnStatusDisplay
import com.example.wifip2photspot.blockedDevicesSection
import com.example.wifip2photspot.connectedDevicesSection
import com.example.wifip2photspot.isLocationEnabled
import com.example.wifip2photspot.isWifiEnabled
import com.example.wifip2photspot.ui.IdleCountdownDisplay
import com.example.wifip2photspot.VPN.HotspotControlButton
import com.example.wifip2photspot.socksProxy.ProxyInfoDisplay
import com.example.wifip2photspot.socksProxy.ProxySetupInstructions
import com.example.wifip2photspot.ui.NotificationSettingsAlert
import com.example.wifip2photspot.viewModel.VpnViewModel

@SuppressLint("StateFlowValueCalledInComposition", "UnrememberedMutableState")
@Composable
fun MainScreen(
    navController: NavHostController,
    hotspotViewModel: HotspotViewModel,
    vpnViewModel: VpnViewModel
) {
    val context = LocalContext.current

    val ssid by hotspotViewModel.ssid.collectAsState()
    val password by hotspotViewModel.password.collectAsState()
    val selectedBand by hotspotViewModel.selectedBand.collectAsState()
    // Collect state from ViewModels
    val isHotspotEnabled by hotspotViewModel.isHotspotEnabled.collectAsState()
    val isProcessing by hotspotViewModel.isProcessing.collectAsState()
    val uploadSpeed by hotspotViewModel.uploadSpeed.collectAsState()
    val downloadSpeed by hotspotViewModel.downloadSpeed.collectAsState()
    val connectedDevices by hotspotViewModel.connectedDevices.collectAsState()
    val logEntries by hotspotViewModel.logEntries.collectAsState()
    val remainingIdleTime by hotspotViewModel.remainingIdleTime.collectAsState()
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
    val connectedDeviceInfos by hotspotViewModel.connectedDeviceInfos.collectAsState()
    // Collect the blocked devices from the hotspotViewModel
    val blockedDeviceInfos by hotspotViewModel.blockedDeviceInfos.collectAsState()
    val (sessionRxBytes, sessionTxBytes) = hotspotViewModel.getSessionDataUsage()
    val uploadSpeedEntries by hotspotViewModel.uploadSpeedEntries.collectAsState()
    val downloadSpeedEntries by hotspotViewModel.downloadSpeedEntries.collectAsState()
    val batteryLevel by hotspotViewModel.batteryLevel.collectAsState()
    // Proxy server state
//    val proxyPort by hotspotViewModel.proxyPort.collectAsState()
//    // Mutable state to control the visibility of NotificationSettingsAlert
//    val showNotificationSettingsAlert = mutableStateOf(false)

    val isVpnActive by vpnViewModel.isVpnActive.collectAsState()
    val vpnStatusMessage by vpnViewModel.vpnStatusMessage.collectAsState()

    val proxyPort = 8181 // Must match the proxy and SSH local port



    // Update hotspotViewModel when text changes
    LaunchedEffect(ssidFieldState.text) {
        hotspotViewModel.updateSSID(ssidFieldState.text)
    }
    LaunchedEffect(passwordFieldState.text) {
        hotspotViewModel.updatePassword(passwordFieldState.text)
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
            hotspotViewModel.startIdleMonitoring()
        }
    }
    // Handle UI Events
    LaunchedEffect(key1 = true) {
        hotspotViewModel.eventFlow.collect { event ->
            when (event) {
                is HotspotViewModel.UiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }

                is HotspotViewModel.UiEvent.ShowSnackbar -> {
                    // Implement Snackbar if needed
                }

//                is HotspotViewModel.UiEvent.StartProxyService -> {
//                    val intent = Intent(context, ProxyService::class.java)
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                        context.startForegroundService(intent)
//                    } else {
//                        context.startService(intent)
//                    }
//                }

//                is HotspotViewModel.UiEvent.StopProxyService -> {
//                    val intent = Intent(context, ProxyService::class.java)
//                    context.stopService(intent)
//                }
                HotspotViewModel.UiEvent.StartProxyService -> TODO()
                HotspotViewModel.UiEvent.StopProxyService -> TODO()
            }
        }
    }
    // Scaffold for overall layout
    Scaffold(
        topBar = {
            ImprovedHeader(
                isHotspotEnabled = isHotspotEnabled,
                HotspotViewModel = hotspotViewModel,
                onSettingsClick = { navController.navigate("settings_screen") },
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

                item {
                    // Proxy Information Display
                    ProxyInfoDisplay(
                        proxyIp = "192.168.49.1",
                        proxyPort = 8181
                    )
                }

                item {
                    // VPN Status Display
                    VpnStatusDisplay(
                        isVpnActive = vpnViewModel.isVpnActive.collectAsState().value,
                        vpnStatusMessage = vpnViewModel.vpnStatusMessage.collectAsState().value
                    )
                }

                // Display Idle Countdown if applicable
                item {
                    IdleCountdownDisplay(remainingIdleTime = remainingIdleTime)

                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (connectedDeviceInfos.isNotEmpty()) {
                    item {
                        SpeedGraphSection(
                            uploadSpeeds = uploadSpeedEntries,
                            downloadSpeeds = downloadSpeedEntries
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        // Proxy Setup Instructions
                        ProxySetupInstructions()

                        Spacer(modifier = Modifier.height(16.dp))
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
                            isHotspotEnabled = isHotspotEnabled
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
//                item {
//                    // VPN Status Display
//                    VpnStatusDisplay(
//                        isVpnActive = isVpnActive,
//                        vpnStatusMessage = vpnStatusMessage
//                    )
//                }
//                item {
//                    // Hotspot Control Button
//                    HotspotControlButton(
//                        isHotspotEnabled = isHotspotEnabled,
//                        isProcessing = isProcessing,
//                        onStartTapped = { hotspotViewModel.startTethering() },
//                        onStopTapped = { hotspotViewModel.stopTethering() }
//                    )
//                }

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
                            proxyPort = proxyPort,
                            onStartTapped = {
                                hotspotViewModel.startTethering()
//                                hotspotViewModel.on(
//                                    ssidInput = ssidFieldState.text.ifBlank { "TetherGuard" },
//                                    passwordInput = passwordFieldState.text.ifBlank { "00000000" },
//                                    selectedBand = selectedBand,
//                                )
                            },
                            onStopTapped = {
                                hotspotViewModel.onButtonStopTapped()
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
                            hotspotViewModel.updateDeviceAlias(deviceAddress, alias)
                        },
                        onBlockUnblock = { deviceAddress ->
                            val deviceInfo =
                                connectedDeviceInfos.find { it.device.deviceAddress == deviceAddress }
                            if (deviceInfo != null) {
                                if (deviceInfo.isBlocked) {
                                    hotspotViewModel.unblockDevice(deviceAddress)
                                } else {
                                    hotspotViewModel.blockDevice(deviceAddress)
                                }
                            }
                        },
                        onDisconnect = { deviceAddress ->
                            val deviceInfo =
                                connectedDeviceInfos.find { it.device.deviceAddress == deviceAddress }
                            if (deviceInfo != null) {
                                hotspotViewModel.disconnectDevice(deviceInfo)
                            }
                        }
                    )
                    if (blockedDeviceInfos.isNotEmpty()) {
                        blockedDevicesSection(
                            devices = blockedDeviceInfos,
                            onUnblock = { deviceAddress ->
                                hotspotViewModel.unblockDevice(deviceAddress)
                            }
                        )
                    }
                }


                // Display Connected Devices
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                // Log Section
                item {
                    LogSection(logEntries = logEntries)
                }

            }
//            // Show the Notification Settings Alert if needed
//            NotificationSettingsAlert(
//                showAlert = showNotificationSettingsAlert.value,
//                onDismiss = { showNotificationSettingsAlert.value = false }
//            )
// SSH Configuration Dialog
//            SSHConfigurationDialog(
//                isOpen = showSSHConfigDialog.value,
//                onDismiss = { showSSHConfigDialog.value = false },
//                onSave = { username, password, host, port ->
//                    // Save credentials securely
//                    hotspotViewModel.saveSSHCredentials(username, password)
//                    hotspotViewModel.setSSHHost(host)
//                    hotspotViewModel.setSSHPort(port)
//                }
//            )
        }
    )
}

