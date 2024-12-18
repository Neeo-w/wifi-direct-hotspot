// MainActivity.kt
package com.example.wifip2photspot

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModelProvider
import com.example.wifip2photspot.ui.screens.PermissionDeniedDialog
import com.example.wifip2photspot.ui.screens.ServiceDisabledDialog
import com.example.wifip2photspot.ui.theme.WiFiP2PHotspotTheme
import com.example.wifip2photspot.viewModel.HotspotViewModel
import com.example.wifip2photspot.viewModel.HotspotViewModelFactory
import com.example.wifip2photspot.viewModel.WifiDirectBroadcastReceiver
import timber.log.Timber

class MainActivity : ComponentActivity() {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

    private lateinit var hotspotViewModel: HotspotViewModel
    private lateinit var wifiP2pManager: WifiP2pManager
    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var receiver: WifiDirectBroadcastReceiver
    private var receiverRegistered = false

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val deniedPermissions = permissions.filter { !it.value }.keys
            if (deniedPermissions.isEmpty()) {
                initializeWifiP2p()
            } else {
                showPermissionRationaleDialog(deniedPermissions.toList())
            }
        }

    private var showPermissionDialog by mutableStateOf(false)
    private var deniedPermissionsList by mutableStateOf<List<String>>(emptyList())
    private var showWifiDisabledDialog by mutableStateOf(false)
    private var showLocationDisabledDialog by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create the HotspotViewModel using a Factory if needed
        hotspotViewModel = ViewModelProvider(
            this,
            HotspotViewModelFactory(application as Application, dataStore)
        ).get(HotspotViewModel::class.java)

        // Request permissions for Wi-Fi and location
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.INTERNET
            )
        )

        setContent {
            val isDarkTheme by hotspotViewModel.isDarkTheme.collectAsState()
            WiFiP2PHotspotTheme(useDarkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WiFiP2PHotspotApp(hotspotViewModel = hotspotViewModel)

                    LaunchedEffect(Unit) {
                        if (checkAndRequestPermissions()) {
                            initializeWifiP2p()
                        }
                    }

                    // Show dialogs for Wi-Fi, Location, and permissions
                    if (showWifiDisabledDialog) {
                        ServiceDisabledDialog(
                            serviceName = "Wi-Fi",
                            onDismiss = { showWifiDisabledDialog = false },
                            onConfirm = { openWifiSettings() }
                        )
                    }

                    if (showLocationDisabledDialog) {
                        ServiceDisabledDialog(
                            serviceName = "Location",
                            onDismiss = { showLocationDisabledDialog = false },
                            onConfirm = { openLocationSettings() }
                        )
                    }

                    if (showPermissionDialog) {
                        PermissionDeniedDialog(
                            deniedPermissions = deniedPermissionsList,
                            onDismiss = { showPermissionDialog = false },
                            onConfirm = {
                                showPermissionDialog = false
                                requestPermissions(deniedPermissionsList)
                            },
                            onOpenSettings = {
                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", packageName, null)
                                )
                                startActivity(intent)
                                showPermissionDialog = false
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isWifiEnabled()) {
            showWifiDisabledDialog = true
        } else if (!isLocationEnabled()) {
            showLocationDisabledDialog = true
        } else {
            if (checkAndRequestPermissions()) {
                initializeWifiP2p()
            }
        }

        if (this::wifiP2pManager.isInitialized && this::channel.isInitialized) {
            val intentFilter = IntentFilter().apply {
                addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            }
            receiver = WifiDirectBroadcastReceiver(wifiP2pManager, channel, hotspotViewModel)
            registerReceiver(receiver, intentFilter)
            receiverRegistered = true
        }

    }

    override fun onPause() {
        super.onPause()
        if (receiverRegistered) {
            unregisterReceiver(receiver)
            receiverRegistered = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the Wi-Fi Direct broadcast receiver when the activity is destroyed
        if (this::receiver.isInitialized && receiverRegistered) {
            unregisterReceiver(receiver)
        }
    }

    private fun checkAndRequestPermissions(): Boolean {
        val requiredPermissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }

        return if (requiredPermissions.isNotEmpty()) {
            requestPermissions(requiredPermissions)
            false
        } else {
            true
        }
    }

    private fun requestPermissions(permissions: List<String>) {
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun showPermissionRationaleDialog(deniedPermissions: List<String>) {
        deniedPermissionsList = deniedPermissions
        showPermissionDialog = true
    }

    private fun initializeWifiP2p() {
        if (!isWifiEnabled()) {
            showWifiDisabledDialog = true
            return
        }

        if (!isLocationEnabled()) {
            showLocationDisabledDialog = true
            return
        }

        wifiP2pManager = getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
        channel = wifiP2pManager.initialize(this, mainLooper, null)
    }

    private fun isWifiEnabled(): Boolean {
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        return wifiManager.isWifiEnabled
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun openWifiSettings() {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
        startActivity(intent)
        showWifiDisabledDialog = false
    }

    private fun openLocationSettings() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(intent)
        showLocationDisabledDialog = false
    }
}
