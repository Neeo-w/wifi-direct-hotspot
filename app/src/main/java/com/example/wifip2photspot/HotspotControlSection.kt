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
//import com.example.wifip2photspot.Proxy.ProxyService

@Composable
fun HotspotControlSection(
    isHotspotEnabled: Boolean,
    isProcessing: Boolean,
    ssidInput: String,
    passwordInput: String,
    selectedBand: String,
    onStartTapped: () -> Unit,
    onStopTapped: () -> Unit,
    proxyPort: Int,

    ) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
//        Button(
//            onClick = {
//                onStartTapped()
//                // Start ProxyService
//                val intent = Intent(context, ProxyService::class.java)
//                context.startService(intent)
//            },
//            enabled = !isHotspotEnabled && !isProcessing
//        )
        // Control Buttons
        Spacer(modifier = Modifier.height(8.dp))

        // Proxy Port Display (optional)
        Text(text = "Proxy Port: $proxyPort")

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (isHotspotEnabled) {
                    onStopTapped()
                } else {
                    onStartTapped()
                }
            },
            enabled = !isProcessing,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isHotspotEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        )
        {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 8.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Text(text = if (isHotspotEnabled) "Stopping..." else "Starting...")
            } else {
                Text(text = if (isHotspotEnabled) "Stop WiFi-Direct" else "Start WiFi-Direct")
            }
        }
    }
//        {
//            if (isProcessing) {
//                CircularProgressIndicator(
//                    modifier = Modifier.size(20.dp),
//                    strokeWidth = 2.dp,
//                    color = MaterialTheme.colorScheme.onPrimary
//                )
//            } else {
//                Text("Start Hotspot & Proxy")
//            }
//        }

//    val statusIcon = when {
//        isProcessing -> Icons.Default.Sync
//        isHotspotEnabled -> Icons.Default.Wifi
//        else -> Icons.Default.WifiOff
//    }
//    val statusText = when {
//        isProcessing -> if (isHotspotEnabled) "Stopping hotspot..." else "Starting hotspot..."
//        isHotspotEnabled -> "Hotspot & Proxy are active"
//        else -> "Hotspot & Proxy are inactive"
//    }
//    Icon(
//        imageVector = statusIcon,
//        contentDescription = statusText,
//        tint = if (isHotspotEnabled) Color(0xFF4CAF50) else Color(0xFFF44336) // Green or Red
//    )
//    Button(
//        onClick = {
//            onStopTapped()
//            // Stop ProxyService
//            val intent = Intent(context, ProxyService::class.java)
//            context.stopService(intent)
//        },
//        enabled = isHotspotEnabled && !isProcessing
//    ) {
//        if (isProcessing) {
//            CircularProgressIndicator(
//                modifier = Modifier.size(20.dp),
//                strokeWidth = 2.dp,
//                color = MaterialTheme.colorScheme.onPrimary
//            )
//        } else {
//            Text("Stop Hotspot & Proxy")
//        }
//    }
}


