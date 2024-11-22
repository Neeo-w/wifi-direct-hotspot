package com.example.wifip2photspot.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import com.example.wifip2photspot.viewModel.HotspotViewModel

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun SettingsScreen(
    navController: NavHostController,
    hotspotViewModel: HotspotViewModel
){
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = { paddingValues ->
            SettingsContent(
                hotspotViewModel = hotspotViewModel,
                paddingValues = paddingValues
            )
        }
    )
}


