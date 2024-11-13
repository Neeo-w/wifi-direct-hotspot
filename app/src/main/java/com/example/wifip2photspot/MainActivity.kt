// MainActivity.kt
package com.example.wifip2photspot

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModelProvider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
//import com.example.wifip2photspot.Proxy.ProxyService
//import com.example.wifip2photspot.Proxy.ProxyService.Companion.CHANNEL_ID
import com.example.wifip2photspot.ui.theme.WiFiP2PHotspotTheme
import com.example.wifip2photspot.viewModel.HotspotViewModel
import com.example.wifip2photspot.viewModel.HotspotViewModelFactory
import com.example.wifip2photspot.viewModel.VpnViewModel
import com.example.wifip2photspot.viewModel.VpnViewModelFactory
import com.example.wifip2photspot.viewModel.WifiDirectBroadcastReceiver
import com.example.wifip2photspot.VPN.VpnRepository
import android.app.Application
import android.content.Context

//private val Context.dataStore by preferencesDataStore(name = "settings")

class MainActivity : ComponentActivity() {
    private lateinit var hotspotViewModel: HotspotViewModel
    private lateinit var vpnViewModel: VpnViewModel
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

//    // Define the permission launcher as a member variable
//    private val requestPermissionLauncher = registerForActivityResult(
//        ActivityResultContracts.RequestPermission()
//    ) { isGranted: Boolean ->
//        if (isGranted) {
//            // Permission granted, proceed with notifications
//            // You can send a confirmation toast or log if needed
//        } else {
//            // Permission denied, show alert to guide user
//            showNotificationSettingsAlert.value = true
//        }
//    }
//    // Mutable state to control the visibility of NotificationSettingsAlert
//    private val showNotificationSettingsAlert = mutableStateOf(false)


//    private lateinit var viewModel: HotspotViewModel

    private lateinit var receiver: WifiDirectBroadcastReceiver
    private lateinit var intentFilter: IntentFilter

    //    private lateinit var receiver: BroadcastReceiver
    private lateinit var wifiManager: WifiP2pManager
    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var notificationManager: NotificationManager


    private val VPN_REQUEST_CODE = 1001

    private val channelName = "Data Usage Alerts"
    private val channelDescription = "Notifications for data usage thresholds"
    private val importance = NotificationManager.IMPORTANCE_HIGH



    @OptIn(ExperimentalComposeUiApi::class)
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Request Notification Permission for Android 13+

//
//        // Create notification channels
//        createNotificationChannel()

        // Initialize Repository
        val vpnRepository = VpnRepository(application)
//        val dataStore = YourDataStoreType(this) // Initialize appropriately

        System.setProperty("user.home", filesDir.absolutePath)

// Initialize VpnViewModel
        val vpnViewModelFactory = VpnViewModelFactory(application, dataStore, vpnRepository)
        vpnViewModel = ViewModelProvider(this, vpnViewModelFactory).get(VpnViewModel::class.java)

        // Initialize HotspotViewModel
        val hotspotViewModelFactory = HotspotViewModelFactory(application, dataStore, vpnRepository)
        hotspotViewModel =
            ViewModelProvider(this, hotspotViewModelFactory).get(HotspotViewModel::class.java)

        wifiManager =
            applicationContext.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = wifiManager.initialize(this, mainLooper, null)

//        // Initialize NotificationManager
//        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        // Mutable state to control the visibility of NotificationSettingsAlert
//        val showNotificationSettingsAlert = mutableStateOf(false)

        // Define the permission launcher as a member variable
//        val requestPermissionLauncher = registerForActivityResult(
//            ActivityResultContracts.RequestPermission()
//        ) { isGranted: Boolean ->
//            if (isGranted) {
//                // Permission granted, proceed with notifications
//            } else {
//                // Permission denied, show alert to guide user
//                showNotificationSettingsAlert.value = true
//            }
//        }
        // Initialize the BroadcastReceiver
        receiver = WifiDirectBroadcastReceiver(
            manager = wifiManager, // Ensure wifiManager is initialized
            channel = channel, // Ensure channel is initialized
            viewModel = hotspotViewModel
        )

        // Initialize IntentFilter
        intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }

//        // Initialize DataStore
//        val dataStore = applicationContext.dataStore

        // Initialize ViewModel with Factory
