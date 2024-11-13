package com.example.wifip2photspot.socksProxy


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ProxySetupInstructions.kt
@Composable
fun ProxySetupInstructions() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Proxy Setup Instructions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "1. Connect to the hotspot.")
        Text(text = "2. Go to your device's Wi-Fi settings.")
        Text(text = "3. Modify the connected Wi-Fi network.")
        Text(text = "4. Set the proxy to Manual.")
        Text(text = "5. Enter the following details:")
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "   - Proxy hostname: 192.168.49.1")
        Text(text = "   - Proxy port: 8181")
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "6. Save the settings.")
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your device is now configured to route traffic through the secure SSH tunnel and proxy.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
