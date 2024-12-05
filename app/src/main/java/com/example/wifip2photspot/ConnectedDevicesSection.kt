package com.example.wifip2photspot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            HorizontalDivider()
        }
    }
}
