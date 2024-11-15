package com.example.wifip2photspot.VPN


import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor

class MyVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        establishVpn()
        return START_STICKY
    }

    private fun establishVpn() {
        Builder().apply {
            setSession("WiFiP2PHotspot VPN")
            addAddress("10.0.0.2", 32)
            addRoute("0.0.0.0", 0)
        }.establish()?.let {
            vpnInterface = it
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        vpnInterface?.close()
        vpnInterface = null
    }
}
