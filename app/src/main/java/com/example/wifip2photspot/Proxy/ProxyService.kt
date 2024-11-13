// File: app/src/main/java/com/example/vpnshare/service/ProxyService.kt

package com.example.wifip2photspot.Proxy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.wifip2photspot.R
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import java.security.cert.CertificateFactory
import java.security.KeyStore
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.SSLContext

enum class ConnectionStatus {
    CONNECTED,
    CONNECTING,
    DISCONNECTED,
    ERROR
}

//class ProxyService : VpnService() {
//
//    private var vpnInterface: ParcelFileDescriptor? = null
//    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
//
//    companion object {
//        const val CHANNEL_ID = "ProxyServiceChannel"
//        const val NOTIFICATION_ID = 1
//        const val PROXY_SERVER_ADDRESS = "your.proxy.server.address" // Replace with your proxy server address
//        const val PROXY_SERVER_PORT = 443 // Typically SSL port
//        const val SOCKS_PROXY_PORT = 1080 // Port for SOCKS proxy
//    }
//
//    private var socksProxyJob: Job? = null
//
//    override fun onCreate() {
//        super.onCreate()
//        createNotificationChannel()
//        startForeground(NOTIFICATION_ID, buildNotification())
//    }
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        // Start the foreground service here
//        startForeground(NOTIFICATION_ID, buildNotification())
//
//        serviceScope.launch {
//            try {
//                setupVpn()
//                manageVpnTraffic()
//                startSocksProxy()
//            } catch (e: Exception) {
//                Timber.e(e, "Error in ProxyService")
//                stopSelf()
//            }
//        }
//        return START_STICKY
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        vpnInterface?.close()
//        vpnInterface = null
//        serviceScope.cancel()
//        stopSocksProxy()
//    }
//
//    /**
//     * Sets up the VPN interface with specified IP and routing.
//     */
//    private fun setupVpn() {
//        val builder = Builder()
//        builder.setSession("VPNShare")
//            .addAddress("10.0.0.2", 24) // VPN interface IP
//            .addRoute("0.0.0.0", 0) // Route all traffic through VPN
//            .setMtu(1500)
//            .establish()?.let { vpnInterface = it }
//            ?: throw IOException("Failed to establish VPN interface")
//        Timber.d("VPN interface established")
//    }
//
//    /**
//     * Manages the traffic between the VPN interface and the remote proxy server.
//     */
//    private suspend fun manageVpnTraffic() {
//        vpnInterface?.fileDescriptor?.let { fd ->
//            val input = FileInputStream(fd).buffered()
//            val output = FileOutputStream(fd).buffered()
//
//            // Update connection status to CONNECTING
//            updateConnectionStatus(ConnectionStatus.CONNECTING)
//            val sslSocket = createSSLSocket(PROXY_SERVER_ADDRESS, PROXY_SERVER_PORT)
//                ?: throw IOException("Failed to create SSL socket")
//            // Update connection status to CONNECTED
//            updateConnectionStatus(ConnectionStatus.CONNECTED)
//            Timber.d("Connected to Proxy Server at $PROXY_SERVER_ADDRESS:$PROXY_SERVER_PORT")
//
//            val proxyInput = sslSocket.inputStream.buffered()
//            val proxyOutput = sslSocket.outputStream.buffered()
//
//            // Launch coroutine to read from VPN and write to Proxy
//            val vpnToProxy = serviceScope.launch {
//                try {
//                    val buffer = ByteArray(4096)
//                    var bytesRead: Int
//                    while (true) {
//                        bytesRead = input.read(buffer)
//                        if (bytesRead == -1) break
//                        proxyOutput.write(buffer, 0, bytesRead)
//                        proxyOutput.flush()
//                        updateDataUsage(bytesRead.toLong())
//                        Timber.d("Forwarded $bytesRead bytes to proxy")
//                    }
//                } catch (e: Exception) {
//                    Timber.e(e, "Error forwarding data to proxy")
//                    updateConnectionStatus(ConnectionStatus.ERROR)
//                }
//            }
//
//            // Launch coroutine to read from Proxy and write to VPN
//            val proxyToVpn = serviceScope.launch {
//                try {
//                    val buffer = ByteArray(4096)
//                    var bytesRead: Int
//                    while (true) {
//                        bytesRead = proxyInput.read(buffer)
//                        if (bytesRead == -1) break
//                        output.write(buffer, 0, bytesRead)
//                        output.flush()
//                        updateDataUsage(bytesRead.toLong())
//                        Timber.d("Received $bytesRead bytes from proxy")
//                    }
//                } catch (e: Exception) {
//                    Timber.e(e, "Error receiving data from proxy")
//                    updateConnectionStatus(ConnectionStatus.ERROR)
//                }
//            }
//
//            // Wait for both coroutines to finish
//            vpnToProxy.join()
//            proxyToVpn.join()
//
//            // Clean up
//            sslSocket.close()
//            updateConnectionStatus(ConnectionStatus.DISCONNECTED)
//            Timber.d("Proxy connection closed")
//        } ?: throw IOException("VPN Interface not established")
//    }
//
//    /**
//     * Creates an SSL socket connected to the remote proxy server.
//     */
//    private fun createSSLSocket(serverAddress: String, serverPort: Int): SSLSocket? {
//        return try {
//            val sslSocketFactory = createPinnedSSLSocketFactory()
//                ?: throw IOException("Failed to create pinned SSLSocketFactory")
//            val sslSocket = sslSocketFactory.createSocket() as SSLSocket
//            sslSocket.connect(InetSocketAddress(serverAddress, serverPort), 10000) // 10-second timeout
//            sslSocket.startHandshake()
//            Timber.d("SSL handshake completed with proxy server")
//            sslSocket
//        } catch (e: Exception) {
//            Timber.e(e, "Failed to create SSL socket")
//            null
//        }
//    }
//
//    /**
//     * Creates an SSL socket factory with a pinned certificate.
//     */
//    private fun createPinnedSSLSocketFactory(): SSLSocketFactory? {
//        return try {
//            // Load the trusted certificate from raw resources
//            val cf = CertificateFactory.getInstance("X.509")
//            val caInput = resources.openRawResource(R.raw.server_certificate) // Ensure you have this file in res/raw
//            val ca = cf.generateCertificate(caInput)
//            caInput.close()
//
//            // Create a KeyStore containing our trusted CAs
//            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
//            keyStore.load(null, null)
//            keyStore.setCertificateEntry("ca", ca)
//
//            // Create a TrustManager that trusts the CAs in our KeyStore
//            val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
//            tmf.init(keyStore)
//
//            // Create an SSLContext that uses our TrustManager
//            val sslContext = SSLContext.getInstance("TLS")
//            sslContext.init(null, tmf.trustManagers, null)
//
//            sslContext.socketFactory
//        } catch (e: Exception) {
//            Timber.e(e, "Failed to create pinned SSLSocketFactory")
//            null
//        }
//    }
//
//    /**
//     * Builds the notification for the foreground service.
//     */
//    private fun buildNotification(): Notification {
//        return NotificationCompat.Builder(this, CHANNEL_ID)
//            .setContentTitle("VPNShare Proxy Service")
//            .setContentText("Proxy is running")
//            .setSmallIcon(R.drawable.ic_proxy) // Ensure you have this icon in your resources
//            .setPriority(NotificationCompat.PRIORITY_LOW)
//            .setOngoing(true) // Makes the notification non-dismissible
//            .build()
//    }
//
//    /**
//     * Creates a notification channel for the service.
//     */
//    private fun createNotificationChannel() {
//        val serviceChannel = NotificationChannel(
//            CHANNEL_ID,
//            "VPN Proxy Service Channel",
//            NotificationManager.IMPORTANCE_LOW
//        ).apply {
//            description = "Channel for VPN Proxy Service"
//        }
//        val manager = getSystemService(NotificationManager::class.java)
//        manager.createNotificationChannel(serviceChannel)
//    }
//
//
//    /**
//     * Updates the data usage in SharedPreferences.
//     */
//    private fun updateDataUsage(bytes: Long) {
//        val sharedPreferences = getSharedPreferences("Proxy_Settings", Context.MODE_PRIVATE)
//        val currentUsage = sharedPreferences.getString("dataUsage", "0 MB") ?: "0 MB"
//        val currentBytes = parseDataUsage(currentUsage)
//        val totalBytes = currentBytes + bytes
//        val formattedUsage = formatBytes(totalBytes)
//        sharedPreferences.edit().putString("dataUsage", formattedUsage).apply()
//    }
//
//    /**
//     * Updates the connection status in SharedPreferences.
//     */
//    private fun updateConnectionStatus(status: ConnectionStatus) {
//        val sharedPreferences = getSharedPreferences("Proxy_Settings", Context.MODE_PRIVATE)
//        sharedPreferences.edit().putString("connectionStatus", status.name).apply()
//    }
//
//    /**
//     * Parses data usage string to bytes.
//     */
//    private fun parseDataUsage(dataUsage: String): Long {
//        return try {
//            when {
//                dataUsage.endsWith("GB") -> {
//                    (dataUsage.replace(" GB", "").toDouble() * 1024 * 1024 * 1024).toLong()
//                }
//                dataUsage.endsWith("MB") -> {
//                    (dataUsage.replace(" MB", "").toDouble() * 1024 * 1024).toLong()
//                }
//                dataUsage.endsWith("KB") -> {
//                    (dataUsage.replace(" KB", "").toDouble() * 1024).toLong()
//                }
//                else -> 0L
//            }
//        } catch (e: NumberFormatException) {
//            0L
//        }
//    }
//
//    /**
//     * Formats bytes to a human-readable string.
//     */
//    private fun formatBytes(bytes: Long): String {
//        return when {
//            bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
//            bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024))
//            bytes >= 1024 -> String.format("%.2f KB", bytes / 1024.0)
//            else -> "$bytes B"
//        }
//    }
//
//    /**
//     * Starts the SOCKS proxy server.
//     */
//    private fun startSocksProxy() {
//        socksProxyJob = serviceScope.launch {
//            val serverSocket = ServerSocket(SOCKS_PROXY_PORT)
//            Timber.d("SOCKS Proxy started on port $SOCKS_PROXY_PORT")
//            try {
//                while (isActive) {
//                    val clientSocket = serverSocket.accept()
//                    Timber.d("Accepted SOCKS Proxy client: ${clientSocket.inetAddress.hostAddress}")
//                    handleSocksClient(clientSocket)
//                }
//            } catch (e: IOException) {
//                Timber.e(e, "SOCKS Proxy encountered an error")
//            } finally {
//                serverSocket.close()
//            }
//        }
//    }
//
//    /**
//     * Handles incoming SOCKS proxy client connections.
//     */
//    private suspend fun handleSocksClient(clientSocket: Socket) {
//        serviceScope.launch {
//            try {
//                // Implement SOCKS5 handshake and proxying
//                val input = clientSocket.getInputStream().buffered()
//                val output = clientSocket.getOutputStream().buffered()
//
//                // Perform SOCKS5 handshake
//                // 1. Client sends: [SOCKS Version (0x05), Number of Methods]
//                val version = input.read()
//                if (version != 0x05) {
//                    clientSocket.close()
//                    return@launch
//                }
//                val nMethods = input.read()
//                val methods = ByteArray(nMethods)
//                input.read(methods)
//
//                // 2. Server selects NO AUTHENTICATION (0x00)
//                output.write(byteArrayOf(0x05, 0x00))
//                output.flush()
//
//                // 3. Client sends: [SOCKS Version (0x05), CMD, Reserved, ATYP, DST.ADDR, DST.PORT]
//                val version2 = input.read()
//                if (version2 != 0x05) {
//                    clientSocket.close()
//                    return@launch
//                }
//                val cmd = input.read()
//                val reserved = input.read()
//                val atyp = input.read()
//
//                if (cmd != 0x01) { // Only handle CONNECT command
//                    // Reply: Command not supported
//                    output.write(byteArrayOf(0x05, 0x07, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
//                    output.flush()
//                    clientSocket.close()
//                    return@launch
//                }
//
//                val dstAddr: String = when (atyp) {
//                    0x01 -> { // IPv4
//                        val addrBytes = ByteArray(4)
//                        input.read(addrBytes)
//                        addrBytes.joinToString(".") { (it.toInt() and 0xFF).toString() }.toString()
//                    }
//                    0x03 -> { // Domain name
//                        val domainLength = input.read()
//                        val domainBytes = ByteArray(domainLength)
//                        input.read(domainBytes)
//                        String(domainBytes)
//                    }
//                    0x04 -> { // IPv6
//                        val addrBytes = ByteArray(16)
//                        input.read(addrBytes)
//                        // Convert to IPv6 string representation
//                        java.net.InetAddress.getByAddress(addrBytes).hostAddress
//                    }
//                    else -> {
//                        // Address type not supported
//                        output.write(byteArrayOf(0x05, 0x08, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
//                        output.flush()
//                        clientSocket.close()
//                        return@launch
//                    }
//                }
//
//                val dstPort = input.read() shl 8 or input.read()
//
//                // Connect to destination server
//                val destSocket = Socket()
//                try {
//                    destSocket.connect(InetSocketAddress(dstAddr, dstPort), 10000) // 10-second timeout
//
//                    // Reply: Success
//                    val reply = ByteArray(10)
//                    reply[0] = 0x05
//                    reply[1] = 0x00
//                    reply[2] = 0x00
//                    reply[3] = 0x01
//                    reply[4] = 0x00
//                    reply[5] = 0x00
//                    reply[6] = 0x00
//                    reply[7] = 0x00
//                    reply[8] = 0x00
//                    reply[9] = 0x00
//                    output.write(reply)
//                    output.flush()
//
//                    // Launch bi-directional proxying
//                    val proxy1 = launch { proxyData(input, destSocket.getOutputStream()) }
//                    val proxy2 = launch { proxyData(destSocket.getInputStream(), output) }
//
//                    proxy1.join()
//                    proxy2.join()
//
//                } catch (e: Exception) {
//                    Timber.e(e, "Failed to connect to destination server")
//                    // Reply: Connection refused
//                    output.write(byteArrayOf(0x05, 0x05, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
//                    output.flush()
//                } finally {
//                    destSocket.close()
//                    clientSocket.close()
//                }
//
//            } catch (e: Exception) {
//                Timber.e(e, "Error handling SOCKS client")
//                clientSocket.close()
//            }
//        }
//    }
//
//    /**
//     * Proxies data between input and output streams.
//     */
//    private suspend fun proxyData(input: InputStream, output: OutputStream) {
//        try {
//            val buffer = ByteArray(4096)
//            var bytesRead: Int
//            while (true) {
//                bytesRead = input.read(buffer)
//                if (bytesRead == -1) break
//                output.write(buffer, 0, bytesRead)
//                output.flush()
//            }
//        } catch (e: IOException) {
//            // Connection closed or error
//        }
//    }
//
//    /**
//     * Stops the SOCKS proxy server.
//     */
//    private fun stopSocksProxy() {
//        socksProxyJob?.cancel()
//        socksProxyJob = null
//        Timber.d("SOCKS Proxy stopped")
//    }
//}
