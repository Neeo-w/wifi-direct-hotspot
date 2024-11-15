package com.example.wifip2photspot.Proxy

// ProxyLimitationsSection.kt


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProxyLimitationsSection() {
    Card(
        elevation = CardDefaults.cardElevation(4.dp),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Proxy Limitations", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "• Only HTTP and HTTPS traffic is supported.",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "• Other protocols may not function correctly.",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "• Manual proxy configuration is required on connected devices.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
