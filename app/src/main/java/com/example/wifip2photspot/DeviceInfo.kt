package com.example.wifip2photspot

import android.net.wifi.p2p.WifiP2pDevice
import java.time.LocalDate

// DeviceInfo.kt
data class DeviceInfo(
    val device: WifiP2pDevice,
    val alias: String? = null,
    val connectionTime: Long = System.currentTimeMillis(),
    val ipAddress: String? = null,
    val isBlocked: Boolean = false
)





