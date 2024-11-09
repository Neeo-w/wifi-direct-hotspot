package com.example.wifip2photspot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding

//@Composable
//fun BandSelection(
//    selectedBand: String,
//    onBandSelected: (String) -> Unit,
//    bands: List<String>,
//    isHotspotEnabled: Boolean
//) {
//    Card(
//        elevation = CardDefaults.cardElevation(4.dp),
//        shape = MaterialTheme.shapes.medium,
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(vertical = 8.dp)
//    ) {
//        Column(modifier = Modifier.padding(16.dp)) {
//            Text(
//                text = "Select Band",
//                style = MaterialTheme.typography.titleMedium,
//                modifier = Modifier.padding(bottom = 8.dp)
//            )
//            Row(
//                horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()
//            ) {
//                bands.forEach { band ->
//                    OutlinedButton(
//                        onClick = { onBandSelected(band) },
//                        colors = ButtonDefaults.outlinedButtonColors(
//                            containerColor = if (selectedBand == band) MaterialTheme.colorScheme.primary.copy(
//                                alpha = 0.1f
//                            ) else Color.Transparent
//                        ),
//                        enabled = !isHotspotEnabled,
//                        modifier = Modifier
//                            .weight(1f)
//                            .padding(horizontal = 4.dp)
//                    ) {
//                        Text(
//                            text = band,
//                            color = if (selectedBand == band) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
//                        )
//                    }
//                }
//            }
//        }
//    }
//}