//        viewModel = ViewModelProvider(
//            this,
//            HotspotViewModelFactory(application, dataStore,vpnRepository)
//        )[HotspotViewModel::class.java]

        // Initialize and Register BroadcastReceiver
//        receiver = WifiDirectBroadcastReceiver(
//            manager = viewModel.wifiManager,
//            channel = viewModel.channel,
//            viewModel = viewModel
//        )
//        intentFilter = IntentFilter().apply {
//            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
//            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
//            // Add other actions if necessary
//        }
//        intentFilter = IntentFilter().apply {
//            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
//            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
//            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
//            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
//        }


//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channel = NotificationChannel(CHANNEL_ID, channelName, importance).apply {
//                description = channelDescription
//            }
//            // Register the channel with the system
//            val notificationManager =
//                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            notificationManager.createNotificationChannel(channel)
//        }



//        // Request necessary permissions
//        if (!allPermissionsGranted()) {
//            requestPermissions()
//        }

//        setContent {
//            // Theme State
//            var isDarkTheme by rememberSaveable { mutableStateOf(false) }
//            WiFiP2PHotspotTheme(useDarkTheme = isDarkTheme) {
//                Surface(
//                    modifier = Modifier.fillMaxSize(),
//                    color = MaterialTheme.colorScheme.background
//                ) {
//                    PermissionHandler {
//                        WiFiP2PHotspotApp(
//                            viewModel = viewModel,
//                            activity = this,
//                            isDarkTheme = isDarkTheme,
//                            onThemeChange = { isDark ->
//                                isDarkTheme = isDark
//                            })
//                        // Start or stop the proxy based on the hotspot state
//                        if (isEnabled) {
//                            startProxyService()
//                        } else {
//                            stopProxyService()
//                        }
//                    }
//                }
//            }
//        }
//        val viewModel = ViewModelProvider(
//            this,
//            HotspotViewModelFactory(application, dataStore,vpnRepository)
//        ).get(HotspotViewModel::class.java)
        // Start VPN service with user consent
        val vpnIntent = VpnService.prepare(this)
        if (vpnIntent != null) {
            startActivityForResult(vpnIntent, VPN_REQUEST_CODE)
        } else {
            // VPN permissions already granted
            vpnRepository.startVpn()
        }



        setContent {
            val isDarkTheme by hotspotViewModel.isDarkTheme.collectAsState()
            WiFiP2PHotspotTheme(useDarkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current
                    var showAlert by remember { mutableStateOf(false) }
                    // Set up NavHost
                    WiFiP2PHotspotApp(
                        hotspotViewModel = hotspotViewModel,
                        vpnViewModel = vpnViewModel
                    )

//                    // Check if notifications are enabled
//                    LaunchedEffect(key1 = true) {
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
//                                // Notifications are disabled, show alert
//                                showNotificationSettingsAlert.value = true
//                            } else {
//                                // Notifications are enabled, request notification permission
//                                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS
//                                )
//                            }
//                        } else {
//                            // For devices below Android 13, no runtime permission required
//                        }
//                    }

                }
            }
        }
    }

//    private fun createNotificationChannel() {
//        // Notification channels are only available in Android 8.0 (API level 26) and above
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val name = "Device Connection Notifications"
//            val descriptionText = "Notifications related to device connections and VPN status."
//            val importance = NotificationManager.IMPORTANCE_DEFAULT
//            val channel = NotificationChannel("device_connection_channel", name, importance).apply {
//                description = descriptionText
//            }
//
//            // Register the channel with the system
//            val notificationManager: NotificationManager =
//                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            notificationManager.createNotificationChannel(channel)
//        }
//    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == VPN_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
//            vpnRepository.startVpn()
//        }
//    }
//    private fun startProxyService() {
//        val intent = Intent(this, ProxyService::class.java)
//        startService(intent)
//    }
//
//    private fun stopProxyService() {
//        val intent = Intent(this, ProxyService::class.java)
//        stopService(intent)
//    }

    override fun onDestroy() {
        super.onDestroy()
        // Ensure services are stopped
    }
//    override fun onResume() {
//        super.onResume()
//        // Usage of receiver
//        registerReceiver(receiver, IntentFilter)
//    }

    override fun onPause() {
        super.onPause()
        // Unregister receiver
        unregisterReceiver(receiver)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(receiver, intentFilter)
    }
//
//    override fun onPause() {
//        super.onPause()
//        unregisterReceiver(receiver)
//    }

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

