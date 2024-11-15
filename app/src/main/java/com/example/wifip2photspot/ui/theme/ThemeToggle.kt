package com.example.wifip2photspot.ui.theme

// ThemeToggle.kt

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.wifip2photspot.SwitchPreference

@Composable
fun ThemeToggle(
    isDarkTheme: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
//        Text("Dark Theme", style = MaterialTheme.typography.bodyLarge)
        SwitchPreference(
            label = "Dark Theme",
            checked = isDarkTheme,
            onCheckedChange = onToggle

        )
    }
}

