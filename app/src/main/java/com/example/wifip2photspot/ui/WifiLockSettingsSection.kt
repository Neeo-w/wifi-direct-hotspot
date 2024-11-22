package com.example.wifip2photspot.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.wifip2photspot.SwitchPreference


@Composable
fun WifiLockSettingsSection(
    wifiLockEnabled: Boolean,
    onWifiLockEnabledChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.padding(8.dp)) {
        Text("Wi-Fi Lock", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        SwitchPreference(
            label = "Keep Wi-Fi Awake",
            checked = wifiLockEnabled,
            onCheckedChange = onWifiLockEnabledChange
        )
        Text(
            text = "Enable this to prevent the Wi-Fi from going to sleep while the hotspot is active.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
