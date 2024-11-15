package com.example.wifip2photspot.Proxy

// ProxyControlSection.kt

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// ProxyControlSection.kt
@Composable
fun ProxyControlSection(
    isProxyRunning: Boolean,
    isProcessing: Boolean,
    onStartProxy: () -> Unit,
    onStopProxy: () -> Unit,
    onChangeProxyPort: (Int) -> Unit,
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
            Text("Proxy Server Controls", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = onStartProxy,
                    enabled = !isProxyRunning && !isProcessing
                ) {
                    if (isProcessing && !isProxyRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Start Proxy")
                    }
                }

                Button(
                    onClick = onStopProxy,
                    enabled = isProxyRunning && !isProcessing
                ) {
                    if (isProcessing && isProxyRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Stop Proxy")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Optionally, allow users to change the proxy port
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Proxy Port:", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = proxyPort.toString(),
                    onValueChange = { newPort ->
                        newPort.toIntOrNull()?.let {
                            onChangeProxyPort(it)
                        }
                    },
                    label = { Text("Port") },
                    singleLine = true,
                    modifier = Modifier.width(100.dp)
                )
            }
        }
    }
}

