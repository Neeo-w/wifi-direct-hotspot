package com.example.wifip2photspot.ui.screens

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.wifip2photspot.*
import com.example.wifip2photspot.ui.IdleCountdownDisplay
import com.example.wifip2photspot.viewModel.HotspotViewModel
import com.example.wifip2photspot.ui.components.ProxyInfo
import timber.log.Timber

@SuppressLint("StateFlowValueCalledInComposition", "UnrememberedMutableState", "TimberArgCount")
@Composable
fun MainScreen(
    navController: NavHostController,
    hotspotViewModel: HotspotViewModel
) {
    val context = LocalContext.current

    // Collect necessary states from the ViewModel
    val ssid by hotspotViewModel.ssid.collectAsState()
    val password by hotspotViewModel.password.collectAsState()
    val selectedBand by hotspotViewModel.selectedBand.collectAsState()

    val isHotspotEnabled by hotspotViewModel.isHotspotEnabled.collectAsState()
    val isProcessing by hotspotViewModel.isProcessing.collectAsState()
    val connectedDevices by hotspotViewModel.connectedDevices.collectAsState()
    val logEntries by hotspotViewModel.logEntries.collectAsState()
    val remainingIdleTime by hotspotViewModel.remainingIdleTime.collectAsState()

    val blockedDeviceInfos by hotspotViewModel.blockedDeviceInfos.collectAsState()
    val batteryLevel by hotspotViewModel.batteryLevel.collectAsState()
    val isVpnActive by hotspotViewModel.isVpnActive.collectAsState()

    // Proxy-related states

    val isProxyRunning by hotspotViewModel.isProxyRunning.collectAsState()
    val proxyIpAddress by hotspotViewModel.proxyIpAddress.collectAsState()

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

            // If the hotspot isn't enabled, show InputFieldsSection to configure SSID and password
            if (!isHotspotEnabled) {
                item {
                    InputFieldsSection(
                        ssidInput = TextFieldValue(ssid),
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
                // Hotspot is enabled
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

            // Display VPN status
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

            // If hotspot is enabled, show ProxyInfo and ProxyControlSection
            if (isHotspotEnabled) {
                // Proxy always runs on port 8888 with SSID/password credentials
                item {
                    ProxyInfo(
                        proxyIp = proxyIpAddress,
                        proxyPort = 8888,
                        isProxyRunning = isProxyRunning
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }


                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

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

                // Blocked Devices Section
                if (blockedDeviceInfos.isNotEmpty()) {
                    blockedDevicesSection(devices = blockedDeviceInfos,
                        onUnblock = { deviceAddress ->
                            hotspotViewModel.unblockDevice(deviceAddress)
                        })
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
    })
}
