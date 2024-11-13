package com.example.wifip2photspot.permissions

import android.net.VpnService
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.wifip2photspot.viewModel.VpnViewModel

// RequestVpnPermission.kt

@Composable
fun RequestVpnPermission(vpnViewModel: VpnViewModel) {
    val context = LocalContext.current
    val vpnIntent = remember { VpnService.prepare(context) }
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(vpnIntent) {
        if (vpnIntent != null) {
            // Show a dialog explaining why VPN permission is needed
            showDialog = true
        } else {
            // Permission already granted
            vpnViewModel.toggleVpn(true)
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { /* Handle dismissal */ },
            title = { Text("Allow VPN Connection") },
            text = { Text("This app requires VPN permission to route internet traffic through the hotspot securely.") },
            confirmButton = {
                TextButton(onClick = {
                    context.startActivity(vpnIntent)
                    showDialog = false
                }) {
                    Text("Allow")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Deny")
                }
            }
        )
    }
}
