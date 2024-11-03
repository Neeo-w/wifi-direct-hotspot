// WiFiP2PHotspotApp.kt
package com.example.wifip2photspot

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun WiFiP2PHotspotApp(viewModel: HotspotViewModel) {
    val context = LocalContext.current

    val ssid by viewModel.ssid.collectAsState()
    val password by viewModel.password.collectAsState()
    val selectedBand by viewModel.selectedBand.collectAsState()
    val isHotspotEnabled by viewModel.isHotspotEnabled.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val uploadSpeed by viewModel.uploadSpeed.collectAsState()
    val downloadSpeed by viewModel.downloadSpeed.collectAsState()
    val connectedDevices by viewModel.connectedDevices.collectAsState()
    val logEntries by viewModel.logEntries.collectAsState()

    // Handle UI Events
    LaunchedEffect(viewModel.eventFlow) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is HotspotViewModel.UiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is HotspotViewModel.UiEvent.ShowSnackbar -> {
                    // Implement Snackbar if needed
                }
            }
        }
    }

    // Scaffold for Snackbar and other UI elements
    Scaffold(
        topBar = { ImprovedHeader(isHotspotEnabled = isHotspotEnabled) },
        content = { paddingValues ->
            LazyColumn(
                contentPadding = paddingValues,
                modifier = Modifier.fillMaxSize()
            ) {
//                Spacer(modifier = Modifier.height(16.dp))

                // Display Input Fields and Band Selection When No Devices Connected
                if (connectedDevices.isEmpty()) {
                    item {
                        InputFieldsSection(
                            ssidInput = ssid,
                            onSsidChange = { newSSID ->
                                viewModel.updateSSID(newSSID)
                            },
                            passwordInput = password,
                            onPasswordChange = { newPassword ->
                                viewModel.updatePassword(newPassword)
                            },
                            passwordVisible = false, // Manage visibility via ViewModel if needed
                            onPasswordVisibilityChange = { /* Implement if managed in ViewModel */ },
                            isHotspotEnabled = isHotspotEnabled
                        )
                    }

                    item {
                        BandSelection(
                            selectedBand = selectedBand,
                            onBandSelected = { newBand ->
                                viewModel.updateSelectedBand(newBand)
                            },
                            bands = listOf("Auto", "2.4GHz", "5GHz"),
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

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    HotspotControlSection(
                        isHotspotEnabled = isHotspotEnabled,
                        isProcessing = isProcessing,
                        ssidInput = ssid,
                        passwordInput = password,
                        selectedBand = selectedBand,
                        onStartTapped = {
                            viewModel.onButtonStartTapped(
                                ssidInput = ssid,
                                passwordInput = password,
                                selectedBand = selectedBand
                            )
                        },
                        onStopTapped = {
                            viewModel.onButtonStopTapped()
                        }
                    )
                }

                // Display Connected Devices When Present
                if (connectedDevices.isNotEmpty()) {
                    item {
                        ConnectedDevicesSection(
                            devices = connectedDevices,
                            onDeviceClick = { device ->
                                // Handle device click (e.g., show details or disconnect)
                                // Implement as needed
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    // Pass logEntries to LogSection
                    LogSection(logEntries = logEntries)
                }
            }
        }
    )
}
