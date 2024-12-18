package com.example.wifip2photspot

// IpHeader.kt
import java.net.InetAddress
import java.nio.ByteBuffer

class IpHeader(private val buffer: ByteBuffer) {
    val version: Int
    val headerLength: Int
    val totalLength: Int
    val protocol: Int
    val sourceAddress: InetAddress
    val destinationAddress: InetAddress

    init {
        version = (buffer.get(0).toInt() shr 4) and 0x0F
        headerLength = (buffer.get(0).toInt() and 0x0F) * 4
        totalLength = buffer.getShort(2).toInt()
        protocol = buffer.get(9).toInt() and 0xFF
        val srcAddr = ByteArray(4)
        buffer.position(12)
        buffer.get(srcAddr)
        sourceAddress = InetAddress.getByAddress(srcAddr)
        val destAddr = ByteArray(4)
        buffer.get(destAddr)
        destinationAddress = InetAddress.getByAddress(destAddr)
    }

    companion object {
        const val PROTOCOL_TCP = 6
        const val PROTOCOL_UDP = 17
    }
}
