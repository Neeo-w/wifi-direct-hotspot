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
import timber.log.Timber


class WifiDirectBroadcastReceiver(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    private val viewModel: HotspotViewModel
) : BroadcastReceiver() {

    private val TAG = "WifiDirectBroadcastReceiver"

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
                Timber.tag(TAG).d("WIFI_P2P_CONNECTION_CHANGED_ACTION received")
                val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                if (networkInfo != null && networkInfo.isConnected) {
                    // Connected to a P2P network
                    manager.requestConnectionInfo(channel) { info ->
                        if (info.groupFormed && info.isGroupOwner) {
                            // Inform ViewModel that the device is now the Group Owner
                            Timber.tag(TAG).d("Group Owner: %s", info.groupOwnerAddress.hostAddress)
                            CoroutineScope(Dispatchers.Main).launch {
                                viewModel.onGroupOwnerChanged(true)
                            }

                            // Request group info and update connected devices
                            manager.requestGroupInfo(channel) { group ->
                                if (group != null) {
                                    val ssid = group.networkName
                                    val passphrase = group.passphrase
                                    viewModel.updateLog("Group Info: SSID=$ssid, Passphrase=$passphrase")
                                    viewModel.onDevicesChanged(group.clientList)
                                    viewModel.updateLog("Connected Devices: ${group.clientList.size}")
                                }
                            }
                        } else {
                            Timber.tag(TAG).d("Connected as a client.")
                            // Inform ViewModel that the device is a client
                            CoroutineScope(Dispatchers.Main).launch {
                                viewModel.onGroupOwnerChanged(false)
                            }
                        }
                    }
                } else {
                    // Disconnected from a P2P network
                    Timber.tag(TAG).d("Disconnected from group.")
                    viewModel.onDevicesChanged(emptyList())
                    viewModel.updateLog("Disconnected from group.")

                    // Inform ViewModel to handle disconnection
                    CoroutineScope(Dispatchers.Main).launch {
                        viewModel.onDisconnected()
                    }
                }
            }

            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                // Optionally handle changes to this device's Wi-Fi state
                Timber.tag(TAG).d("WIFI_P2P_THIS_DEVICE_CHANGED_ACTION received")
                // Update ViewModel or log device state changes if necessary
            }
        }
    }
}
