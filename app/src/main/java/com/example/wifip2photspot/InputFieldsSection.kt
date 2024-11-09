// InputFieldsSection.kt
package com.example.wifip2photspot

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.ContentAlpha

@Composable
fun InputFieldsSection(
    ssidInput: TextFieldValue,
    onSsidChange: (TextFieldValue) -> Unit,
    passwordInput: TextFieldValue,
    onPasswordChange: (TextFieldValue) -> Unit,
    isHotspotEnabled: Boolean,
    proxyPort: Int,
    onProxyPortChange: (Int) -> Unit,
    selectedBand: String,
    onBandSelected: (String) -> Unit,
    bands: List<String>
) {
    // State for password visibility
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    // Compute error states
    val ssidErrorState = ssidInput.text.isEmpty()
    val passwordErrorState = passwordInput.text.length !in 8..63
    if (!isHotspotEnabled) {
        Card(
            elevation = CardDefaults.cardElevation(4.dp),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .imePadding()
            ) {
                // SSID Input Field
                OutlinedTextField(
                    value = ssidInput,
                    onValueChange = onSsidChange,
                    label = { Text("SSID") },
                    placeholder = { Text("TetherGuard") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Wifi,
                            contentDescription = "SSID Icon"
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .alpha(if (!isHotspotEnabled) 1f else ContentAlpha.disabled),
                    enabled = !isHotspotEnabled,
                    singleLine = true,
                    isError = ssidErrorState,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Next
                    ),
                    supportingText = {
                        if (ssidErrorState) {
                            Text(
                                text = "SSID cannot be empty",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )

                // Password Input Field with Show/Hide Toggle
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = onPasswordChange,
                    label = { Text("Password") },
                    placeholder = { Text("00000000") },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = "Password Icon"
                        )
                    },
                    trailingIcon = {
                        val image = if (passwordVisible)
                            Icons.Filled.Visibility
                        else
                            Icons.Filled.VisibilityOff

                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = image,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .alpha(if (!isHotspotEnabled) 1f else ContentAlpha.disabled),
                    enabled = !isHotspotEnabled,
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = passwordErrorState,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    supportingText = {
                        if (passwordErrorState) {
                            Text(
                                text = "Password must be 8-63 characters",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )

                // Proxy Port Input Field

                // Proxy Port Field
                if (!isHotspotEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Proxy Port:", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = proxyPort.toString(),
                            onValueChange = { newPort ->
                                newPort.toIntOrNull()?.let {
                                    onProxyPortChange(it)
                                }
                            },
                            label = { Text("Port") },
                            singleLine = true,
                            modifier = Modifier.width(100.dp)
                        )
                    }
                }
            }

            // Band Selection Section
            BandSelection(
                selectedBand = selectedBand,
                onBandSelected = onBandSelected,
                bands = bands,
                isHotspotEnabled = isHotspotEnabled
            )
        }
    }
}

@Composable
fun BandSelection(
    selectedBand: String,
    onBandSelected: (String) -> Unit,
    bands: List<String>,
    isHotspotEnabled: Boolean
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Select Band",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            bands.forEach { band ->
                OutlinedButton(
                    onClick = { onBandSelected(band) },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selectedBand == band)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else
                            Color.Transparent
                    ),
                    enabled = !isHotspotEnabled,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = band,
                        color = if (selectedBand == band)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}


