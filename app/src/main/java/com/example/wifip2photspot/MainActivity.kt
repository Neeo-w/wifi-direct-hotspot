// MainActivity.kt
package com.example.wifip2photspot

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModelProvider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.contentcapture.ContentCaptureManager.Companion.isEnabled
import com.example.wifip2photspot.Proxy.ProxyService
import com.example.wifip2photspot.Proxy.ProxyService.Companion.CHANNEL_ID
import com.example.wifip2photspot.ui.theme.WiFiP2PHotspotTheme

private val Context.dataStore by preferencesDataStore(name = "settings")

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: HotspotViewModel
    private lateinit var receiver: WifiDirectBroadcastReceiver
    private lateinit var intentFilter: IntentFilter

    private val channelName = "Data Usage Alerts"
    private val channelDescription = "Notifications for data usage thresholds"
    private val importance = NotificationManager.IMPORTANCE_HIGH

    @OptIn(ExperimentalComposeUiApi::class)
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize DataStore
        val dataStore = applicationContext.dataStore

        // Initialize ViewModel with Factory
        viewModel = ViewModelProvider(
            this,
            HotspotViewModelFactory(application, dataStore)
        )[HotspotViewModel::class.java]

        // Initialize and Register BroadcastReceiver
        receiver = WifiDirectBroadcastReceiver(
            manager = viewModel.wifiManager,
            channel = viewModel.channel,
            viewModel = viewModel
        )
//        intentFilter = IntentFilter().apply {
//            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
//            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
//            // Add other actions if necessary
//        }
        intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, channelName, importance).apply {
                description = channelDescription
            }
            // Register the channel with the system
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }



//        // Request necessary permissions
//        if (!allPermissionsGranted()) {
//            requestPermissions()
//        }

        setContent {
            // Theme State
            var isDarkTheme by rememberSaveable { mutableStateOf(false) }
            WiFiP2PHotspotTheme(useDarkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PermissionHandler {
                        WiFiP2PHotspotApp(
                            viewModel = viewModel,
                            activity = this,
                            isDarkTheme = isDarkTheme,
                            onThemeChange = { isDark ->
                                isDarkTheme = isDark
                            })
                        // Start or stop the proxy based on the hotspot state
                        if (isEnabled) {
                            startProxyService()
                        } else {
                            stopProxyService()
                        }
                    }
                }
            }
        }
    }
//    // In your Application class or MainActivity
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//        val name = "Data Usage Alerts"
//        val descriptionText = "Notifications for data usage thresholds"
//        val importance = NotificationManager.IMPORTANCE_HIGH
//        val channel = NotificationChannel("data_usage_channel", name, importance).apply {
//            description = descriptionText
//        }
//        val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        notificationManager.createNotificationChannel(channel)
//    }

    private fun startProxyService() {
        val intent = Intent(this, ProxyService::class.java)
        startService(intent)
    }

    private fun stopProxyService() {
        val intent = Intent(this, ProxyService::class.java)
        stopService(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Ensure services are stopped
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(receiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }

}

//    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
//        ContextCompat.checkSelfPermission(
//            baseContext, it
//        ) == PackageManager.PERMISSION_GRANTED
//    }
//
//    private fun requestPermissions() {
//        // Check if we should show a rationale
//        val shouldShowRationale = REQUIRED_PERMISSIONS.any { permission ->
//            ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
//        }
//
//        if (shouldShowRationale) {
//            // Show a dialog explaining why the permissions are needed
//            AlertDialog.Builder(this)
//                .setTitle("Permissions Required")
//                .setMessage("This app requires location and Wi-Fi permissions to function correctly.")
//                .setPositiveButton("OK") { dialog, _ ->
//                    ActivityCompat.requestPermissions(
//                        this,
//                        REQUIRED_PERMISSIONS,
//                        PERMISSION_REQUEST_CODE
//                    )
//                    dialog.dismiss()
//                }
//                .setNegativeButton("Cancel") { dialog, _ ->
//                    Toast.makeText(
//                        this,
//                        "Permissions not granted by the user.",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                    dialog.dismiss()
//                    finish()
//                }
//                .create()
//                .show()
//        } else {
//            // Directly request permissions
//            ActivityCompat.requestPermissions(
//                this,
//                REQUIRED_PERMISSIONS,
//                PERMISSION_REQUEST_CODE
//            )
//        }
//    }
//
//    companion object {
//        private const val PERMISSION_REQUEST_CODE = 1
//        private val REQUIRED_PERMISSIONS = arrayOf(
//            Manifest.permission.ACCESS_FINE_LOCATION,
//            Manifest.permission.CHANGE_WIFI_STATE,
//            Manifest.permission.ACCESS_WIFI_STATE,
//            Manifest.permission.INTERNET,
//            Manifest.permission.ACCESS_NETWORK_STATE,
//            // Add NEARBY_WIFI_DEVICES if targeting Android 13+
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                Manifest.permission.NEARBY_WIFI_DEVICES
//            } else {
//                null
//            }
//        ).filterNotNull().toTypedArray()
//    }
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<String>,
//        grantResults: IntArray
//    ) {
//        if (requestCode == PERMISSION_REQUEST_CODE) {
//            if (allPermissionsGranted()) {
//                // Permissions granted, proceed as normal
//                Toast.makeText(
//                    this,
//                    "Permissions granted.",
//                    Toast.LENGTH_SHORT
//                ).show()
//            } else {
//                // Permissions denied
//                Toast.makeText(
//                    this,
//                    "Permissions not granted by the user.",
//                    Toast.LENGTH_SHORT
//                ).show()
//                // Optionally, direct the user to app settings
//                AlertDialog.Builder(this)
//                    .setTitle("Permissions Denied")
//                    .setMessage("You have denied some permissions. Allow all permissions at [Settings] > [Permissions]")
//                    .setPositiveButton("Open Settings") { dialog, _ ->
//                        val intent =
//                            android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
//                        val uri: android.net.Uri =
//                            android.net.Uri.fromParts("package", packageName, null)
//                        intent.data = uri
//                        startActivity(intent)
//                        dialog.dismiss()
//                    }
//                    .setNegativeButton("Exit") { dialog, _ ->
//                        dialog.dismiss()
//                        finish()
//                    }
//                    .create()
//                    .show()
//            }
//        }
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//    }
//}
