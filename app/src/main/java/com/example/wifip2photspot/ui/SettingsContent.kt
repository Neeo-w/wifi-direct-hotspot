package com.example.wifip2photspot.ui


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.net.VpnService
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.wifip2photspot.BandSelection
import com.example.wifip2photspot.ContactSupportSection
import com.example.wifip2photspot.FeedbackForm
import com.example.wifip2photspot.HotspotViewModel
import com.example.wifip2photspot.NotificationSettingsSection
import com.example.wifip2photspot.ui.theme.ThemeToggle

@Composable
fun SettingsContent(viewModel: HotspotViewModel, paddingValues: PaddingValues) {
    // Collect necessary state from ViewModel
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val notificationEnabled by viewModel.notificationEnabled.collectAsState()
    val soundEnabled by viewModel.notificationSoundEnabled.collectAsState()
    val vibrationEnabled by viewModel.notificationVibrationEnabled.collectAsState()
    val autoShutdownEnabled by viewModel.autoShutdownEnabled.collectAsState()
    val idleTimeoutMinutes by viewModel.idleTimeoutMinutes.collectAsState()
    val wifiLockEnabled by viewModel.wifiLockEnabled.collectAsState()
    val selectedBand by viewModel.selectedBand.collectAsState()
    val isHotspotEnabled by viewModel.isHotspotEnabled.collectAsState()

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
                viewModel.updateTheme(isDark)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Notification Settings
        NotificationSettingsSection(
            notificationEnabled = notificationEnabled,
            onNotificationEnabledChange = { viewModel.updateNotificationEnabled(it) },
            soundEnabled = soundEnabled,
            onSoundEnabledChange = { viewModel.updateNotificationSoundEnabled(it) },
            vibrationEnabled = vibrationEnabled,
            onVibrationEnabledChange = { viewModel.updateNotificationVibrationEnabled(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Idle Settings
        IdleSettingsSection(
            autoShutdownEnabled = autoShutdownEnabled,
            onAutoShutdownEnabledChange = { viewModel.updateAutoShutdownEnabled(it) },
            idleTimeoutMinutes = idleTimeoutMinutes,
            onIdleTimeoutChange = { viewModel.updateIdleTimeoutMinutes(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Wi-Fi Lock Settings
        WifiLockSettingsSection(
            wifiLockEnabled = wifiLockEnabled,
            onWifiLockEnabledChange = { viewModel.updateWifiLockEnabled(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Band Selection (if applicable)
        BandSelection(
            selectedBand = selectedBand,
            onBandSelected = { viewModel.updateSelectedBand(it) },
            bands = listOf("Auto", "2.4GHz", "5GHz"),
            isHotspotEnabled = isHotspotEnabled
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Feedback Form
        FeedbackForm(onSubmit = { feedback ->
            viewModel.submitFeedback(feedback)
        })

        Spacer(modifier = Modifier.height(16.dp))

        // Contact Support Section
        ContactSupportSection(onContactSupport = {
            viewModel.contactSupport()
        })

        Spacer(modifier = Modifier.height(16.dp))

        // Add more settings sections as needed...
    }
}