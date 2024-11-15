package com.example.wifip2photspot.ui


import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import com.example.wifip2photspot.HotspotViewModel
import com.example.wifip2photspot.ui.theme.ThemeToggle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController, viewModel: HotspotViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = { paddingValues ->
            SettingsContent(viewModel = viewModel, paddingValues = paddingValues)
        }
    )
}

// In your settings screen
//LanguageSelection(
//selectedLanguage = selectedLanguage,
//onLanguageChange = { newLanguage ->
//    viewModel.updateLanguage(newLanguage)
//    // Update app locale
//    val locale = Locale(newLanguage)
//    Locale.setDefault(locale)
//    val config = Configuration()
//    config.locale = locale
//    context.resources.updateConfiguration(config, context.resources.displayMetrics)
//}
//)