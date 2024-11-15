package com.example.wifip2photspot

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LogSection(logEntries: List<String>) {
    // Define the maximum number of log entries to display
    val maxLogEntries = 100

    // Remember only the last 'maxLogEntries' logs to avoid excessive display content
    val truncatedLog = remember(logEntries) {
        if (logEntries.size > maxLogEntries) {
            logEntries.takeLast(maxLogEntries)
        } else {
            logEntries
        }
    }

    // Display logs in a Card with some styling and scrollable column
    Card(
        elevation = CardDefaults.cardElevation(4.dp),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Logs",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 200.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Display logs or fallback message
                Text(
                    text = truncatedLog.joinToString("\n").ifEmpty { "No logs available." },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}