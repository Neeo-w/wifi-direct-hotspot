package com.example.wifip2photspot

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.ContentAlpha

@Composable
fun NotificationSettingsSection(
    notificationEnabled: Boolean,
    onNotificationEnabledChange: (Boolean) -> Unit,
    soundEnabled: Boolean,
    onSoundEnabledChange: (Boolean) -> Unit,
    vibrationEnabled: Boolean,
    onVibrationEnabledChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Notification Settings", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        SwitchPreference(
            label = "Enable Notifications",
            checked = notificationEnabled,
            onCheckedChange = onNotificationEnabledChange
        )

        SwitchPreference(
            label = "Sound",
            checked = soundEnabled,
            onCheckedChange = onSoundEnabledChange,
            enabled = notificationEnabled
        )

        SwitchPreference(
            label = "Vibration",
            checked = vibrationEnabled,
            onCheckedChange = onVibrationEnabledChange,
            enabled = notificationEnabled
        )
    }
}

@Composable
fun SwitchPreference(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}
