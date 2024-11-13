package com.example.wifip2photspot.socksProxy


import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SSHTunnelManager(
    private val username: String,
    private val password: String,
    private val host: String,
    private val port: Int = 22,
    private val remotePort: Int = 80, // HTTP
    private val localPort: Int = 1080 // SOCKS Proxy Port
) {
    private var session: Session? = null

    suspend fun connect() {
        withContext(Dispatchers.IO) {
            val jsch = JSch()
            session = jsch.getSession(username, host, port)
            session?.setPassword(password)

            // Avoid asking for key confirmation
            session?.setConfig("StrictHostKeyChecking", "no")

            session?.connect()

            // Set up dynamic port forwarding (SOCKS)
            session?.setPortForwardingL(localPort, "localhost", remotePort)
        }
    }

    suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            session?.disconnect()
            session = null
        }
    }

    fun isConnected(): Boolean {
        return session?.isConnected ?: false
    }
}
