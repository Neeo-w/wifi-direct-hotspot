package com.example.wifip2photspot.socksProxy


// SSHServerManager.kt

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.sshd.common.keyprovider.KeyPairProvider
import org.apache.sshd.server.SshServer
import org.apache.sshd.server.auth.password.PasswordAuthenticator
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider
import org.apache.sshd.server.session.ServerSession
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.file.Paths

class SSHServerManager(
    private val context: Context,
    private val sshUsername: String,
    private val sshPassword: String,
    private val proxyPort: Int = 8181 // SOCKS Proxy Port
) {

    private var sshServer: SshServer? = null
    private val TAG = "SSHServerManager"

    suspend fun startSSHServer(localPort: Int = 2222) {
        withContext(Dispatchers.IO) {
            // Initialize the SSH server after 'user.home' is set
            sshServer = SshServer.setUpDefaultServer()
            sshServer?.port = localPort

            // Host Key Provider expects a Path, so we convert the string to a Path
            val hostKeyPath = Paths.get(context.filesDir.absolutePath, "hostkey.ser")
            val hostKeyProvider: KeyPairProvider = SimpleGeneratorHostKeyProvider(hostKeyPath)
            sshServer?.keyPairProvider = hostKeyProvider

            // Set up Password Authentication
            sshServer?.passwordAuthenticator =
                PasswordAuthenticator { username, password, session ->
                    val clientAddress = session.clientAddress
                    val ipAddress = if (clientAddress is InetSocketAddress) {
                        clientAddress.address.hostAddress
                    } else {
                        null
                    }
                    // Allow only connections from the local Wi-Fi Direct IP range (e.g., 192.168.49.x)
                    val isAllowedIP = ipAddress?.startsWith("192.168.49.") ?: false
                    val isAuthenticated = username == sshUsername && password == sshPassword
                    Log.d(TAG, "Authentication attempt from $ipAddress: ${if (isAuthenticated && isAllowedIP) "Success" else "Failure"}")
                    isAuthenticated && isAllowedIP
                }

            try {
                sshServer?.start()
                Log.d(TAG, "SSH Server started on port $localPort")
            } catch (e: IOException) {
                Log.e(TAG, "Failed to start SSH Server: ${e.message}")
                e.printStackTrace()
            }
            
        }
    }


    suspend fun stopSSHServer() {
        withContext(Dispatchers.IO) {
            try {
                sshServer?.stop()
                sshServer = null
                Log.d(TAG, "SSH Server stopped.")
            } catch (e: IOException) {
                Log.e(TAG, "Failed to stop SSH Server: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun isRunning(): Boolean {
        return sshServer?.isOpen ?: false
    }
}


