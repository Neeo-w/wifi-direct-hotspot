package com.example.wifip2photspot



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

//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ImprovedHeader(isHotspotEnabled: Boolean) {
//    var showMenu by remember { mutableStateOf(false) }
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImprovedHeader(isHotspotEnabled: Boolean) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text("Asol") },
        navigationIcon = {
            IconButton(onClick = { /* Open navigation drawer */ }) {
                Icon(Icons.Filled.Menu, contentDescription = "Menu")
            }
        },
        actions = {
            IconButton(onClick = { /* Open settings */ }) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
            }
            IconButton(onClick = { showMenu = !showMenu }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More options")
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(text = { Text("Help") }, onClick = { /* Navigate to help */ })
                DropdownMenuItem(text = { Text("About") }, onClick = { /* Navigate to about */ })
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        ),
//        elevation = 4.dp
    )
}
