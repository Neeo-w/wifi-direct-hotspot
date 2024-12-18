//package com.example.wifip2photspot
//
//import android.os.ParcelFileDescriptor
//import com.example.wifip2photspot.MyVpnService
//import java.io.InputStream
//import java.io.OutputStream
//import java.net.HttpURLConnection
//import java.net.InetSocketAddress
//import java.net.ServerSocket
//import java.net.Socket
//import java.net.URL
//import java.nio.ByteBuffer
//import java.nio.channels.FileChannel
//
//// Function to start the proxy server and integrate with VPN
//fun startProxyServer(vpnService: MyVpnService) {
//    val serverSocket = ServerSocket(8080)  // Listen for HTTP/HTTPS requests on port 8080
//    println("Proxy Server started, listening on port 8080...")
//
//    while (true) {
//        // Accept incoming client connections
//        val clientSocket = serverSocket.accept()
//
//        // Handle each client request in a separate thread
//        Thread {
//            try {
//                handleClientRequest(clientSocket, vpnService)
//            } catch (e: Exception) {
//                println("Error handling client request: ${e.message}")
//            } finally {
//                clientSocket.close()
//            }
//        }.start()
//    }
//}
//
//// Function to handle HTTP/HTTPS requests from the connected device (via Wi-Fi Direct)
//fun handleClientRequest(clientSocket: Socket, vpnService: MyVpnService) {
//    val inputStream = clientSocket.getInputStream()
//    val outputStream = clientSocket.getOutputStream()
//
//    val buffer = ByteArray(4096)
//    var bytesRead: Int
//
//    // Read incoming request from the client
//    bytesRead = inputStream.read(buffer)
//    if (bytesRead > 0) {
//        // Here, you'd send the request through the VPN tunnel and get a response
//
//        // For example, forwarding HTTP request via VPN (through the proxy)
//        val requestData = String(buffer, 0, bytesRead)
//
//        // Log the incoming request data (for debugging purposes)
//        println("Received Request: $requestData")
//
//        // TODO: Forward the request through VPN (use VpnService to send/receive traffic via VPN)
//
//        // For now, we'll just forward the request to the destination server via the Internet (no VPN for now)
//        // This part should be modified to send the request through VPN
//        val url = URL("http://example.com") // Replace with actual destination URL
//        val connection = url.openConnection() as HttpURLConnection
//        connection.requestMethod = "GET" // Assuming GET request for simplicity
//        connection.connect()
//
//        // Read the response from the server
//        val serverInputStream = connection.inputStream
//        val responseBuffer = ByteArray(4096)
//        var bytesReadFromServer = serverInputStream.read(responseBuffer)
//
//        // Write the server response back to the client
//        while (bytesReadFromServer != -1) {
//            outputStream.write(responseBuffer, 0, bytesReadFromServer)
//            bytesReadFromServer = serverInputStream.read(responseBuffer)
//        }
//
//        // Close the input/output streams
//        serverInputStream.close()
//        outputStream.flush()
//    }
//
//
//    /// Function to handle HTTP/HTTPS requests from the connected device (via Wi-Fi Direct)
//    fun handleClientRequest(clientSocket: Socket, vpnService: MyVpnService) {
//        val inputStream = clientSocket.getInputStream()
//        val outputStream = clientSocket.getOutputStream()
//
//        val buffer = ByteArray(4096)
//        var bytesRead: Int
//
//        // Read incoming request from the client
//        bytesRead = inputStream.read(buffer)
//        if (bytesRead > 0) {
//            // Here, you'd send the request through the VPN tunnel and get a response
//
//            // For example, forwarding HTTP request via VPN (through the proxy)
//            val requestData = String(buffer, 0, bytesRead)
//
//            // TODO: Forward the request through VPN (use VpnService to send/receive traffic via VPN)
//
//            // For demonstration, just echoing the data back to the client (this part is a placeholder)
//            outputStream.write(buffer, 0, bytesRead)
//            outputStream.flush()
//        }
//    }}
