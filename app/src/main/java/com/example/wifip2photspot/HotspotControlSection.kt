// HotspotControlSection.kt
package com.example.wifip2photspot
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun HotspotControlSection(
    isHotspotEnabled: Boolean,
    isProcessing: Boolean,
    onStartTapped: () -> Unit,
    onStopTapped: () -> Unit,
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Start Button with Loading Indicator
        Button(
            onClick = onStartTapped,
            enabled = !isHotspotEnabled && !isProcessing,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Start Hotspot")
            }
        }

        val statusIcon = when {
            isProcessing -> Icons.Default.Sync
            isHotspotEnabled -> Icons.Default.Wifi
            else -> Icons.Default.WifiOff
        }
        val statusText = when {
            isProcessing -> if (isHotspotEnabled) "Stopping hotspot..." else "Starting hotspot..."
            isHotspotEnabled -> "Hotspot active"
            else -> "Hotspot is inactive"
        }
        Icon(
            imageVector = statusIcon,
            contentDescription = statusText,
            tint = if (isHotspotEnabled) Color(0xFF4CAF50) else Color(0xFFF44336) // Green or Red
        )

        // Stop Button with Loading Indicator
        Button(
            onClick = onStopTapped,
            enabled = isHotspotEnabled && !isProcessing,
            modifier = Modifier.weight(1f).padding(start = 8.dp)
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Stop Hotspot")
            }
        }
    }
}