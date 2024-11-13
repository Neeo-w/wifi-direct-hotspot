package com.example.wifip2photspot.socksProxy

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


@Composable
fun ProxyInfoDisplay(
    proxyIp: String = "192.168.49.1",
    proxyPort: Int = 8181
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Proxy Configuration",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "IP Address: $proxyIp")
        Text(text = "Port: $proxyPort")
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Configure your client device's proxy settings to use the above IP and port.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

//// SSHConfigurationDialog.kt
//@Composable
//fun SSHConfigurationDialog(
//    isOpen: Boolean,
//    onDismiss: () -> Unit,
//    onSave: (username: String, password: String, host: String, port: Int) -> Unit
//) {
//    if (isOpen) {
//        var username by remember { mutableStateOf("") }
//        var password by remember { mutableStateOf("") }
//        var host by remember { mutableStateOf("") }
//        var port by remember { mutableStateOf("22") }
//
//        AlertDialog(
//            onDismissRequest = onDismiss,
//            title = { Text(text = "SSH Configuration") },
//            text = {
//                Column {
//                    OutlinedTextField(
//                        value = username,
//                        onValueChange = { username = it },
//                        label = { Text("Username") },
//                        modifier = Modifier.fillMaxWidth()
//                    )
//                    Spacer(modifier = Modifier.height(8.dp))
//                    OutlinedTextField(
//                        value = password,
//                        onValueChange = { password = it },
//                        label = { Text("Password") },
//                        modifier = Modifier.fillMaxWidth(),
//                        visualTransformation = PasswordVisualTransformation()
//                    )
//                    Spacer(modifier = Modifier.height(8.dp))
//                    OutlinedTextField(
//                        value = host,
//                        onValueChange = { host = it },
//                        label = { Text("SSH Host") },
//                        modifier = Modifier.fillMaxWidth()
//                    )
//                    Spacer(modifier = Modifier.height(8.dp))
//                    OutlinedTextField(
//                        value = port,
//                        onValueChange = { port = it },
//                        label = { Text("SSH Port") },
//                        modifier = Modifier.fillMaxWidth(),
//                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
//                    )
//                }
//            },
//            confirmButton = {
//                Button(onClick = {
//                    onSave(username, password, host, port.toIntOrNull() ?: 22)
//                    onDismiss()
//                }) {
//                    Text("Save")
//                }
//            },
//            dismissButton = {
//                OutlinedButton(onClick = onDismiss) {
//                    Text("Cancel")
//                }
//            }
//        )
//    }
//}
// SSHStatusDisplay.kt
//@Composable
//fun SSHStatusDisplay(
//    isServerRunning: Boolean,
//    isClientConnected: Boolean
//) {
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(16.dp)
//    ) {
//        Text(
//            text = "SSH Status",
//            style = MaterialTheme.typography.titleMedium,
//            fontWeight = FontWeight.Bold
//        )
//        Spacer(modifier = Modifier.height(8.dp))
//        Row(verticalAlignment = Alignment.CenterVertically) {
//            Icon(
//                imageVector = if (isServerRunning) Icons.Filled.CheckCircle else Icons.Filled.Error,
//                contentDescription = "SSH Server Status",
//                tint = if (isServerRunning) Color.Green else Color.Red
//            )
//            Spacer(modifier = Modifier.width(8.dp))
//            Text(
//                text = if (isServerRunning) "SSH Server Running" else "SSH Server Stopped",
//                color = if (isServerRunning) Color.Green else Color.Red
//            )
//        }
//        Spacer(modifier = Modifier.height(8.dp))
//        Row(verticalAlignment = Alignment.CenterVertically) {
//            Icon(
//                imageVector = if (isClientConnected) Icons.Filled.CheckCircle else Icons.Filled.Error,
//                contentDescription = "SSH Client Status",
//                tint = if (isClientConnected) Color.Green else Color.Red
//            )
//            Spacer(modifier = Modifier.width(8.dp))
//            Text(
//                text = if (isClientConnected) "SSH Client Connected" else "SSH Client Disconnected",
//                color = if (isClientConnected) Color.Green else Color.Red
//            )
//        }
//    }
//}
