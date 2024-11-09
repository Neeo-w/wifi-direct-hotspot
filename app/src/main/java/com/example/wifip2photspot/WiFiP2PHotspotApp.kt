// WiFiP2PHotspotApp.kt
package com.example.wifip2photspot

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.net.VpnService
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.wifip2photspot.ui.theme.ThemeToggle


import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat.startActivityForResult
import com.example.wifip2photspot.Proxy.DataUsageSection
import com.example.wifip2photspot.Proxy.ProxyControlSection
import com.example.wifip2photspot.Proxy.ProxyInstructionsSection
import com.example.wifip2photspot.Proxy.ProxyLimitationsSection
import com.example.wifip2photspot.Proxy.ProxyService
import com.example.wifip2photspot.Proxy.ProxyStatusSection


@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun WiFiP2PHotspotApp(
    viewModel: HotspotViewModel, activity: Activity, isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit
) {

    val context = LocalContext.current

    // Collect state from ViewModel
    val ssid by viewModel.ssid.collectAsState()
    val password by viewModel.password.collectAsState()
    val selectedBand by viewModel.selectedBand.collectAsState()
    val isHotspotEnabled by viewModel.isHotspotEnabled.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val uploadSpeed by viewModel.uploadSpeed.collectAsState()
    val downloadSpeed by viewModel.downloadSpeed.collectAsState()
    val connectedDevices by viewModel.connectedDevices.collectAsState()
    val logEntries by viewModel.logEntries.collectAsState()
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




//// Proxy server state
//    val isProxyRunning by viewModel.isProxyRunning.collectAsState()
    val proxyPort by viewModel.proxyPort.collectAsState()
//    val proxyIP = "192.168.49.1" // Typically the group owner's IP in Wi-Fi Direct
//

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
    // Handle UI Events
    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is HotspotViewModel.UiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }

                is HotspotViewModel.UiEvent.ShowSnackbar -> {
                    // Implement Snackbar if needed
                }

                is HotspotViewModel.UiEvent.StartProxyService -> {
                    val intent = Intent(context, ProxyService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                }

                is HotspotViewModel.UiEvent.StopProxyService -> {
                    val intent = Intent(context, ProxyService::class.java)
                    context.stopService(intent)
                }
            }
        }
    }


    // Scaffold for overall layout
    Scaffold(
        topBar = { ImprovedHeader(isHotspotEnabled = isHotspotEnabled) },
        content = { paddingValues ->
            LazyColumn(
                contentPadding = paddingValues,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Theme Toggle
                item {
                    ThemeToggle(
                        isDarkTheme = isDarkTheme,
                        onToggle = onThemeChange
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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
                            isHotspotEnabled = isHotspotEnabled,
                            proxyPort = proxyPort,
                            onProxyPortChange = { newPort ->
                                viewModel.updateProxyPort(newPort)
                            },
                            selectedBand = selectedBand,
                            onBandSelected = { newBand ->
                                viewModel.updateSelectedBand(newBand)
                            },
                            bands = listOf("Auto", "2.4GHz", "5GHz")
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
                    item {
                        DataUsageSection(
                            rxBytes = sessionRxBytes,
                            txBytes = sessionTxBytes
                        )
                    }
//                    item {
//                        HistoricalDataUsageSection(historicalData = viewModel.historicalDataUsage.value)
//                    }
                    item {
                        SpeedGraphSection(
                            uploadSpeeds = uploadSpeedEntries,
                            downloadSpeeds = downloadSpeedEntries
                        )
                    }
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
                                viewModel.onButtonStartTapped(
                                    ssidInput = ssidFieldState.text.ifBlank { "TetherGuard" },
                                    passwordInput = passwordFieldState.text.ifBlank { "00000000" },
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
                item {
                    NotificationSettingsSection(
                        notificationEnabled = viewModel.notificationEnabled.value,
                        onNotificationEnabledChange = { viewModel.updateNotificationEnabled(it) },
                        soundEnabled = viewModel.notificationSoundEnabled.value,
                        onSoundEnabledChange = { viewModel.updateNotificationSoundEnabled(it) },
                        vibrationEnabled = viewModel.notificationVibrationEnabled.value,
                        onVibrationEnabledChange = { viewModel.updateNotificationVibrationEnabled(it) }
                    )
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
                    if(blockedDeviceInfos.isNotEmpty()) {
                        blockedDevicesSection(
                            devices = blockedDeviceInfos,
                            onUnblock = { deviceAddress ->
                                viewModel.unblockDevice(deviceAddress)
                            }
                        )
                    }
                }
//                item {
//                    HotspotScheduler(
//                        onScheduleStart = { timeInMillis ->
//                            viewModel.scheduleHotspotStart(timeInMillis)
//                        },
//                        onScheduleStop = { timeInMillis ->
//                            viewModel.scheduleHotspotStop(timeInMillis)
//                        }
//                    )
//                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Log Section
                item {
                    LogSection(logEntries = logEntries)
                }
                item {
                    FeedbackForm(onSubmit = { feedback ->
                        viewModel.submitFeedback(feedback)
                    })
                }
                item {
                    ContactSupportSection(onContactSupport = {
                        viewModel.contactSupport()
                    })
                }
            }
        }
    )
}


/**
 * Checks if all required permissions are granted.
 */
//fun arePermissionsGranted(context: Context): Boolean {
//    val fineLocation = ContextCompat.checkSelfPermission(
//        context, Manifest.permission.ACCESS_FINE_LOCATION
//    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
//
//    val wifiState = ContextCompat.checkSelfPermission(
//        context, Manifest.permission.ACCESS_WIFI_STATE
//    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
//
//    val changeWifiState = ContextCompat.checkSelfPermission(
//        context, Manifest.permission.CHANGE_WIFI_STATE
//    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
//
//    return fineLocation && wifiState && changeWifiState
//}

/**
 * Opens the device's Wi-Fi settings.
 */
fun openDeviceSettings(context: Context) {
    val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
    context.startActivity(intent)
}

/**
 * Opens the app-specific settings screen.
 */
fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    intent.data = Uri.fromParts("package", context.packageName, null)
    context.startActivity(intent)
}

/**
 * Determines if Wi-Fi is enabled.
 */
fun isWifiEnabled(context: Context): Boolean {
    val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    return wifiManager?.isWifiEnabled ?: false
}

/**
 * Determines if Location services are enabled.
 */
fun isLocationEnabled(context: Context): Boolean {
    val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    return locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true || locationManager?.isProviderEnabled(
        LocationManager.NETWORK_PROVIDER
    ) == true
}

/**
 * Launches settings if services are enabled.
 */
//fun launchSettingsIfServicesEnabled(context: Context) {
//    if (arePermissionsGranted(context)) {
//        if (isWifiEnabled(context) && isLocationEnabled(context)) {
//            openDeviceSettings(context)
//        } else {
//            // Optionally, prompt to enable services
//            Toast.makeText(
//                context, "Please enable Wi-Fi and Location services.", Toast.LENGTH_SHORT
//            ).show()
//        }
//    } else {
//        // Optionally, prompt for permissions again or inform the user
//        Toast.makeText(context, "Permissions are not granted.", Toast.LENGTH_SHORT).show()
//    }
//}


@Composable
fun StartVpnButton(onStartVpn: () -> Unit) {
    Button(onClick = onStartVpn) {
        Text("Start VPN")
    }
}
//// Launcher for requesting multiple permissions
//    val permissionLauncher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.RequestMultiplePermissions()
//    ) { permissions ->
//        val allGranted = permissions.all { it.value }
//        if (allGranted) {
//            Toast.makeText(
//                context, context.getString(R.string.permissions_granted), Toast.LENGTH_SHORT
//            ).show()
//            if (!isWifiEnabled(context) || !isLocationEnabled(context)) {
//                showServiceEnableDialog = true
//            }
//        } else {
//            // Use the Activity instance to call shouldShowRequestPermissionRationale
//            val permanentlyDenied =
//                permissions.any { !it.value && !activity.shouldShowRequestPermissionRationale(it.key) }
//            if (permanentlyDenied) {
//                showSettingsDialog = true
//            } else {
//                Toast.makeText(
//                    context, context.getString(R.string.permissions_denied), Toast.LENGTH_SHORT
//                ).show()
//            }
//        }
//    }
//
//// Show Service Enable Dialog
//    if (showServiceEnableDialog) {
//        AlertDialog(onDismissRequest = { showServiceEnableDialog = false },
//            title = { Text(text = stringResource(id = R.string.enable_services)) },
//            text = { Text(text = stringResource(id = R.string.enable_wifi_location_services)) },
//            confirmButton = {
//                TextButton(onClick = {
//                    showServiceEnableDialog = false
//                    openDeviceSettings(context)
//                }) {
//                    Text(text = stringResource(id = R.string.go_to_settings))
//                }
//            },
//            dismissButton = {
//                TextButton(onClick = { showServiceEnableDialog = false }) {
//                    Text(text = stringResource(id = R.string.cancel))
//                }
//            })
//    }
//
//// Show Settings Dialog
//    if (showSettingsDialog) {
//        AlertDialog(onDismissRequest = { showSettingsDialog = false },
//            title = { Text(text = stringResource(id = R.string.permissions_required)) },
//            text = { Text(text = stringResource(id = R.string.permissions_permanently_denied)) },
//            confirmButton = {
//                TextButton(onClick = {
//                    showSettingsDialog = false
//                    openAppSettings(context)
//                }) {
//                    Text(text = stringResource(id = R.string.go_to_settings))
//                }
//            },
//            dismissButton = {
//                TextButton(onClick = { showSettingsDialog = false }) {
//                    Text(text = stringResource(id = R.string.cancel))
//                }
//            })
//    }
