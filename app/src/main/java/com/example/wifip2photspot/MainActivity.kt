// MainActivity.kt
package com.example.wifip2photspot
import android.content.Context
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModelProvider
import com.example.wifip2photspot.ui.theme.WiFiP2PHotspotTheme
import com.example.wifip2photspot.viewModel.HotspotViewModel
import com.example.wifip2photspot.viewModel.HotspotViewModelFactory
import com.example.wifip2photspot.viewModel.WifiDirectBroadcastReceiver

class MainActivity : ComponentActivity() {
    private lateinit var hotspotViewModel: HotspotViewModel
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
    private lateinit var receiver: WifiDirectBroadcastReceiver
    private lateinit var intentFilter: IntentFilter
    private lateinit var wifiManager: WifiP2pManager
    private lateinit var channel: WifiP2pManager.Channel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize HotspotViewModel
        val hotspotViewModelFactory = HotspotViewModelFactory(application, dataStore)
        hotspotViewModel =
            ViewModelProvider(this, hotspotViewModelFactory)[HotspotViewModel::class.java]

        wifiManager =
            applicationContext.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = wifiManager.initialize(this, mainLooper, null)
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

        setContent {
            val isDarkTheme by hotspotViewModel.isDarkTheme.collectAsState()
            WiFiP2PHotspotTheme(useDarkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Set up NavHost
                    WiFiP2PHotspotApp(
                        hotspotViewModel = hotspotViewModel
                    )

                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Unregister receiver
        unregisterReceiver(receiver)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(receiver, intentFilter)
    }
}
