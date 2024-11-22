package com.example.wifip2photspot

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
fun FeedbackForm(onSubmit: (String) -> Unit) {
    var feedbackText by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Feedback", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = feedbackText,
            onValueChange = { feedbackText = it },
            label = { Text("Your Feedback") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                onSubmit(feedbackText)
                feedbackText = ""
            },
            enabled = feedbackText.isNotBlank()
        ) {
            Text("Submit")
        }
    }
}

@Composable
fun ContactSupportSection(onContactSupport: () -> Unit) {
    Column(modifier = Modifier.padding(8.dp)) {
        Text("Need Help?", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = onContactSupport) {
            Text("Contact Support")
        }
    }
}



