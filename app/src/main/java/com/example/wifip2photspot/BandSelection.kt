package com.example.wifip2photspot

import androidx.compose.foundation.clickable
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
fun BandSelection(
    selectedBand: String,
    onBandSelected: (String) -> Unit,
    bands: List<String>,
    isHotspotEnabled: Boolean
) {
    Column(modifier = Modifier.padding(8.dp)) {
        Text("Wi-Fi Band Selection", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        bands.forEach { band ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = isHotspotEnabled.not()) { onBandSelected(band) }
                    .padding(vertical = 4.dp)
            ) {
                RadioButton(
                    selected = selectedBand == band,
                    onClick = { onBandSelected(band) },
                    enabled = isHotspotEnabled.not()
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(band, style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (isHotspotEnabled) {
            Text(
                text = "Cannot change band while hotspot is active.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}