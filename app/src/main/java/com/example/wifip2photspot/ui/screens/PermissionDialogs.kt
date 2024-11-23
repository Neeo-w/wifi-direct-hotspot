// File: PermissionDialogs.kt
package com.example.wifip2photspot.ui.screens

import android.Manifest
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun PermissionDeniedDialog(
    deniedPermissions: List<String>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val message = buildString {
        append("The app requires the following permissions for proper functionality:\n\n")
        deniedPermissions.forEach { permission ->
            when (permission) {
                Manifest.permission.ACCESS_FINE_LOCATION -> append("- Access Fine Location\n")
                Manifest.permission.NEARBY_WIFI_DEVICES -> append("- Nearby Wi-Fi Devices\n")
                else -> append("- $permission\n")
            }
        }
        append("\nPlease grant these permissions.")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Permissions Required") },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Grant Permissions")
            }
        },
        dismissButton = {
            TextButton(onClick = onOpenSettings) {
                Text("Open Settings")
            }
        }
    )
}
