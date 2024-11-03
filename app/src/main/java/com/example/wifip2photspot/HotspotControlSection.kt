package com.example.wifip2photspot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.ui.Alignment


@Composable
fun HotspotControlSection(
    isHotspotEnabled: Boolean,
    isProcessing: Boolean,
    ssidInput: String,
    passwordInput: String,
    selectedBand: String,
    onStartTapped: () -> Unit,
    onStopTapped: () -> Unit
) {
    Spacer(modifier = Modifier.height(16.dp))

    Card(
        elevation = CardDefaults.cardElevation(4.dp),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Status Text with Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val statusIcon = when {
                        isProcessing -> Icons.Default.Sync
                        isHotspotEnabled -> Icons.Default.Wifi
                        else -> Icons.Default.WifiOff
                    }
                    val statusText = when {
                        isProcessing -> if (isHotspotEnabled) "Stopping hotspot..." else "Starting hotspot..."
                        isHotspotEnabled -> "Hotspot is active"
                        else -> "Hotspot is inactive"
                    }
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = statusText,
                        tint = if (isHotspotEnabled) Color(0xFF4CAF50) else Color(0xFFF44336) // Green or Red
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Start/Stop Button
                Button(
                    onClick = {
                        if (isHotspotEnabled) {
                            onStopTapped()
                        } else {
                            onStartTapped()
                        }
                    }, enabled = !isProcessing, modifier = Modifier.fillMaxWidth()
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(if (isHotspotEnabled) "Stop Hotspot" else "Start Hotspot")
                    }
                }
            }
        }
    }
}