package com.example.wifip2photspot.proxy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.wifip2photspot.MainActivity
import com.example.wifip2photspot.R
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket

class ProxyService : Service() {

    companion object {
        const val CHANNEL_ID = "ProxyServiceChannel"
        const val NOTIFICATION_ID = 1
    }

    private var proxyPort: Int = 8888
    private var proxyUsername: String = "TetherGuard"
    private var proxyPassword: String = "00000000"
    private var serverSocket: ServerSocket? = null
    private var serviceJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Timber.d("ProxyService Created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        proxyPort = intent?.getIntExtra("proxy_port", 8888) ?: 8888
        proxyUsername = intent?.getStringExtra("proxy_username") ?: "TetherGuard"
        proxyPassword = intent?.getStringExtra("proxy_password") ?: "00000000"

        startForegroundServiceWithNotification()
        startProxy()
        return START_STICKY
    }

    override fun onDestroy() {
        serviceJob?.cancel()
        serverSocket?.close()
        Timber.d("ProxyService Destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundServiceWithNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Proxy Service",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.lightColor = Color.BLUE
            channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("WiFiP2PHotspot Proxy")
            .setContentText("Proxy Server running on port $proxyPort")
            .setSmallIcon(R.drawable.ic_proxy)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun startProxy() {
        serviceJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                serverSocket = ServerSocket(proxyPort)
                Timber.d("ProxyServer listening on port $proxyPort")

                while (isActive) {
                    val clientSocket = serverSocket?.accept()
                    if (clientSocket != null) {
                        Timber.d("Client connected: ${clientSocket.inetAddress.hostAddress}")
                        launch { handleClient(clientSocket) }
                    }
                }
            } catch (e: IOException) {
                Timber.e("ProxyService Error: ${e.message}")
                stopSelf()
            }
        }
    }

    private suspend fun handleClient(clientSocket: Socket) {
        withContext(Dispatchers.IO) {
            try {
                val proxyHandler = ProxyHandler(clientSocket, proxyUsername, proxyPassword)
                proxyHandler.process()
            } catch (e: Exception) {
                Timber.e("Error handling client: ${e.message}")
            }
        }
    }
}
