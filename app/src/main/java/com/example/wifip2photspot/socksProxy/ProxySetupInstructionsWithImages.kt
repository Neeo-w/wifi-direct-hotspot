package com.example.wifip2photspot.socksProxy
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ProxySetupInstructionsWithImages.kt
@Composable
fun ProxySetupInstructionsWithImages() {
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
        StepItem(stepNumber = 1, description = "Connect to the hotspot.")
        StepItem(stepNumber = 2, description = "Go to your device's Wi-Fi settings.")
        StepItem(stepNumber = 3, description = "Modify the connected Wi-Fi network.")
        StepItem(stepNumber = 4, description = "Set the proxy to Manual.")
        StepItem(stepNumber = 5, description = "Enter the following details:")
        Text(text = "   - Proxy hostname: 192.168.49.1")
        Text(text = "   - Proxy port: 8181")
        StepItem(stepNumber = 6, description = "Save the settings.")
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your device is now configured to route traffic through the secure SSH tunnel and proxy.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun StepItem(stepNumber: Int, description: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = "$stepNumber.",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
