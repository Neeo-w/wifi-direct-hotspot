package com.example.wifip2photspot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager

class WifiDirectBroadcastReceiver(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    private val activity: MainActivity
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        when (action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                // Check to see if Wi-Fi is enabled and notify appropriate activity
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    activity.setIsWifiP2pEnabled(true)
                } else {
                    activity.setIsWifiP2pEnabled(false)
                }
            }
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                // Respond to new connection or disconnections
                val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                if (networkInfo != null && networkInfo.isConnected) {
                    // We are connected with the other device, request connection info to find group owner IP
                    manager.requestConnectionInfo(channel) { info ->
                        // Use info.groupOwnerAddress to get the IP address
                        // You can also get group info to get the list of connected devices
                        manager.requestGroupInfo(channel) { group ->
                            if (group != null) {
                                val deviceList = group.clientList
                                activity.onDevicesChanged(deviceList)
                            }
                        }
                    }
                } else {
                    // Disconnected
                    activity.onDevicesChanged(emptyList())
                }
            }
            // Handle other actions if necessary
        }
    }
}
