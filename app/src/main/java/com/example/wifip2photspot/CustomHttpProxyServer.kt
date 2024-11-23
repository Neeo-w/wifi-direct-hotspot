package com.example.wifip2photspot

import android.util.Log
import timber.log.Timber
import java.io.*
import java.net.ServerSocket
import java.net.Socket

import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.Executors

import kotlin.concurrent.thread

class HttpProxyServer(private val port: Int, private val bindIp: String) {

    private val clientHandlingExecutor = Executors.newFixedThreadPool(50) // Adjust pool size as needed
    private val dataForwardingExecutor = Executors.newCachedThreadPool()

    private var serverSocket: ServerSocket? = null
    private var isRunning = false

    fun start() {
        serverSocket = ServerSocket(port)
        isRunning = true

        thread {
            while (isRunning) {
                try {
                    val clientSocket = serverSocket!!.accept()
                    clientHandlingExecutor.execute { handleClient(clientSocket) }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun stop() {
        isRunning = false
        serverSocket?.close()
    }

    private fun handleClient(clientSocket: Socket) {
        val targetSocket: Socket? = null

        try {
            val clientInput = clientSocket.getInputStream()
            val clientOutput = clientSocket.getOutputStream()

            // Read the client's request
            val requestBuffer = ByteArray(8192)
            val bytesRead = clientInput.read(requestBuffer)
            if (bytesRead == -1) {
                clientSocket.close()
                return
            }

            val requestHeader = String(requestBuffer, 0, bytesRead)
            val requestLines = requestHeader.split("\r\n")
            if (requestLines.isEmpty()) {
                clientSocket.close()
                return
            }

            val requestLine = requestLines[0]
            val tokens = requestLine.split(" ")
            if (tokens.size < 3) {
                clientSocket.close()
                return
            }

            val method = tokens[0]
            val uri = tokens[1]

            if (method.equals("CONNECT", ignoreCase = true)) {
                // Handle HTTPS tunneling
                val hostPort = uri.split(":")
                val host = hostPort[0]
                val port = hostPort.getOrNull(1)?.toIntOrNull() ?: 443

                val targetSocket = Socket(host, port)
                clientOutput.write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray())
                clientOutput.flush()

                // Forward data between client and target server
                // Start data forwarding threads
                val clientToServer = thread {
                    forwardData(clientInput, targetSocket!!.getOutputStream())
                }
                val serverToClient = thread {
                    forwardData(targetSocket!!.getInputStream(), clientOutput)
                }

                // Wait for both threads to finish
                clientToServer.join()
                serverToClient.join()
                targetSocket.close()
            } else {
                // Handle HTTP request
                // Modify headers as needed
                val modifiedRequestHeader = modifyHeaders(requestHeader)

                // Extract host and port from headers
                val hostLine = requestLines.find { it.startsWith("Host:", ignoreCase = true) }
                if (hostLine == null) {
                    clientSocket.close()
                    return
                }
                val hostPort = hostLine.substringAfter(" ").split(":")
                val host = hostPort[0]
                val port = hostPort.getOrNull(1)?.toIntOrNull() ?: 80

                val targetSocket = Socket(host, port)
                val targetOutput = targetSocket.getOutputStream()
                targetOutput.write(modifiedRequestHeader.toByteArray())
                targetOutput.flush()

                // Forward data between client and target server
                val clientToServer = thread {
                    forwardData(clientInput, targetOutput)
                }
                val serverToClient = thread {
                    forwardData(targetSocket.getInputStream(), clientOutput)
                }

                clientToServer.join()
                serverToClient.join()

                targetSocket.close()
            }

            clientSocket.close()
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                clientSocket.close()
            } catch (_: Exception) {
            } finally {
                try {
                    targetSocket?.close()
                } catch (_: Exception) {}
                try {
                    clientSocket.close()
                } catch (_: Exception) {}
            }
        }
    }

    private fun modifyHeaders(requestHeader: String): String {
        val lines = requestHeader.lines().toMutableList()
        for (i in lines.indices) {
            if (lines[i].startsWith("User-Agent:", ignoreCase = true)) {
                lines[i] = "User-Agent: Mozilla/5.0 (Android)"
            }
            if (lines[i].startsWith("Referer:", ignoreCase = true)) {
                lines[i] = "Referer: https://example.com"
            }
        }
        return lines.joinToString("\r\n") + "\r\n\r\n"
    }

    private fun forwardData(inputStream: InputStream, outputStream: OutputStream) {
        try {
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                outputStream.flush()
            }
        } catch (e: Exception) {
            Timber.tag("HttpProxyServer").e(e, "Error forwarding data: %s", e.message)
        } finally {
            try {
                outputStream.close()
            } catch (_: Exception) {
            }
        }
    }
}
