// ConnectionStatusBar.kt
package com.example.wifip2photspot

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing), label = ""
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
fun BatteryStatusSection(batteryLevel: Int) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Battery Status", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Battery Level: $batteryLevel%")
    }
}

@Composable
fun SwitchPreference(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    var checkedState by rememberSaveable { mutableStateOf(true) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = checked,
            onCheckedChange = {
                checkedState = it
                onCheckedChange(it)
            },
            enabled = enabled,
            thumbContent = {
                if (checked) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),

                        )}
            }
        )
    }
}

//
//
//@Composable
//fun ProxyInfo(
//    proxyIp: String,
//    proxyPort: Int,
//    isProxyRunning: Boolean
//) {
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(vertical = 8.dp),
//        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
//    ) {
//        Column(modifier = Modifier.padding(16.dp)) {
//            Text(
//                text = "Proxy Server",
//                style = MaterialTheme.typography.titleMedium
//            )
//            Text(
//                text = if (isProxyRunning) "Status: Running" else "Status: Stopped",
//                style = MaterialTheme.typography.bodyMedium,
//                color = if (isProxyRunning) androidx.compose.ui.graphics.Color.Green else androidx.compose.ui.graphics.Color.Red
//            )
//            if (isProxyRunning) {
//                Text(
//                    text = "URL: $proxyIp:$proxyPort",
//                    style = MaterialTheme.typography.bodySmall
//                )
//            }
//        }
//    }
//}
//
//
//
