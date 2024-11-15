package com.example.wifip2photspot


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.HelpOutline

//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ImprovedHeader(isHotspotEnabled: Boolean) {
//    var showMenu by remember { mutableStateOf(false) }
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImprovedHeader(
    isHotspotEnabled: Boolean,
    viewModel: HotspotViewModel,
    onSettingsClick: () -> Unit,
    onHelpClick: () -> Unit

) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text("Asol") },
        navigationIcon = {
            IconButton(onClick = { /* Open navigation drawer if needed */ }) {
                Icon(Icons.Filled.Menu, contentDescription = "Menu")
            }
        },
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
            }
            IconButton(onClick = { showMenu = !showMenu }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More options")
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = {
                        Row {
                            Icon(Icons.Default.HelpOutline, contentDescription = "Help")
                            Spacer(Modifier.width(8.dp))
                            Text("Help")
                        }
                    },
                    onClick = {
                        onHelpClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("About") },
                    onClick = { /* Navigate to about */ }
                )
                // Other menu items as needed
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}