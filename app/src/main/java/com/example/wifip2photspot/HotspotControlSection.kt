// HotspotControlSection.kt
package com.example.wifip2photspot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
        // Control Buttons
        Spacer(modifier = Modifier.height(8.dp))

        // Proxy Port Display (optional)

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
}