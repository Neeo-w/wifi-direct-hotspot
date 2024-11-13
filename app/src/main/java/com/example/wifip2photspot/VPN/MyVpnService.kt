// MyVpnService.kt
package com.example.wifip2photspot.VPN

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.*
import kotlinx.coroutines.NonCancellable.isActive
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.io.FileInputStream
import java.io.FileOutputStream


import kotlinx.coroutines.launch

import java.net.Socket

class MyVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        establishVpn()
        return START_STICKY
    }

    private fun establishVpn() {
        val builder = Builder()
            .setSession("WiFiP2PHotspot VPN")
            .addAddress("10.0.0.2", 24) // Virtual IP for Device A
            .addRoute("0.0.0.0", 0) // Route all traffic through VPN
            .addDnsServer("8.8.8.8")
            .setMtu(1500)

        vpnInterface = builder.establish()

        if (vpnInterface != null) {
            coroutineScope.launch {
                try {
                    handleVpnTraffic(vpnInterface!!)
                } catch (e: Exception) {
                    e.printStackTrace()
                    stopSelf()
                }
            }
        } else {
            // Handle failure to establish VPN
            stopSelf()
        }
    }

    private suspend fun handleVpnTraffic(fd: ParcelFileDescriptor) {
        val inputStream = FileInputStream(fd.fileDescriptor)
        val outputStream = FileOutputStream(fd.fileDescriptor)
        val buffer = ByteArray(32767)

        try {
            while (isActive) { // isActive is available within CoroutineScope
                val length = withContext(Dispatchers.IO) {
                    try {
                        inputStream.read(buffer)
                    } catch (e: IOException) {
                        e.printStackTrace()
                        -1
                    }
                }

                if (length > 0) {
                    // Extract destination IP and port from packet (requires parsing)
                    val destinationIp = extractDestinationIp(buffer, length)
                    val destinationPort = extractDestinationPort(buffer, length)

                    // Forward packet to the destination server
                    val response = forwardPacketToServer(buffer, length, destinationIp, destinationPort)

                    // Write the response back to the VPN interface
                    withContext(Dispatchers.IO) {
                        try {
                            outputStream.write(response, 0, response.size)
                            outputStream.flush()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                } else if (length == -1) {
                    // End of stream or error
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            // Clean up streams
            try {
                inputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            try {
                outputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            stopSelf()
        }
    }

    private fun extractDestinationIp(buffer: ByteArray, length: Int): String {
        // Implement packet parsing to extract destination IP address
        // Placeholder implementation
        return "8.8.8.8"
    }

    private fun extractDestinationPort(buffer: ByteArray, length: Int): Int {
        // Implement packet parsing to extract destination port
        // Placeholder implementation
        return 53
    }

    private fun forwardPacketToServer(
        buffer: ByteArray,
        length: Int,
        destinationIp: String,
        destinationPort: Int
    ): ByteArray {
        // Implement logic to forward the packet to the destination server
        // For example, create a socket connection and send the data

        return try {
            val socket = Socket(destinationIp, destinationPort)
            val output = socket.getOutputStream()
            val input = socket.getInputStream()

            output.write(buffer, 0, length)
            output.flush()

            val responseBuffer = ByteArray(32767)
            val responseLength = input.read(responseBuffer)

            socket.close()

            if (responseLength > 0) {
                responseBuffer.copyOf(responseLength)
            } else {
                ByteArray(0)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            ByteArray(0)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            vpnInterface?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        coroutineScope.cancel()
    }
}





