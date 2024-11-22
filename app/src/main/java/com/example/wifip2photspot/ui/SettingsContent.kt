package com.example.wifip2photspot.ui


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.wifip2photspot.BandSelection
import com.example.wifip2photspot.ContactSupportSection
import com.example.wifip2photspot.FeedbackForm
import com.example.wifip2photspot.viewModel.HotspotViewModel
import com.example.wifip2photspot.ui.theme.ThemeToggle

@Composable
fun SettingsContent(
    hotspotViewModel: HotspotViewModel,
    paddingValues: PaddingValues
) {
    // Collect necessary state from ViewModel
    val isDarkTheme by hotspotViewModel.isDarkTheme.collectAsState()
    val autoShutdownEnabled by hotspotViewModel.autoShutdownEnabled.collectAsState()
    val idleTimeoutMinutes by hotspotViewModel.idleTimeoutMinutes.collectAsState()
    val wifiLockEnabled by hotspotViewModel.wifiLockEnabled.collectAsState()
    val selectedBand by hotspotViewModel.selectedBand.collectAsState()
    val isHotspotEnabled by hotspotViewModel.isHotspotEnabled.collectAsState()


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Dark Theme Toggle
        ThemeToggle(
            isDarkTheme = isDarkTheme,
            onToggle = { isDark ->
                hotspotViewModel.updateTheme(isDark)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Spacer(modifier = Modifier.height(16.dp))

        // Idle Settings
        IdleSettingsSection(
            autoShutdownEnabled = autoShutdownEnabled,
            onAutoShutdownEnabledChange = { hotspotViewModel.updateAutoShutdownEnabled(it) },
            idleTimeoutMinutes = idleTimeoutMinutes,
            onIdleTimeoutChange = { hotspotViewModel.updateIdleTimeoutMinutes(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Wi-Fi Lock Settings
        WifiLockSettingsSection(
            wifiLockEnabled = wifiLockEnabled,
            onWifiLockEnabledChange = { hotspotViewModel.updateWifiLockEnabled(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Band Selection (if applicable)
        BandSelection(
            selectedBand = selectedBand,
            onBandSelected = { hotspotViewModel.updateSelectedBand(it) },
            bands = listOf("Auto", "2.4GHz", "5GHz"),
            isHotspotEnabled = isHotspotEnabled
        )

        Spacer(modifier = Modifier.height(16.dp))


        Spacer(modifier = Modifier.height(16.dp))


        Spacer(modifier = Modifier.height(16.dp))

        // Feedback Form
        FeedbackForm(onSubmit = { feedback ->
            hotspotViewModel.submitFeedback(feedback)
        })

        Spacer(modifier = Modifier.height(16.dp))

        // Contact Support Section
        ContactSupportSection(onContactSupport = {
            hotspotViewModel.contactSupport()
        })

        Spacer(modifier = Modifier.height(16.dp))

    }
}