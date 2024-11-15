package com.example.wifip2photspot.ui

import androidx.compose.foundation.layout.*

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
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