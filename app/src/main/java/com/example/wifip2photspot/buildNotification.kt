//import android.app.Notification
//import android.app.NotificationChannel
//import android.app.NotificationManager
//import android.content.Context
//import android.os.Build
//import androidx.core.app.NotificationCompat
//import androidx.core.app.NotificationManagerCompat
//import com.example.wifip2photspot.R
//
//class NotificationHelper(private val context: Context) {
//
//    private val channelId = "hotspot_service_channel"
//    private val notificationId = 101
//
//    init {
//        // Create a notification channel for Android 8.0 and above
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channel = NotificationChannel(
//                channelId,
//                "Hotspot Service Notifications",
//                NotificationManager.IMPORTANCE_LOW
//            ).apply {
//                description = "Notification channel for Wi-Fi Direct hotspot service and VPN."
//            }
//            val notificationManager =
//                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            notificationManager.createNotificationChannel(channel)
//        }
//    }
//
//    // Build and return a notification
//    fun buildNotification(isRunning: Boolean): Notification {
//        val actionText = if (isRunning) "Stop" else "Start"
//        val actionIntent = if (isRunning) {
//            // Intent to stop the hotspot (you can add logic here for stopping the service)
//            // For example, you might call a method to stop the VPN or Wi-Fi Direct service
//        } else {
//            // Intent to start the hotspot (you can add logic here for starting the service)
//            // For example, you might call a method to start the VPN or Wi-Fi Direct service
//        }
//
//        return NotificationCompat.Builder(context, channelId)
//            .setSmallIcon(R.drawable.ic_hotspot) // Replace with your app's icon
//            .setContentTitle("Wi-Fi Direct Hotspot Service")
//            .setContentText("The hotspot is currently ${if (isRunning) "active" else "inactive"}.")
//            .setPriority(NotificationCompat.PRIORITY_LOW)
//            .setOngoing(isRunning) // Make the notification ongoing (i.e., it can't be dismissed by the user)
//            .addAction(R.drawable.ic_play_arrow, actionText, actionIntent) // Add an action to start/stop service
//            .setAutoCancel(false) // Keep the notification until the service is stopped
//            .build()
//    }
//
//    // Show the notification
//    fun showNotification(isRunning: Boolean) {
//        val notification = buildNotification(isRunning)
//        NotificationManagerCompat.from(context).notify(notificationId, notification)
//    }
//
//    // Cancel the notification
//    fun cancelNotification() {
//        NotificationManagerCompat.from(context).cancel(notificationId)
//    }
//}
