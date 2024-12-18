//package com.example.wifip2photspot
////
//import android.util.Log
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import timber.log.Timber
//import java.io.*
//import java.net.HttpURLConnection
//import java.net.ServerSocket
//import java.net.Socket
//import java.net.URL
//
//class ProxyServer(private val port: Int = 8080) {
//
//    private var serverSocket: ServerSocket? = null
//    private val TAG = "ProxyServer"
//
//    // Start the proxy server
//    suspend fun start() {
//        withContext(Dispatchers.IO) { // Ensure network operations are done in the IO thread
//            try {
//                serverSocket = ServerSocket(port)
//                Timber.tag(TAG).d("Proxy server started on port %d", port)
//
//                // Listen for incoming client connections
//                while (!serverSocket!!.isClosed) {
//                    val clientSocket = serverSocket!!.accept()  // This is the blocking operation
//                    Timber.tag(TAG).d("Client connected: %s", clientSocket.inetAddress)
//                    handleClientRequest(clientSocket)
//                }
//            } catch (e: IOException) {
//                Timber.tag(TAG).e("Error starting proxy server: %s", e.message)
//            }
//        }
//    }
//
//    // Stop the proxy server
//    fun stop() {
//        try {
//            serverSocket?.close()
//            Timber.tag(TAG).d("Proxy server stopped.")
//        } catch (e: IOException) {
//            Timber.tag(TAG).e("Error stopping proxy server: %s", e.message)
//        }
//    }
//
//    // Handle incoming client requests
//    private fun handleClientRequest(clientSocket: Socket) {
//        Thread {
//            try {
//                val input = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
//                val output = clientSocket.getOutputStream()
//
//                // Read the first line of the HTTP request (Request Line)
//                val requestLine = input.readLine()
//                Timber.tag(TAG).d("Request: %s", requestLine)
//
//                // Parse the requested URL from the HTTP request
//                val url = parseRequestUrl(requestLine)
//
//                // If URL is valid, forward the request to the destination server
//                if (url != null) {
//                    forwardRequestToServer(requestLine, input, output, url)
//                } else {
//                    // Send a bad request response if the URL is invalid
//                    sendBadRequestResponse(output)
//                }
//
//                input.close()
//                output.close()
//                clientSocket.close()
//            } catch (e: IOException) {
//                Timber.tag(TAG).e("Error handling client request: %s", e.message)
//            }
//        }.start()
//    }
//
//    // Parse the requested URL from the HTTP request
//    private fun parseRequestUrl(requestLine: String): URL? {
//        try {
//            val parts = requestLine.split(" ")
//            if (parts.size >= 2) {
//                return URL(parts[1])  // The URL is the second part of the request line
//            }
//        } catch (e: Exception) {
//            Timber.tag(TAG).e("Invalid request URL: %s", e.message)
//        }
//        return null
//    }
//
//    // Forward the request to the destination server
//    private fun forwardRequestToServer(
//        requestLine: String,
//        input: BufferedReader,
//        output: OutputStream,
//        url: URL
//    ) {
//        try {
//            // Open connection to the remote server
//            val connection = url.openConnection() as HttpURLConnection
//            connection.requestMethod = "GET"  // Default to GET method, adjust as needed
//
//            // Set up the connection headers, forwarding any headers from the client request
//            forwardHeaders(connection, input)
//
//            // Read the response from the remote server
//            val responseCode = connection.responseCode
//            val responseMessage = connection.responseMessage
//
//            // Send the response status code and message back to the client
//            output.write("HTTP/1.1 $responseCode $responseMessage\r\n".toByteArray())
//
//            // Forward response headers to the client
//            connection.headerFields.forEach { (key, value) ->
//                if (key != null) {
//                    value.forEach {
//                        output.write("$key: $it\r\n".toByteArray())
//                    }
//                }
//            }
//
//            output.write("\r\n".toByteArray())  // End of headers
//
//            // Forward the response body
//            val inputStream: InputStream = connection.inputStream
//            inputStream.copyTo(output)
//
//        } catch (e: Exception) {
//            Timber.tag(TAG).e("Error forwarding request: %s", e.message)
//            sendBadRequestResponse(output)
//        }
//    }
//
//    // Forward headers from the client request to the destination server
//    private fun forwardHeaders(connection: HttpURLConnection, input: BufferedReader) {
//        var line: String?
//        while (input.readLine().also { line = it } != null && line!!.isNotEmpty()) {
//            val headerParts = line!!.split(":", limit = 2)
//            if (headerParts.size == 2) {
//                connection.setRequestProperty(headerParts[0].trim(), headerParts[1].trim())
//            }
//        }
//    }
//
//    // Send a bad request response to the client if there's an error
//    private fun sendBadRequestResponse(output: OutputStream) {
//        output.write("HTTP/1.1 400 Bad Request\r\n".toByteArray())
//        output.write("\r\n".toByteArray())
//    }
//}
