package com.example.wifip2photspot.ui

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext

import android.content.Context


class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        // Notification channels are only available in Android 8.0 (API level 26) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Device Connection Notifications"
            val descriptionText = "Notifications related to device connections and VPN status."
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("device_connection_channel", name, importance).apply {
                description = descriptionText
            }

            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}


@Composable
fun NotificationSettingsAlert(showAlert: Boolean, onDismiss: () -> Unit) {
    if (showAlert) {
        val context = LocalContext.current
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Enable Notifications") },
            text = { Text("Please enable notifications in your device settings to stay updated on device connections.") },
            confirmButton = {
                TextButton(onClick = {
                    // Open app notification settings
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                    onDismiss()
                }) {
                    Text("Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}
