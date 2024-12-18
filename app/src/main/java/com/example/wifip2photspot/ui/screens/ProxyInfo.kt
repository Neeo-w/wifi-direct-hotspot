package com.example.wifip2photspot.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProxyInfo(
    proxyIp: String,
    proxyPort: Int,
    isProxyRunning: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Proxy Information", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "IP Address: $proxyIp")
        Text(text = "Port: $proxyPort")
        Text(
            text = if (isProxyRunning) "Status: Running" else "Status: Stopped",
            color = if (isProxyRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
    }
}
