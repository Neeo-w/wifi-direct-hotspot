package com.example.wifip2photspot.VPN

import android.app.Application
import android.content.Intent
// VpnRepository.kt (Extended)


import android.content.Context

class VpnRepository(private val context: Context) {

    private var isVpnActive: Boolean = false

    fun startVpn() {
        if (!isVpnActive) {
            val vpnIntent = Intent(context, MyVpnService::class.java)
            context.startService(vpnIntent)
            isVpnActive = true
        }
    }

    fun stopVpn() {
        if (isVpnActive) {
            val vpnIntent = Intent(context, MyVpnService::class.java)
            context.stopService(vpnIntent)
            isVpnActive = false
        }
    }

    fun isVpnActive(): Boolean {
        return isVpnActive
    }
}
