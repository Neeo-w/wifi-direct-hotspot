package com.example.wifip2photspot.VPN

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun VpnSettingsSection(
    isVpnActive: Boolean,
    vpnStatusMessage: String,
    onVpnToggle: (Boolean) -> Unit
) {
    Column(modifier = Modifier.padding(8.dp)) {
        Text("VPN Settings", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Enable VPN Tethering", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = isVpnActive,
                onCheckedChange = onVpnToggle
            )
        }
        Text(
            text = vpnStatusMessage,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
// VpnStatusDisplay.kt
fun VpnStatusDisplay(
    isVpnActive: Boolean,
    vpnStatusMessage: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isVpnActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isVpnActive) Icons.Filled.CheckCircle else Icons.Filled.Error,
            contentDescription = "VPN Status",
            tint = if (isVpnActive) Color.Green else Color.Red
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isVpnActive) "VPN is active" else "VPN is inactive",
            style = MaterialTheme.typography.bodyLarge,
            color = if (isVpnActive) Color.Green else Color.Red
        )
    }
}


// VpnControlSection.kt
@Composable
fun VpnControlSection(
    isVpnRunning: Boolean,
    onStartVpn: () -> Unit,
    onStopVpn: () -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(4.dp),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("VPN Controls", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = onStartVpn,
                    enabled = !isVpnRunning
                ) {
                    Text("Start VPN")
                }

                Button(
                    onClick = onStopVpn,
                    enabled = isVpnRunning
                ) {
                    Text("Stop VPN")
                }
            }
        }
    }
}
