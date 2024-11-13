// ProxyInstructionsSection.kt
package com.example.wifip2photspot.Proxy

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ProxyInstructionsSection(
    proxyIP: String,
    proxyPort: Int
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Section Title with Icon
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Instructions Icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Configure Connected Devices",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = Color.Gray, thickness = 1.dp)

            Spacer(modifier = Modifier.height(12.dp))

            // Instructions List
            Column(modifier = Modifier.fillMaxWidth()) {
                InstructionStep(
                    stepNumber = 1,
                    icon = Icons.Default.Wifi,
                    description = "Go to Wi-Fi settings on your device."
                )
                InstructionStep(
                    stepNumber = 2,
                    icon = Icons.Default.Wifi,
                    description = "Long-press the connected hotspot network and select 'Modify network'."
                )
                InstructionStep(
                    stepNumber = 3,
                    icon = Icons.Default.Wifi,
                    description = "Expand 'Advanced options'."
                )
                InstructionStep(
                    stepNumber = 4,
                    icon = Icons.Default.Wifi,
                    description = "Under 'Proxy', select 'Manual'."
                )
                InstructionStep(
                    stepNumber = 5,
                    icon = Icons.Default.Info,
                    description = "Enter the Proxy hostname and Proxy port as below:"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            // Display Proxy Configuration
            ProxyConfiguration(proxyIP = proxyIP, proxyPort = proxyPort)

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color.Gray, thickness = 1.dp)

            Spacer(modifier = Modifier.height(12.dp))
            // Additional Notes
            Text(
                text = "Note:",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "• Only HTTP and HTTPS traffic is supported.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "• Manual proxy configuration is required on each connected device.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "• Ensure the proxy port is not blocked by any firewall or security settings.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun InstructionStep(
    stepNumber: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = "$stepNumber.",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.width(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = icon,
            contentDescription = "Step $stepNumber Icon",
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ProxyConfiguration(proxyIP: String, proxyPort: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Proxy Hostname:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = proxyIP,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF1E88E5) // Blue color for emphasis
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Proxy Port:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = proxyPort.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF1E88E5) // Blue color for emphasis
            )
        }
    }
}
