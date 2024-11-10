// PermissionHandler.kt
package com.example.wifip2photspot

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun PermissionHandler(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    var showRationale by remember { mutableStateOf(false) }

    val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.CHANGE_WIFI_STATE,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.NEARBY_WIFI_DEVICES,
        Manifest.permission.CHANGE_NETWORK_STATE,
        Manifest.permission.FOREGROUND_SERVICE,
//        Manifest.permission.VIBRATE,
//        Manifest.permission.BIND_VPN_SERVICE
    )

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissionsGranted ->
            val allGranted = permissionsGranted.all { it.value }
            if (!allGranted) {
                showRationale = true
            }
        }
    )

    LaunchedEffect(key1 = true) {
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (!allGranted) {
            launcher.launch(permissions)
        }
    }

    if (showRationale) {
        // Show a dialog or UI to explain why permissions are needed
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Permissions Required") },
            text = { Text("This app requires location and Wi-Fi permissions to function correctly.") },
            confirmButton = {
                Button(onClick = { launcher.launch(permissions) }) {
                    Text("Grant Permissions")
                }
            },
            dismissButton = {
                Button(onClick = { /* Handle denial */ }) {
                    Text("Cancel")
                }
            }
        )
    } else {
        content()
    }
}
//
//@Composable
//fun RequestPermissions(onPermissionsGranted: () -> Unit) {
//    val context = LocalContext.current
//    val lifecycleOwner = LocalLifecycleOwner.current
//
//    val permissionState = rememberMultiplePermissionsState(
//        permissions = listOf(
//            android.Manifest.permission.ACCESS_FINE_LOCATION,
//            android.Manifest.permission.CHANGE_WIFI_STATE,
//            android.Manifest.permission.ACCESS_WIFI_STATE
//        )
//    )
//
//    LaunchedEffect(key1 = true) {
//        permissionState.launchMultiplePermissionRequest()
//    }
//
//    when {
//        permissionState.allPermissionsGranted -> {
//            onPermissionsGranted()
//        }
//        permissionState.shouldShowRationale -> {
//            // Show rationale and request permission again
//            AlertDialog(
//                onDismissRequest = { /* Handle dismiss */ },
//                title = { Text("Permissions Required") },
//                text = { Text("This app requires location and Wi-Fi permissions to function correctly.") },
//                confirmButton = {
//                    TextButton(onClick = { permissionState.launchMultiplePermissionRequest() }) {
//                        Text("Grant")
//                    }
//                },
//                dismissButton = {
//                    TextButton(onClick = { /* Handle dismiss */ }) {
//                        Text("Cancel")
//                    }
//                }
//            )
//        }
//        else -> {
//            // Permissions denied permanently
//            AlertDialog(
//                onDismissRequest = { /* Handle dismiss */ },
//                title = { Text("Permissions Denied") },
//                text = { Text("Please enable permissions from settings.") },
//                confirmButton = {
//                    TextButton(onClick = { /* Open app settings */ }) {
//                        Text("Settings")
//                    }
//                },
//                dismissButton = {
//                    TextButton(onClick = { /* Handle dismiss */ }) {
//                        Text("Cancel")
//                    }
//                }
//            )
//        }
//    }
//}
