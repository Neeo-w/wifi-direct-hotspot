package com.example.wifip2photspot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MACAddressFilterSection(
    allowedMacAddresses: Set<String>,
    blockedMacAddresses: Set<String>,
    onAddAllowed: (String) -> Unit,
    onRemoveAllowed: (String) -> Unit,
    onAddBlocked: (String) -> Unit,
    onRemoveBlocked: (String) -> Unit
) {
    var macInput by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text("MAC Address Filtering", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = macInput,
            onValueChange = { macInput = it },
            label = { Text("MAC Address") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row {
            Button(onClick = { onAddAllowed(macInput); macInput = "" }) {
                Text("Add to Whitelist")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { onAddBlocked(macInput); macInput = "" }) {
                Text("Add to Blacklist")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Whitelisted MAC Addresses:")
        allowedMacAddresses.forEach { mac ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(mac)
                IconButton(onClick = { onRemoveAllowed(mac) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Blacklisted MAC Addresses:")
        blockedMacAddresses.forEach { mac ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(mac)
                IconButton(onClick = { onRemoveBlocked(mac) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                }
            }
        }
    }
}
