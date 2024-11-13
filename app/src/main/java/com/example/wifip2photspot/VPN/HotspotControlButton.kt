package com.example.wifip2photspot.VPN

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// HotspotControlButton.kt
@Composable
fun HotspotControlButton(
    isHotspotEnabled: Boolean,
    isProcessing: Boolean,
    onStartTapped: () -> Unit,
    onStopTapped: () -> Unit
) {
    Button(
        onClick = {
            if (isHotspotEnabled) {
                onStopTapped()
            } else {
                onStartTapped()
            }
        },
        enabled = !isProcessing,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isHotspotEnabled) Color.Red else Color.Green
        )
    ) {
        if (isProcessing) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 8.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
            Text(text = if (isHotspotEnabled) "Stopping..." else "Starting...")
        } else {
            Text(text = if (isHotspotEnabled) "Stop Hotspot & VPN" else "Start Hotspot & VPN")
        }
    }
}
