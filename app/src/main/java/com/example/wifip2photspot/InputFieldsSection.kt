package com.example.wifip2photspot

// InputFieldsSection.kt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.ContentAlpha

@Composable
fun InputFieldsSection(
    ssidInput: String,
    onSsidChange: (String) -> Unit,
    passwordInput: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibilityChange: (Boolean) -> Unit,
    isHotspotEnabled: Boolean
) {
    // Compute error states
    val ssidErrorState = ssidInput.isEmpty()
    val passwordErrorState = passwordInput.length !in 8..63

    Card(
        elevation = CardDefaults.cardElevation(4.dp),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // SSID Input Field
            OutlinedTextField(
                value = ssidInput,
                onValueChange = onSsidChange,
                label = { Text("SSID") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Wifi,
                        contentDescription = "SSID Icon"
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (!isHotspotEnabled) 1f else ContentAlpha.disabled),
                enabled = !isHotspotEnabled,
                singleLine = true,
                isError = ssidErrorState,
                supportingText = {
                    if (ssidErrorState) {
                        Text(
                            text = "SSID cannot be empty",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Password Input Field with Show/Hide Feature
            OutlinedTextField(
                value = passwordInput,
                onValueChange = onPasswordChange,
                label = { Text("Password") },
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

                    val description =
                        if (passwordVisible) "Hide password" else "Show password"

                    IconButton(onClick = {
                        onPasswordVisibilityChange(!passwordVisible)
                    }) {
                        Icon(
                            imageVector = image,
                            contentDescription = description
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (!isHotspotEnabled) 1f else ContentAlpha.disabled),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { /* Handle action if needed */ }
                ),
                enabled = !isHotspotEnabled,
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                isError = passwordErrorState,
                supportingText = {
                    if (passwordErrorState) {
                        Text(
                            text = "Password must be 8-63 characters",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    }
}
