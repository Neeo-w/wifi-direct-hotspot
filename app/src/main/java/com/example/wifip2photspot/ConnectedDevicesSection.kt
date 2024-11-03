package com.example.wifip2photspot

import android.net.wifi.p2p.WifiP2pDevice

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
// ConnectedDevicesSection.kt


@Composable
fun ConnectedDevicesSection(
    devices: List<WifiP2pDevice>,
    onDeviceClick: (WifiP2pDevice) -> Unit = {}
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
        devices.forEach { device ->
            DeviceItem(device = device, onClick = onDeviceClick)
        }
    }
}

@Composable
fun DeviceItem(
    device: WifiP2pDevice,
    onClick: (WifiP2pDevice) -> Unit = {}
) {
    Card(
        elevation = CardDefaults.cardElevation(2.dp),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick(device) }
            .semantics { contentDescription = "Device: ${device.deviceName.ifBlank { "Unknown Device" }}, Address: ${device.deviceAddress}" }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Smartphone,
                contentDescription = "Device Icon",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = device.deviceName.ifBlank { "Unknown Device" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { contentDescription = "Device Name: ${device.deviceName.ifBlank { "Unknown Device" }}" }
                )
                Text(
                    text = "Address: ${device.deviceAddress}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { contentDescription = "Device Address: ${device.deviceAddress}" }
                )
                Text(
                    text = "Status: ${getDeviceStatus(device.status)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { contentDescription = "Device Status: ${getDeviceStatus(device.status)}" }
                )
            }
            IconButton(
                onClick = {
                    // Handle device action (e.g., view details, disconnect)
                },
                modifier = Modifier.semantics { contentDescription = "View details for ${device.deviceName.ifBlank { "Unknown Device" }}" }
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "Device Info Icon",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
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
