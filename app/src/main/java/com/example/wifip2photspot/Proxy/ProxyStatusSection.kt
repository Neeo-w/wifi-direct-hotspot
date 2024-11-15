package com.example.wifip2photspot.Proxy



import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProxyStatusSection(
    isProxyRunning: Boolean,
    proxyIP: String,
    proxyPort: Int
) {
    Card(
        elevation = CardDefaults.cardElevation(4.dp),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Proxy Server Status", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isProxyRunning) "Running" else "Stopped",
                color = if (isProxyRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (isProxyRunning) {
                Text("Proxy IP: $proxyIP", style = MaterialTheme.typography.bodyMedium)
                Text("Proxy Port: $proxyPort", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
