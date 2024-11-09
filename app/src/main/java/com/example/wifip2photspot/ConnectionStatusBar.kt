// ConnectionStatusBar.kt
package com.example.wifip2photspot

import android.provider.CalendarContract.Colors
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

@Composable
fun ConnectionStatusBar(
    uploadSpeed: Int,
    downloadSpeed: Int,
    totalDownload: Int,
    connectedDevicesCount: Int,
    errorMessage: String? = null
) {
    // Main Card encapsulating all metrics
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Speed Metrics Sub-Card
                MetricSubCard(
                    title = stringResource(id = R.string.speed),
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.Start
                    ) {
                        // Animated Upload Speed
                        AnimatedMetric(
                            icon = Icons.Default.Upload,
                            tint = Color(0xFF2196F3), // Blue
                            value = uploadSpeed,
                            unit = stringResource(id = R.string.kbps)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Animated Download Speed
                        AnimatedMetric(
                            icon = Icons.Default.Download,
                            tint = Color(0xFF4CAF50), // Green
                            value = downloadSpeed,
                            unit = stringResource(id = R.string.kbps)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Total Download Metrics Sub-Card
                MetricSubCard(
                    title = stringResource(id = R.string.download),
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = Color(0xFFFF9800), // Orange
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        AnimatedText(
                            text = "$totalDownload",
                            unit = stringResource(id = R.string.kbps)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Connected Devices Metrics Sub-Card
                MetricSubCard(
                    title = stringResource(id = R.string.devices),
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeviceHub,
                            contentDescription = null,
                            tint = Color(0xFF9C27B0), // Purple
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        AnimatedText(
                            text = "$connectedDevicesCount",
                            unit = ""
                        )
                    }
                }
            }

            // Error Message Display
            errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AnimatedMetric(
    icon: ImageVector,
    tint: Color,
    value: Int,
    unit: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // Accessibility handled in parent composable
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        AnimatedText(
            text = "$value",
            unit = unit
        )
    }
}

@Composable
fun AnimatedText(
    text: String,
    unit: String
) {
    // Animate the numeric value
    val targetValue = text.toIntOrNull() ?: 0
    val animatedValue by animateIntAsState(
        targetValue = targetValue,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$animatedValue ",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
    }
}
@Composable
fun AnimatedButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    // Animation for scaling the button when pressed
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 100)
    )

    // Animation for color change
    val buttonColor by animateColorAsState(
        targetValue = if (enabled) MaterialTheme.colorScheme.primary else Color.Gray,
        animationSpec = tween(durationMillis = 300)
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
    ) {
        Text(text)
    }
}


@Composable
fun MetricSubCard(
    title: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
}

@Composable
fun DataUsageSection(
    rxBytes: Long,
    txBytes: Long
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Session Data Usage", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Download: ${formatBytes(rxBytes)}")
        Text("Upload: ${formatBytes(txBytes)}")
    }
}

fun formatBytes(bytes: Long): String {
    val kb = bytes / 1024
    val mb = kb / 1024
    return when {
        mb > 0 -> "$mb MB"
        kb > 0 -> "$kb KB"
        else -> "$bytes Bytes"
    }
}

//@Composable
//fun HistoricalDataUsageSection(historicalData: List<DataUsageRecord>) {
//    Column(modifier = Modifier.padding(16.dp)) {
//        Text("Historical Data Usage", style = MaterialTheme.typography.titleMedium)
//        Spacer(modifier = Modifier.height(8.dp))
//        historicalData.forEach { record ->
//            Text("${record.date}: Download ${formatBytes(record.rxBytes)}, Upload ${formatBytes(record.txBytes)}")
//        }
//    }
//}

@Composable
fun SpeedGraphSection(
    uploadSpeeds: List<Entry>,
    downloadSpeeds: List<Entry>
) {
    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                axisRight.isEnabled = false
            }
        },
        update = { chart ->
            val uploadDataSet = LineDataSet(uploadSpeeds, "Upload Speed").apply {
                color = Color.Red.toArgb()
            }
            val downloadDataSet = LineDataSet(downloadSpeeds, "Download Speed").apply {
                color = Color.Green.toArgb()
            }
            val data = LineData(uploadDataSet, downloadDataSet)
            chart.data = data
            chart.invalidate()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    )
}

