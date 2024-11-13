// WifiDirectBroadcastReceiver.kt
package com.example.wifip2photspot.viewModel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WifiDirectBroadcastReceiver(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    private val viewModel: HotspotViewModel
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action

        when (action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                // Check if Wi-Fi P2P is enabled
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                val isEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                viewModel.setWifiP2pEnabled(isEnabled)
                viewModel.updateLog("Wi-Fi P2P Enabled: $isEnabled")
            }

            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                // Respond to new connection or disconnections
                val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                if (networkInfo != null && networkInfo.isConnected) {
                    // Connected to a P2P network
                    manager.requestConnectionInfo(channel) { info ->
                        if (info.groupFormed && info.isGroupOwner) {
                            viewModel.updateLog("Group Owner: ${info.groupOwnerAddress.hostAddress}")
                            // Inform ViewModel that the device is now the Group Owner
                            CoroutineScope(Dispatchers.Main).launch {
                                viewModel.onGroupOwnerChanged(true)
                            }
                        } else {
                            viewModel.updateLog("Connected as a client.")
                            // Inform ViewModel that the device is a client
                            CoroutineScope(Dispatchers.Main).launch {
                                viewModel.onGroupOwnerChanged(false)
                            }
                        }

                        // Request group info to get connected devices
                        manager.requestGroupInfo(channel) { group ->
                            if (group != null) {
                                val deviceList = group.clientList
                                viewModel.onDevicesChanged(deviceList)
                                viewModel.updateLog("Connected Devices: ${deviceList.size}")
                            }
                        }
                    }
                } else {
                    // Disconnected from a P2P network
                    viewModel.onDevicesChanged(emptyList())
                    viewModel.updateLog("Disconnected from group.")
                    // Inform ViewModel to handle disconnection
                    CoroutineScope(Dispatchers.Main).launch {
                        viewModel.onDisconnected()
                    }
                }
            }

            // Handle other actions if necessary
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                // Optionally handle changes to this device's Wi-Fi state
                // For example, update device details in the UI
                // Not essential for VPN functionality
            }
        }
    }
}

