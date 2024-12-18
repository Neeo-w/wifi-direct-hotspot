package com.example.wifip2photspot.proxy

// app/src/main/java/com/example/wifip2photspot/proxy/ClientSocketHandler.kt


import android.annotation.SuppressLint
import android.util.Log
import com.example.wifip2photspot.proxy.ServiceStatusRepository
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.*
import java.net.Socket
import java.util.regex.Pattern

class ClientSocketHandler(private val clientSocket: Socket) : Thread() {

    companion object {
        val CONNECT_PATTERN: Pattern = Pattern.compile(
            "CONNECT (.+):(.+) HTTP/(1\\.[01])",
            Pattern.CASE_INSENSITIVE
        )
        const val BUFFER_SIZE = 32768
    }

    private var previousWasR = false
    private val LOG_TAG = "ClientSocketHandler"

    override fun run() {
        try {
            val request = readLine(clientSocket)
            Timber.tag(LOG_TAG).d(request)
            if (request.startsWith("CONNECT")) {
                httpsHandler(request)
            } else {
                httpHandler(request)
            }
        } catch (e: IOException) {
            Timber.tag(LOG_TAG).e("Error handling client: %s", e.message)
            e.printStackTrace()
        } finally {
            try {
                clientSocket.close()
            } catch (e: IOException) {
                Timber.tag(LOG_TAG).e("Error closing client socket: %s", e.message)
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("TimberArgCount")
    private fun httpHandler(request: String) {
        try {
            val clientInputStream = clientSocket.getInputStream()
            val clientOutputStream = clientSocket.getOutputStream()
            Timber.tag(LOG_TAG).d(request)
            val url = try {
                request.split(" ")[1]
            } catch (e: IndexOutOfBoundsException) {
                Timber.tag(LOG_TAG).e("Invalid request format")
                e.printStackTrace()
                clientSocket.close()
                return
            }
            Timber.tag(LOG_TAG).d(url)
            if (!url.matches(Regex("(http://)?.+\\.\\w+(/.+)*/?"))) {
                clientInputStream.close()
                clientOutputStream.close()
                clientSocket.close()
                return
            }
            val outputStreamWriter = OutputStreamWriter(
                clientSocket.getOutputStream(),
                "ISO-8859-1"
            )

            val host = if (url.startsWith("http://")) {
                url.split("/")[2]
            } else {
                url.split("/")[0]
            }

            val forwardSocket = try {
                Socket(host, 80)
            } catch (e: IOException) {
                Timber.tag(LOG_TAG).e("%s:80", "Cannot connect to %s", host)
                e.printStackTrace()
                outputStreamWriter.write("HTTP/1.1 502 Bad Gateway\r\n")
                outputStreamWriter.write("Proxy-agent: ProxyForward/1.0\r\n")
                outputStreamWriter.write("\r\n")
                outputStreamWriter.flush()
                return
            }

            forwardSocket.use { forwardSocket ->
                // Start forwarding data from forwardSocket to clientSocket
                val remoteToClient = CoroutineScope(Dispatchers.IO).launch {
                    forwardData(forwardSocket, clientSocket)
                }

                // Modify the GET request to use relative path
                val relativePath = url.replaceFirst(Regex("http://.+?(/.+)"), "/$1")
                val modifiedRequest = "GET $relativePath HTTP/1.1\r\n"
                forwardSocket.getOutputStream().write(modifiedRequest.toByteArray())

                // Start forwarding data from clientSocket to forwardSocket
                if (previousWasR) {
                    val read = clientSocket.getInputStream().read()
                    if (read != -1) {
                        if (read != '\n'.toInt()) {
                            forwardSocket.getOutputStream().write(read)
                        }
                        forwardData(clientSocket, forwardSocket)
                    } else {
                        if (!forwardSocket.isOutputShutdown) {
                            forwardSocket.shutdownOutput()
                        }
                        if (!clientSocket.isInputShutdown) {
                            clientSocket.shutdownInput()
                        }
                    }
                } else {
                    forwardData(clientSocket, forwardSocket)
                }

                runBlocking {
                    remoteToClient.join()
                }
            }
        } catch (e: IOException) {
            Timber.tag(LOG_TAG).e("HTTP Handler Error: %s", e.message)
            e.printStackTrace()
        }
    }

    @SuppressLint("TimberArgCount")
    private fun httpsHandler(request: String) {
        val matcher = CONNECT_PATTERN.matcher(request)
        if (matcher.matches()) {
            try {
                // Read and ignore headers
                var header: String
                do {
                    header = readLine(clientSocket)
                } while (header.isNotEmpty())

                val outputStreamWriter = OutputStreamWriter(
                    clientSocket.getOutputStream(),
                    "ISO-8859-1"
                )

                val forwardSocket = try {
                    matcher.group(2)?.let { Socket(matcher.group(1), it.toInt()) }
                } catch (e: IOException) {
                    Timber.tag(LOG_TAG)
                        .e("%s%s", "%s:", "Cannot connect to %s", matcher.group(1), matcher.group(2))
                    e.printStackTrace()
                    outputStreamWriter.write("HTTP/${matcher.group(3)} 502 Bad Gateway\r\n")
                    outputStreamWriter.write("Proxy-agent: ProxyForward/1.0\r\n")
                    outputStreamWriter.write("\r\n")
                    outputStreamWriter.flush()
                    return
                }

                forwardSocket.use { forwardSocket ->
                    outputStreamWriter.write("HTTP/${matcher.group(3)} 200 Connection established\r\n")
                    outputStreamWriter.write("Proxy-agent: ProxyForward/1.0\r\n")
                    outputStreamWriter.write("\r\n")
                    outputStreamWriter.flush()

                    // Start forwarding data from forwardSocket to clientSocket
                    val remoteToClient = CoroutineScope(Dispatchers.IO).launch {
                        if (forwardSocket != null) {
                            forwardData(forwardSocket, clientSocket)
                        }
                    }

                    // Start forwarding data from clientSocket to forwardSocket
                    if (previousWasR) {
                        val read = clientSocket.getInputStream().read()
                        if (read != -1) {
                            if (read != '\n'.toInt()) {
                                forwardSocket?.getOutputStream()?.write(read)
                            }
                            if (forwardSocket != null) {
                                forwardData(clientSocket, forwardSocket)
                            }
                        } else {
                            if (forwardSocket != null) {
                                if (!forwardSocket.isOutputShutdown) {
                                    forwardSocket.shutdownOutput()
                                }
                            }
                            if (!clientSocket.isInputShutdown) {
                                clientSocket.shutdownInput()
                            }
                        }
                    } else {
                        if (forwardSocket != null) {
                            forwardData(clientSocket, forwardSocket)
                        }
                    }

                    runBlocking {
                        remoteToClient.join()
                    }
                }
            } catch (e: IOException) {
                Timber.tag(LOG_TAG).e("HTTPS Handler Error: %s", e.message)
                e.printStackTrace()
            }
        }
    }

    private fun forwardData(inputSocket: Socket, outputSocket: Socket) {
        try {
            val inputStream = inputSocket.getInputStream()
            val outputStream = outputSocket.getOutputStream()
            val buffer = ByteArray(4096)
            var read: Int
            while (inputStream.read(buffer).also { read = it } >= 0) {
                if (read > 0) {
                    outputStream.write(buffer, 0, read)
                    if (inputStream.available() < 1) {
                        outputStream.flush()
                    }
                }
            }
            outputSocket.shutdownOutput()
            inputSocket.shutdownInput()
        } catch (e: IOException) {
            Timber.tag(LOG_TAG).e("Error forwarding data: %s", e.message)
            e.printStackTrace()
        }
    }

    private fun readLine(socket: Socket): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        var next: Int
        loop@ while (socket.getInputStream().read().also { next = it } != -1) {
            if (previousWasR && next == '\n'.code) {
                previousWasR = false
                continue@loop
            }
            when (next) {
                '\r'.code -> {
                    previousWasR = true
                    break@loop
                }
                '\n'.code -> break@loop
                else -> byteArrayOutputStream.write(next)
            }
        }
        return byteArrayOutputStream.toString("ISO-8859-1")
    }
}
