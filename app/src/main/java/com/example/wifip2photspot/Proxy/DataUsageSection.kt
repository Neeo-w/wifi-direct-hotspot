package com.example.wifip2photspot.Proxy

// DataUsageSection.kt
import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DataUsageSection(
    upload: Int,
    download: Int
) {
    Card(
        elevation = CardDefaults.cardElevation(4.dp),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Data Usage", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Upload: ${formatBytes(upload)}", style = MaterialTheme.typography.bodyMedium)
            Text("Download: ${formatBytes(download)}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// Utility function to format bytes
@SuppressLint("DefaultLocale")
fun formatBytes(bytes: Int): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
    val pre = "KMGTPE"[exp - 1].toString() + "i"
    return String.format("%.1f %sB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
}
