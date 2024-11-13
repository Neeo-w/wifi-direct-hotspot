package com.example.wifip2photspot

import android.app.TimePickerDialog
import android.content.Context
import android.net.wifi.p2p.WifiP2pDevice

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ConnectedDevicesSection.kt


@Composable
fun ConnectedDevicesSection(
    devices: List<DeviceInfo>,
    onDeviceAliasChange: (String, String) -> Unit,
    onBlockUnblock: (String) -> Unit,
    onDisconnect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(
            text = "Connected Devices (${devices.size}):",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .fillMaxWidth()
                .semantics { contentDescription = "Connected Devices Header: ${devices.size} devices connected" }
        )

        if (devices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No devices connected.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { contentDescription = "No devices connected" }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(devices) { deviceInfo ->
                    DeviceInfoCard(
                        deviceInfo = deviceInfo,
                        onAliasChange = { alias ->
                            onDeviceAliasChange(deviceInfo.device.deviceAddress, alias)
                        },
                        onBlockUnblock = onBlockUnblock,
                        onDisconnect = onDisconnect
                    )
                    Divider()
                }
            }
        }
    }
}



//    Text(
//        text = "Connected Devices (${devices.size}):",
//        style = MaterialTheme.typography.titleMedium,
//        modifier = Modifier
//            .padding(vertical = 8.dp)
//            .fillMaxWidth()
//            .semantics { contentDescription = "Connected Devices Header: ${devices.size} devices connected" }
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
//                color = MaterialTheme.colorScheme.onSurfaceVariant,
//                modifier = Modifier.semantics { contentDescription = "No devices connected" }
//            )
//        }
//    } else {
//        devices.forEach { device ->
//            DeviceItem(device = device, onClick = onDeviceClick)
//        }
//    }
@Composable
fun DeviceInfoCard(
    deviceInfo: DeviceInfo,
    onAliasChange: (String) -> Unit,
    onBlockUnblock: (String) -> Unit,
    onDisconnect: (String) -> Unit
) {
    var isEditingAlias by remember { mutableStateOf(false) }
    var aliasText by remember { mutableStateOf(deviceInfo.alias ?: "") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (deviceInfo.isBlocked) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (isEditingAlias) {
                OutlinedTextField(
                    value = aliasText,
                    onValueChange = { aliasText = it },
                    label = { Text("Alias") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            onAliasChange(aliasText)
                            isEditingAlias = false
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Save Alias")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = deviceInfo.alias ?: deviceInfo.device.deviceName,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { isEditingAlias = true },
                        modifier = Modifier.semantics { contentDescription = "Edit Alias" }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Alias")
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "MAC Address: ${deviceInfo.device.deviceAddress}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Connected Since: ${formatTime(deviceInfo.connectionTime)}",
                style = MaterialTheme.typography.bodySmall
            )
            deviceInfo.ipAddress?.let {
                Text("IP Address: $it", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (deviceInfo.isBlocked) {
                    OutlinedButton(
                        onClick = { onBlockUnblock(deviceInfo.device.deviceAddress) }
                    ) {
                        Text("Unblock")
                    }
                } else {
                    OutlinedButton(
                        onClick = { onBlockUnblock(deviceInfo.device.deviceAddress) }
                    ) {
                        Text("Block")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { onDisconnect(deviceInfo.device.deviceAddress) }
                    ) {
                        Text("Disconnect")
                    }
                }
            }
        }
    }
}

fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun getDeviceStatus(status: Int): String {
    return when (status) {
        WifiP2pDevice.AVAILABLE -> "Available"
        WifiP2pDevice.INVITED -> "Invited"
        WifiP2pDevice.CONNECTED -> "Connected"
        WifiP2pDevice.FAILED -> "Failed"
        WifiP2pDevice.UNAVAILABLE -> "Unavailable"
        else -> "Unknown"
    }
}
// ConnectedDevicesSection.kt
fun LazyListScope.connectedDevicesSection(
    devices: List<DeviceInfo>,
    onDeviceAliasChange: (String, String) -> Unit,
    onBlockUnblock: (String) -> Unit,
    onDisconnect: (String) -> Unit
) {
    item {
        Text(
            text = "Connected Devices (${devices.size}):",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }

    if (devices.isEmpty()) {
        item {
            Text(
                text = "No devices connected.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    } else {
        items(devices) { deviceInfo ->
            DeviceInfoCard(
                deviceInfo = deviceInfo,
                onAliasChange = { alias ->
                    onDeviceAliasChange(deviceInfo.device.deviceAddress, alias)
                },
                onBlockUnblock = onBlockUnblock,
                onDisconnect = onDisconnect
            )
            Divider()
        }
    }
}

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
//            .semantics { contentDescription = "Device: ${device.deviceName.ifBlank { "Unknown Device" }}, Address: ${device.deviceAddress}" }
//    ) {
//        Row(
//            verticalAlignment = Alignment.CenterVertically,
//            modifier = Modifier.padding(12.dp)
//        ) {
//            Icon(
//                imageVector = Icons.Filled.Smartphone,
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
//                    color = MaterialTheme.colorScheme.onSurface,
//                    modifier = Modifier.semantics { contentDescription = "Device Name: ${device.deviceName.ifBlank { "Unknown Device" }}" }
//                )
//                Text(
//                    text = "Address: ${device.deviceAddress}",
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant,
//                    modifier = Modifier.semantics { contentDescription = "Device Address: ${device.deviceAddress}" }
//                )
//                Text(
//                    text = "Status: ${getDeviceStatus(device.status)}",
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant,
//                    modifier = Modifier.semantics { contentDescription = "Device Status: ${getDeviceStatus(device.status)}" }
//                )
//            }
//            IconButton(
//                onClick = {
//                    // Handle device action (e.g., view details, disconnect)
//                },
//                modifier = Modifier.semantics { contentDescription = "View details for ${device.deviceName.ifBlank { "Unknown Device" }}" }
//            ) {
//                Icon(
//                    imageVector = Icons.Filled.Info,
//                    contentDescription = "Device Info Icon",
//                    tint = MaterialTheme.colorScheme.primary
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

@Composable
fun HotspotScheduler(
    onScheduleStart: (Long) -> Unit,
    onScheduleStop: (Long) -> Unit
) {
    val context = LocalContext.current
    var startTime by remember { mutableStateOf<Long?>(null) }
    var stopTime by remember { mutableStateOf<Long?>(null) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Hotspot Scheduler", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            showTimePicker(context) { timeInMillis ->
                startTime = timeInMillis
            }
        }) {
            Text("Set Start Time")
        }
        startTime?.let {
            Text("Start Time: ${formatTime(it)}")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            showTimePicker(context) { timeInMillis ->
                stopTime = timeInMillis
            }
        }) {
            Text("Set Stop Time")
        }
        stopTime?.let {
            Text("Stop Time: ${formatTimes(it)}")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            startTime?.let { onScheduleStart(it) }
            stopTime?.let { onScheduleStop(it) }
        }) {
            Text("Schedule Hotspot")
        }
    }
}

// Helper functions
fun showTimePicker(context: Context, onTimeSelected: (Long) -> Unit) {
    val calendar = Calendar.getInstance()
    TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(Calendar.MINUTE, minute)
            onTimeSelected(calendar.timeInMillis)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true
    ).show()
}

fun formatTimes(timeInMillis: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timeInMillis))
}
