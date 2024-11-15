package com.example.wifip2photspot

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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BlockedDevicesSection(
    devices: List<DeviceInfo>,
    onUnblock: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(
            text = "Blocked Devices (${devices.size}):",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        if (devices.isEmpty()) {
            Text(
                text = "No blocked devices.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(devices) { deviceInfo ->
                    BlockedDeviceCard(
                        deviceInfo = deviceInfo,
                        onUnblock = onUnblock
                    )
                    Divider()
                }
            }
        }
    }
}
// BlockedDevicesSection.kt
fun LazyListScope.blockedDevicesSection(
    devices: List<DeviceInfo>,
    onUnblock: (String) -> Unit
) {
    item {
        Text(
            text = "Blocked Devices (${devices.size}):",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }

    if (devices.isEmpty()) {
        item {
            Text(
                text = "No blocked devices.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    } else {
        items(devices) { deviceInfo ->
            BlockedDeviceCard(
                deviceInfo = deviceInfo,
                onUnblock = onUnblock
            )
            Divider()
        }
    }
}

@Composable
fun BlockedDeviceCard(
    deviceInfo: DeviceInfo,
    onUnblock: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = deviceInfo.alias ?: "Unknown Device",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "MAC Address: ${deviceInfo.device.deviceAddress}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = { onUnblock(deviceInfo.device.deviceAddress) }
                ) {
                    Text("Unblock")
                }
            }
        }
    }
}