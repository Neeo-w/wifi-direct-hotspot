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
                // Wi-Fi P2P state has changed (enabled/disabled)
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                val isEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                viewModel.setWifiP2pEnabled(isEnabled)
                viewModel.updateLog("Wi-Fi P2P Enabled: $isEnabled")
            }

            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                // Respond to connection changes (group owner/client)
                Timber.tag(TAG).d("WIFI_P2P_CONNECTION_CHANGED_ACTION received")
                val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)

                // Check for a valid network info and connection status
                networkInfo?.let {
                    if (it.isConnected) {
                        manager.requestConnectionInfo(channel) { info ->
                            if (info.groupFormed && info.isGroupOwner) {
                                // Device is the Group Owner
                                Timber.tag(TAG).d("Group Owner: %s", info.groupOwnerAddress.hostAddress)
                                CoroutineScope(Dispatchers.Main).launch {
                                    viewModel.onGroupOwnerChanged(true)
//                                    viewModel.startVpn()  // Automatically start VPN
                                }

                                // Request group info and update connected devices
                                manager.requestGroupInfo(channel) { group ->
                                    group?.let {
                                        val ssid = it.networkName
                                        val passphrase = it.passphrase
                                        viewModel.updateLog("Group Info: SSID=$ssid, Passphrase=$passphrase")
                                        viewModel.onDevicesChanged(it.clientList)
                                        viewModel.updateLog("Connected Devices: ${it.clientList.size}")
                                    }
                                }
                            } else {
                                // Device is a client
                                Timber.tag(TAG).d("Connected as a client.")
                                CoroutineScope(Dispatchers.Main).launch {
                                    viewModel.onGroupOwnerChanged(false)
//                                    viewModel.stopVpn()  // Automatically stop VPN
                                }
                            }
                        }
                    } else {
                        // Disconnected from the group
                        Timber.tag(TAG).d("Disconnected from group.")
                        viewModel.onDevicesChanged(emptyList())
                        viewModel.updateLog("Disconnected from group.")

                        // Stop VPN on disconnection
                        CoroutineScope(Dispatchers.Main).launch {
                            viewModel.onDisconnected()
//                            viewModel.stopVpn()  // Stop VPN
                        }
                    }
                }
            }

            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                // This device's Wi-Fi state has changed
                Timber.tag(TAG).d("WIFI_P2P_THIS_DEVICE_CHANGED_ACTION received")
                // Optionally update ViewModel or log device state changes if necessary
            }
        }
    }
}
