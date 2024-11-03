// LogSection.kt
package com.example.wifip2photspot

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LogSection(logEntries: List<String>) {
    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Text("Log:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(8.dp)
        ) {
            Text(
                text = logEntries.joinToString("\n"),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
