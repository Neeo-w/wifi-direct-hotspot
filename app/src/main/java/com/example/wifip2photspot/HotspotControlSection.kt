// HotspotControlSection.kt
package com.example.wifip2photspot

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.ResourceBundle

//import com.example.wifip2photspot.Proxy.ProxyService

@Composable
fun HotspotControlSection(
    isHotspotEnabled: Boolean,
    isProcessing: Boolean,
    ssidInput: String,
    passwordInput: String,
    selectedBand: String,
    socksPortInput: String,
    onStartTapped: () -> Unit,
    onStopTapped: () -> Unit
    ) {
    val context = LocalContext.current

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
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Start Tethering")
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
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Stop Tethering")
            }
        }
    }
}



