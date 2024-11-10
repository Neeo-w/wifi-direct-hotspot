package com.example.wifip2photspot.ui
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import com.example.wifip2photspot.SwitchPreference

@Composable
fun IdleCountdownDisplay(remainingIdleTime: Long) {
    if (remainingIdleTime > 0L) {
        val minutes = (remainingIdleTime / 1000) / 60
        val seconds = (remainingIdleTime / 1000) % 60
        val timeString = String.format("%02d:%02d", minutes, seconds)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Idle Warning",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Hotspot will shut down in: $timeString",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}



@Composable
fun IdleSettingsSection(
    autoShutdownEnabled: Boolean,
    onAutoShutdownEnabledChange: (Boolean) -> Unit,
    idleTimeoutMinutes: Int,
    onIdleTimeoutChange: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(8.dp)) {
        Text("Idle Settings", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        SwitchPreference(
            label = "Auto Shutdown when Idle",
            checked = autoShutdownEnabled,
            onCheckedChange = onAutoShutdownEnabledChange
        )

        if (autoShutdownEnabled) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Idle Timeout (minutes): $idleTimeoutMinutes", style = MaterialTheme.typography.bodyLarge)
            Slider(
                value = idleTimeoutMinutes.toFloat(),
                onValueChange = { onIdleTimeoutChange(it.toInt()) },
                valueRange = 1f..60f,
                steps = 59
            )
        }
    }
}