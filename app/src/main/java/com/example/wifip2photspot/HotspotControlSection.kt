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
import com.example.wifip2photspot.Proxy.ProxyService

@Composable
fun HotspotControlSection(
    isHotspotEnabled: Boolean,
    isProcessing: Boolean,
    ssidInput: String,
    proxyPort: Int,
    passwordInput: String,
    selectedBand: String,
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
        Button(
            onClick = {
                onStartTapped()
                // Start ProxyService
                val intent = Intent(context, ProxyService::class.java)
                context.startService(intent)
            },
            enabled = !isHotspotEnabled && !isProcessing
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Start Hotspot & Proxy")
            }
        }

        val statusIcon = when {
            isProcessing -> Icons.Default.Sync
            isHotspotEnabled -> Icons.Default.Wifi
            else -> Icons.Default.WifiOff
        }
        val statusText = when {
            isProcessing -> if (isHotspotEnabled) "Stopping hotspot..." else "Starting hotspot..."
            isHotspotEnabled -> "Hotspot & Proxy are active"
            else -> "Hotspot & Proxy are inactive"
        }
        Icon(
            imageVector = statusIcon,
            contentDescription = statusText,
            tint = if (isHotspotEnabled) Color(0xFF4CAF50) else Color(0xFFF44336) // Green or Red
        )
        Button(
            onClick = {
                onStopTapped()
                // Stop ProxyService
                val intent = Intent(context, ProxyService::class.java)
                context.stopService(intent)
            },
            enabled = isHotspotEnabled && !isProcessing
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Stop Hotspot & Proxy")
            }
        }
    }
}

