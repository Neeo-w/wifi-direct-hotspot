package com.example.wifip2photspot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
            HorizontalDivider()
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
