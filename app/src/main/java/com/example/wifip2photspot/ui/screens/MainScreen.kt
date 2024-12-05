package com.example.wifip2photspot.ui.screens


import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.wifip2photspot.BatteryStatusSection
import com.example.wifip2photspot.ConnectionStatusBar
import com.example.wifip2photspot.HotspotControlSection
import com.example.wifip2photspot.ImprovedHeader
import com.example.wifip2photspot.InputFieldsSection
import com.example.wifip2photspot.LogSection
import com.example.wifip2photspot.ProxyInfo
import com.example.wifip2photspot.blockedDevicesSection
import com.example.wifip2photspot.connectedDevicesSection
import com.example.wifip2photspot.isLocationEnabled
import com.example.wifip2photspot.isWifiEnabled
import com.example.wifip2photspot.ui.IdleCountdownDisplay
import com.example.wifip2photspot.viewModel.HotspotViewModel
import timber.log.Timber

@SuppressLint("StateFlowValueCalledInComposition", "UnrememberedMutableState", "TimberArgCount")
@Composable
fun MainScreen(
    navController: NavHostController, hotspotViewModel: HotspotViewModel
) {
    val context = LocalContext.current

    val ssid by hotspotViewModel.ssid.collectAsState()
    val password by hotspotViewModel.password.collectAsState()
    val selectedBand by hotspotViewModel.selectedBand.collectAsState()
    // Collect state from ViewModels
    val isHotspotEnabled by hotspotViewModel.isHotspotEnabled.collectAsState()
    val isProcessing by hotspotViewModel.isProcessing.collectAsState()
    val connectedDevices by hotspotViewModel.connectedDevices.collectAsState()
    val logEntries by hotspotViewModel.logEntries.collectAsState()
    val remainingIdleTime by hotspotViewModel.remainingIdleTime.collectAsState()

    // Local state for TextFieldValue
    var ssidFieldState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(ssid))
    }
    var passwordFieldState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(password))
    }
    // Dialog State
    var showServiceEnableDialog by remember { mutableStateOf(false) }
    val connectedDeviceInfos by hotspotViewModel.connectedDeviceInfos.collectAsState()
    // Collect the blocked devices from the hotspotViewModel
    val blockedDeviceInfos by hotspotViewModel.blockedDeviceInfos.collectAsState()
    val batteryLevel by hotspotViewModel.batteryLevel.collectAsState()
    val isVpnActive by hotspotViewModel.isVpnActive.collectAsState()


    // Proxy server details (from ViewModel or ProxyServer)
    // Proxy server details from ViewModel
//    val proxyIp = hotspotViewModel.proxyIp.collectAsState().value
//    val proxyPort = hotspotViewModel.proxyPort.collectAsState().value
//    val isProxyRunning = hotspotViewModel.isProxyRunning.collectAsState(initial = false).value

// Proxy server details
    val proxyIp = "192.168.49.1" // This can be dynamically fetched based on the device's IP
    val proxyPort = 8080
    val isProxyRunning by hotspotViewModel.isProxyRunning.collectAsState(initial = false)

    // Update hotspotViewModel when text changes
    LaunchedEffect(ssidFieldState.text) {
        hotspotViewModel.updateSSID(ssidFieldState.text)
    }
    LaunchedEffect(passwordFieldState.text) {
        hotspotViewModel.updatePassword(passwordFieldState.text)
    }
    LaunchedEffect(connectedDeviceInfos) {
        Timber.tag("WiFiP2PHotspotApp")
            .d("%s devices", "ConnectedDeviceInfos updated: %s", connectedDeviceInfos.size)
    }
    // Start idle monitoring when the hotspot is enabled
    LaunchedEffect(isHotspotEnabled) {
        if (isHotspotEnabled) {
            hotspotViewModel.startIdleMonitoring()
        }
    }
    // Scaffold for overall layout
    Scaffold(topBar = {
        ImprovedHeader(
            onSettingsClick = { navController.navigate("settings_screen") },
        )
    }, content = { paddingValues ->
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

            // Proxy Status (if you decide to include this)
            if (isHotspotEnabled) {
                item {
                    ProxyInfo(
                        proxyIp = proxyIp,
                        proxyPort = proxyPort,
                        isProxyRunning = isProxyRunning
                    )
                }
            }
            // Input Fields and Band Selection
            if (!isHotspotEnabled) {
                item {
                    InputFieldsSection(ssidInput = TextFieldValue(ssid),
                        onSsidChange = { newValue ->
                            hotspotViewModel.updateSSID(newValue.text)
                        },
                        passwordInput = TextFieldValue(password),
                        onPasswordChange = { newValue ->
                            hotspotViewModel.updatePassword(newValue.text)
                        },
                        isHotspotEnabled = isHotspotEnabled
                    )
                }
            } else {
                item {
                    Text(
                        text = "Hotspot is enabled with SSID: $ssid",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    )
                }
            }
//
//                    item {
//                        ConnectionStatusBar(
//                            uploadSpeed = uploadSpeed,
//                            downloadSpeed = downloadSpeed,
//                            totalDownload = downloadSpeed, // Adjust if you have a separate totalDownload
//                            connectedDevicesCount = connectedDevices.size
//                        )
//                    }
//                    item {
//                        BatteryStatusSection(batteryLevel = batteryLevel)
//                    }
//                    item {
//                        Spacer(modifier = Modifier.height(16.dp))
//                    }
            // Hotspot Control Section
            item {
                if (isWifiEnabled(context) && isLocationEnabled(context)) {
                    HotspotControlSection(isHotspotEnabled = isHotspotEnabled,
                        isProcessing = isProcessing,
                        onStartTapped = {
                            hotspotViewModel.onButtonStartTapped(
                                ssidInput = ssidFieldState.text.ifBlank { "TetherGuard" },
                                passwordInput = passwordFieldState.text.ifBlank { "00000000" },
                                selectedBand = selectedBand,
                            )
                        },
                        onStopTapped = {
                            hotspotViewModel.onButtonStopTapped()
                        })

                } else {
                    showServiceEnableDialog = true
                }
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                Text(
                    text = if (isVpnActive) "VPN is Active" else "VPN is Inactive",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    color = if (isVpnActive) Color.Green else Color.Red
                )
            }

            if (isHotspotEnabled) {
                // Connected Devices Section
                connectedDevicesSection(devices = connectedDeviceInfos,
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
                    })
                if (blockedDeviceInfos.isNotEmpty()) {
                    blockedDevicesSection(devices = blockedDeviceInfos,
                        onUnblock = { deviceAddress ->
                            hotspotViewModel.unblockDevice(deviceAddress)
                        })
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

    })
}

