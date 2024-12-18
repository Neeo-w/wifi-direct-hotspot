package com.example.wifip2photspot.proxy

import android.util.Base64
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.*
import java.net.HttpURLConnection
import java.net.Socket
import java.net.SocketException
import java.net.URL

class ProxyHandler(
    private val clientSocket: Socket,
    private val expectedUsername: String,
    private val expectedPassword: String
) {

    suspend fun process() {
        withContext(Dispatchers.IO) {
            try {
                val clientReader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
                val clientWriter = BufferedWriter(OutputStreamWriter(clientSocket.getOutputStream()))
                val requestLine = clientReader.readLine()

                if (requestLine.isNullOrEmpty()) {
                    clientSocket.close()
                    return@withContext
                }

                Timber.d("Request: $requestLine")

                val tokens = requestLine.split(" ")
                if (tokens.size < 3) {
                    sendBadRequest(clientWriter)
                    return@withContext
                }

                val method = tokens[0]
                val url = tokens[1]
                val protocol = tokens[2]

                if (method.equals("CONNECT", ignoreCase = true)) {
                    handleHttps(clientReader, clientWriter, url, protocol)
                } else {
                    handleHttp(clientReader, clientWriter, method, url, protocol)
                }

            } catch (e: Exception) {
                Timber.e("Error processing client: ${e.message}")
            } finally {
                clientSocket.close()
            }
        }
    }

    private suspend fun handleHttp(
        clientReader: BufferedReader,
        clientWriter: BufferedWriter,
        method: String,
        urlString: String,
        protocol: String
    ) {
        try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = method

            // Forward headers
            var headerLine: String?
            val headers = mutableMapOf<String, String>()
            while (clientReader.readLine().also { headerLine = it } != null && headerLine!!.isNotEmpty()) {
                val headerParts = headerLine!!.split(":", limit = 2)
                if (headerParts.size == 2) {
                    val key = headerParts[0].trim().lowercase()
                    val value = headerParts[1].trim()
                    headers[key] = value
                    connection.setRequestProperty(headerParts[0].trim(), headerParts[1].trim())
                }
            }

            // Check for Authentication
            val authHeader = headers["proxy-authorization"]
            if (authHeader != null && validateCredentials(authHeader)) {
                // Proceed with request processing
            } else {
                // Send 407 Proxy Authentication Required
                clientWriter.write("$protocol 407 Proxy Authentication Required\r\n")
                clientWriter.write("Proxy-Authenticate: Basic realm=\"WiFiP2PHotspot\"\r\n")
                clientWriter.write("\r\n")
                clientWriter.flush()
                return
            }

            // Connect to target server
            connection.connect()

            // Send response headers to client
            clientWriter.write("$protocol ${connection.responseCode} ${connection.responseMessage}\r\n")
            connection.headerFields.forEach { (key, value) ->
                if (key != null) {
                    clientWriter.write("$key: ${value.joinToString(", ")}\r\n")
                }
            }
            clientWriter.write("\r\n")
            clientWriter.flush()

            // Forward response body
            val inputStream: InputStream = if (connection.responseCode < HttpURLConnection.HTTP_BAD_REQUEST) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            inputStream.copyTo(clientSocket.getOutputStream())
            inputStream.close()

        } catch (e: Exception) {
            Timber.e("HTTP Handler Error: ${e.message}")
            sendBadGateway(clientWriter)
        }
    }

    private suspend fun handleHttps(
        clientReader: BufferedReader,
        clientWriter: BufferedWriter,
        url: String,
        protocol: String
    ) {
        try {
            val hostPort = url.split(":")
            val host = hostPort[0]
            val port = if (hostPort.size > 1) hostPort[1].toInt() else 443

            clientWriter.write("$protocol 200 Connection Established\r\n")
            clientWriter.write("Proxy-agent: WiFiP2PHotspot/1.0\r\n")
            clientWriter.write("\r\n")
            clientWriter.flush()

            val targetSocket = Socket(host, port)

            val clientToTarget = CoroutineScope(Dispatchers.IO).async {
                try {
                    clientSocket.getInputStream().copyTo(targetSocket.getOutputStream())
                } catch (e: SocketException) {
                    // It's normal if one side closes the connection
                    Timber.e("Client->Target copy stopped: ${e.message}")
                }
            }

            val targetToClient = CoroutineScope(Dispatchers.IO).async {
                try {
                    targetSocket.getInputStream().copyTo(clientSocket.getOutputStream())
                } catch (e: SocketException) {
                    // It's normal if one side closes the connection
                    Timber.e("Target->Client copy stopped: ${e.message}")
                }
            }

            // Await both tasks to finish copying.
            try {
                awaitAll(clientToTarget, targetToClient)
            } catch (e: Exception) {
                Timber.e("Error awaiting tunneling tasks: ${e.message}")
            } finally {
                // Close sockets after tasks finish
                try {
                    targetSocket.close()
                } catch (ignore: IOException) { }
                try {
                    clientSocket.close()
                } catch (ignore: IOException) { }
            }

        } catch (e: Exception) {
            Timber.e("HTTPS Handler Error: ${e.message}")
            sendBadGateway(clientWriter)
        }
    }


    private fun validateCredentials(authHeader: String): Boolean {
        val prefix = "Basic "
        if (!authHeader.startsWith(prefix, ignoreCase = true)) return false
        val base64Credentials = authHeader.removePrefix(prefix)
        val credentials = String(Base64.decode(base64Credentials, Base64.DEFAULT))
        val parts = credentials.split(":", limit = 2)
        if (parts.size != 2) return false
        val (username, password) = parts
        return username == expectedUsername && password == expectedPassword
    }

    private suspend fun sendBadRequest(clientWriter: BufferedWriter) {
        clientWriter.write("HTTP/1.1 400 Bad Request\r\n")
        clientWriter.write("Proxy-agent: WiFiP2PHotspot/1.0\r\n")
        clientWriter.write("\r\n")
        clientWriter.flush()
    }

    private suspend fun sendBadGateway(clientWriter: BufferedWriter) {
        clientWriter.write("HTTP/1.1 502 Bad Gateway\r\n")
        clientWriter.write("Proxy-agent: WiFiP2PHotspot/1.0\r\n")
        clientWriter.write("\r\n")
        clientWriter.flush()
    }
}
