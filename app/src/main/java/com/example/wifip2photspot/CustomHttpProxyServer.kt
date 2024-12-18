//package com.example.wifip2photspot
//
//import android.util.Log
//import java.io.*
//import java.net.ServerSocket
//import java.net.Socket
//import java.util.concurrent.Executors
//
//class ProxyServer(private val port: Int, private val bindIp: String = "192.168.49.1") {
//    private val TAG = "ProxyServer"
//
//    private val clientHandlingExecutor = Executors.newFixedThreadPool(50) // Adjust pool size as needed
//    private val dataForwardingExecutor = Executors.newCachedThreadPool()
//
//    private var serverSocket: ServerSocket? = null
//    private var isRunning = false
//
//    // Start the Proxy server and begin accepting connections
//    fun start() {
//        try {
//            serverSocket = ServerSocket(port)
//            Log.d(TAG, "Proxy server started on port $port, binding to $bindIp")
//            isRunning = true
//
//            // Listening for incoming client connections
//            Thread {
//                while (isRunning) {
//                    try {
//                        val clientSocket = serverSocket!!.accept()
//                        Log.d(TAG, "Client connected: ${clientSocket.inetAddress}")
//                        clientHandlingExecutor.execute { handleClient(clientSocket) }
//                    } catch (e: Exception) {
//                        Log.e(TAG, "Error accepting client connection: ${e.message}")
//                    }
//                }
//            }.start()
//
//        } catch (e: IOException) {
//            Log.e(TAG, "Error starting proxy server: ${e.message}")
//        }
//    }
//
//    // Stop the Proxy server
//    fun stop() {
//        isRunning = false
//        serverSocket?.close()
//        Log.d(TAG, "Proxy server stopped.")
//    }
//
//    // Handle client requests by reading them and forwarding to the target server
//    private fun handleClient(clientSocket: Socket) {
//        val targetSocket: Socket?
//
//        try {
//            val clientInput = clientSocket.getInputStream()
//            val clientOutput = clientSocket.getOutputStream()
//
//            // Read the client's request
//            val requestBuffer = ByteArray(8192)  // Buffer to hold the request
//            var bytesRead: Int = clientInput.read(requestBuffer)
//
//            if (bytesRead == -1) {
//                clientSocket.close()
//                return
//            }
//
//            val requestHeader = String(requestBuffer, 0, bytesRead)
//            val requestLines = requestHeader.split("\r\n")
//            if (requestLines.isEmpty()) {
//                clientSocket.close()
//                return
//            }
//
//            val requestLine = requestLines[0]
//            val tokens = requestLine.split(" ")
//
//            if (tokens.size < 2) {
//                clientSocket.close()
//                return
//            }
//
//            val method = tokens[0]
//            val url = tokens[1]
//
//            Log.d(TAG, "Request received: $method $url")
//
//            // Forward the request to the target server
//            targetSocket = Socket(bindIp, 80)  // Connect to the server (using default HTTP port)
//            val targetOutput = targetSocket.getOutputStream()
//            targetOutput.write(requestBuffer, 0, bytesRead)  // Forward the request
//
//            // Handle the response from the target server
//            val targetInput = targetSocket.getInputStream()
//            val responseBuffer = ByteArray(8192)
//
//            while (true) {
//                bytesRead = targetInput.read(responseBuffer)
//                if (bytesRead == -1) break
//                clientOutput.write(responseBuffer, 0, bytesRead)
//            }
//
//            targetSocket.close()
//            clientSocket.close()
//
//        } catch (e: Exception) {
//            Log.e(TAG, "Error handling client request: ${e.message}")
//            try {
//                clientSocket.close()
//            } catch (ex: IOException) {
//                Log.e(TAG, "Error closing client socket: ${ex.message}")
//            }
//        }
//    }
//}
