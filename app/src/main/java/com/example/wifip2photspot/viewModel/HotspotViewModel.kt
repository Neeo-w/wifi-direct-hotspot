// HotspotViewModel.kt
package com.example.wifip2photspot.viewModel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.MacAddress
import android.net.Uri
import android.net.wifi.WifiManager
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Looper
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.contentcapture.ContentCaptureManager.Companion.isEnabled
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wifip2photspot.DeviceInfo
import com.example.wifip2photspot.HttpProxyServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class HotspotViewModel(
    application: Context,
    private val dataStore: DataStore<Preferences>,
) : AndroidViewModel(application as Application) {
    // ----- DataStore Keys -----
    companion object {
        val SSID_KEY = stringPreferencesKey("ssid")
        val PASSWORD_KEY = stringPreferencesKey("password")
        private val SELECTED_BAND_KEY = stringPreferencesKey("selected_band")
        val AUTO_SHUTDOWN_ENABLED_KEY = booleanPreferencesKey("auto_shutdown_enabled")
        val IDLE_TIMEOUT_MINUTES_KEY = intPreferencesKey("idle_timeout_minutes")
        private val BLOCKED_MAC_ADDRESSES_KEY = stringSetPreferencesKey("blocked_mac_addresses")
        private val DEVICE_ALIAS_KEY = stringPreferencesKey("device_aliases")
        private val WIFI_LOCK_ENABLED_KEY = booleanPreferencesKey("wifi_lock_enabled")
        private val _deviceAliases = MutableStateFlow<Map<String, String>>(emptyMap())
        private const val DEFAULT_PROXY_PORT = 8080
        private const val DEFAULT_PROXY_IP = "192.168.49.1"
    }

    //     ----- Wi-Fi P2P Manager and Channel -----
    private val wifiP2pManager: WifiP2pManager =
        application.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    private val channel: WifiP2pManager.Channel =
        wifiP2pManager.initialize(application, Looper.getMainLooper(), null)

    // ----- StateFlows for UI State -----
    private val _ssid = MutableStateFlow("TetherGuard")
    val ssid: StateFlow<String> = _ssid.asStateFlow()

    private val _password = MutableStateFlow("00000000")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _selectedBand = MutableStateFlow("Auto")
    val selectedBand: StateFlow<String> = _selectedBand.asStateFlow()


    private val _isWifiP2pEnabled = MutableStateFlow(false)

    private val _isHotspotEnabled = MutableStateFlow(false)
    val isHotspotEnabled: StateFlow<Boolean> = _isHotspotEnabled.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()


    // Dark Theme State
    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()
    fun updateTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
        // Save to DataStore if needed
    }
    // ----- Log Entries -----
    private val _logEntries = MutableStateFlow<List<String>>(emptyList())
    val logEntries: StateFlow<List<String>> = _logEntries.asStateFlow()
    // ----- UI Events -----
    private val _eventFlow = MutableSharedFlow<UiEvent>()
    // ----- Connected Devices -----
    private val _connectedDevices = MutableStateFlow<List<WifiP2pDevice>>(emptyList())
    val connectedDevices: StateFlow<List<WifiP2pDevice>> = _connectedDevices.asStateFlow()
    private val _connectedDeviceInfos = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val connectedDeviceInfos: StateFlow<List<DeviceInfo>> = _connectedDeviceInfos.asStateFlow()

    // ----- Sealed Class for UI Events -----
    sealed class UiEvent {
        data class ShowToast(val message: String) : UiEvent()
    }
    // Add a new property for blocked devices
    private val _blockedMacAddresses = MutableStateFlow<Set<String>>(emptySet())
    private val _blockedDeviceInfos = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val blockedDeviceInfos: StateFlow<List<DeviceInfo>> = _blockedDeviceInfos.asStateFlow()
    //battery level
    private val _batteryLevel = MutableStateFlow(100)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()
    private val _isIdle = MutableStateFlow(false)

    // Idle Settings State
    private val _autoShutdownEnabled = MutableStateFlow(false)
    val autoShutdownEnabled: StateFlow<Boolean> = _autoShutdownEnabled.asStateFlow()

    private val _idleTimeoutMinutes = MutableStateFlow(10)
    val idleTimeoutMinutes: StateFlow<Int> = _idleTimeoutMinutes.asStateFlow()

    // Remaining Idle Time State
    private val _remainingIdleTime = MutableStateFlow(0L)
    val remainingIdleTime: StateFlow<Long> = _remainingIdleTime.asStateFlow()

    // Wi-Fi Lock Variables
    private var wifiLock: WifiManager.WifiLock? = null

    // Wi-Fi Lock Enabled StateFlow
    private val _wifiLockEnabled = MutableStateFlow(false)
    val wifiLockEnabled: StateFlow<Boolean> = _wifiLockEnabled.asStateFlow()

    private val _groupInfo = MutableStateFlow<Pair<String, String>?>(null)
    val groupInfo: StateFlow<Pair<String, String>?> = _groupInfo

    private val _isGroupOwner = MutableStateFlow(false)
    val isGroupOwner: StateFlow<Boolean> = _isGroupOwner.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    // ----- HTTP Proxy Server -----
    private var proxyServer: HttpProxyServer? = null

    private val _proxyStatus = MutableStateFlow("Stopped")
    val proxyStatus: StateFlow<String> = _proxyStatus.asStateFlow()

    private var proxyPort = DEFAULT_PROXY_PORT
    private var proxyIp = DEFAULT_PROXY_IP

    // Flag to prevent multiple starts/stops
    private val isProxyRunning = AtomicBoolean(false)


    init {
        viewModelScope.launch {
            dataStore.data.catch { exception ->
                if (exception is IOException) emit(emptyPreferences())
                else throw exception
            }.collect { preferences ->
                _ssid.value = preferences[SSID_KEY] ?: "TetherGuard"
                _password.value = preferences[PASSWORD_KEY] ?: "00000000"
                _selectedBand.value = preferences[SELECTED_BAND_KEY] ?: "Auto"
            }
        }

        // Load other preferences
        viewModelScope.launch {
            dataStore.data.collect { preferences ->
                _autoShutdownEnabled.value = preferences[AUTO_SHUTDOWN_ENABLED_KEY] ?: false
                _idleTimeoutMinutes.value = preferences[IDLE_TIMEOUT_MINUTES_KEY] ?: 10
                _wifiLockEnabled.value = preferences[WIFI_LOCK_ENABLED_KEY] ?: false
            }
        }
        // Load blocked devices
        viewModelScope.launch {
            dataStore.data.collect { preferences ->
                val blockedAddresses = preferences[BLOCKED_MAC_ADDRESSES_KEY] ?: emptySet()
                _blockedMacAddresses.value = blockedAddresses
                updateBlockedDevices()
            }
        }
        // Load device aliases from DataStore
        viewModelScope.launch {
            dataStore.data.collect { preferences ->
                val aliasesJson = preferences[DEVICE_ALIAS_KEY] ?: "{}"
                _deviceAliases.value = Json.decodeFromString(aliasesJson)
            }
        }
    }
    // ----- Function to Handle BroadcastReceiver Calls -----
    fun onGroupOwnerChanged(isGroupOwner: Boolean) {
        viewModelScope.launch {
            if (isGroupOwner) {
                _isHotspotEnabled.value = true
                updateLog("Device is Group Owner. Starting SSH Server and VPN.")

            } else {
                _isHotspotEnabled.value = false
                updateLog("Device is Client. SSH Server and VPN are not required.")
            }
        }
    }
    // ----- Function to Handle Device List Changes -----
    @SuppressLint("TimberArgCount")
    fun onDevicesChanged(deviceList: Collection<WifiP2pDevice>) {
        _connectedDevices.value
        updateLog("Connected Devices: ${deviceList.size}")
        _connectedDevices.value = deviceList.toList()
        enforceAccessControl()
        // Check for new connections
        Timber.tag("HotspotViewModel").d("%s devices", "Devices changed: %s", deviceList.size)
        val blockedAddresses = _blockedMacAddresses.value
        _connectedDeviceInfos.value = deviceList.map { device ->
            val isBlocked = blockedAddresses.contains(device.deviceAddress)
            val existingInfo =
                _connectedDeviceInfos.value.find { it.device.deviceAddress == device.deviceAddress }
            existingInfo?.copy(isBlocked = isBlocked) ?: DeviceInfo(
                device = device,
                isBlocked = isBlocked
            )
        }
    }

    // ----- Function to Set Wi-Fi P2P Enabled State -----
    @OptIn(ExperimentalComposeUiApi::class)
    fun setWifiP2pEnabled(enabled: Boolean) {
        _isWifiP2pEnabled.value = enabled
        updateLog("Wi-Fi P2P Enabled: $isEnabled")
    }

    // ----- Function to Update Selected Band -----
// ----- Update Selected Band -----
    fun updateSelectedBand(newBand: String) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[SELECTED_BAND_KEY] = newBand
            }
            _selectedBand.value = newBand
            updateLog("Selected band updated to: $newBand")
        }
    }

    // ----- Function to Update SSID -----
    fun updateSSID(newSSID: String) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[SSID_KEY] = newSSID
            }
            _ssid.value = newSSID
            updateLog("SSID updated to: $newSSID")
        }
    }

    // ----- Function to Update Password -----
    fun updatePassword(newPassword: String) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[PASSWORD_KEY] = newPassword
            }
            _password.value = newPassword
            updateLog("Password updated.")
        }
    }

    // ----- Start Hotspot -----
    fun onButtonStartTapped(ssidInput: String, passwordInput: String, selectedBand: String) {
        onButtonStopTapped() // Stop existing hotspot before creating a new one

        viewModelScope.launch {
            _isProcessing.value = true

            val ssid = "DIRECT-TG-${ssidInput.trim()}"
            val password = passwordInput.trim()

            if (ssid.isEmpty() || password.length !in 8..63) {
                _isProcessing.value = false
                updateLog("Invalid SSID or password.")
                return@launch
            }
            val band = when (selectedBand) {
                "2.4GHz" -> WifiP2pConfig.GROUP_OWNER_BAND_2GHZ
                "5GHz" -> WifiP2pConfig.GROUP_OWNER_BAND_5GHZ
                else -> WifiP2pConfig.GROUP_OWNER_BAND_AUTO
            }

            val config = WifiP2pConfig.Builder()
                .setNetworkName(ssid)
                .setPassphrase(password)
                .enablePersistentMode(false)
                .setGroupOperatingBand(WifiP2pConfig.GROUP_OWNER_BAND_AUTO)
                .setGroupOperatingBand(band)
                .build()

            wifiP2pManager.requestGroupInfo(channel) { group ->
                if (group != null) {
                    wifiP2pManager.removeGroup(channel, object : WifiP2pManager.ActionListener {
                        override fun onSuccess() {
                            updateLog("Existing group removed. Creating new group...")
                            createNewGroup(config)
                        }

                        override fun onFailure(reason: Int) {
                            handleGroupFailure(reason, "remove")
                            updateLog("Failed to remove existing group. Creating new group...")
                            retryGroupRemoval(config)
                        }
                    })
                } else {
                    createNewGroup(config)
                }
            }
        }
    }
    private fun createNewGroup(config: WifiP2pConfig) {
        wifiP2pManager.createGroup(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                viewModelScope.launch {
                    _isHotspotEnabled.value = true
                    _isProcessing.value = false
                    updateLog("Hotspot started successfully: SSID=${config.networkName}")
                    requestGroupInfo()

                    // Acquire Wi-Fi Lock after hotspot starts successfully
                    acquireWifiLock()

                    // Start idle monitoring after hotspot is active
                    startIdleMonitoring()
                    updateLog("Idle monitoring started.")

                    // Start HTTP Proxy Server
                    startHttpProxy()
                }
            }

            override fun onFailure(reason: Int) {
                handleGroupFailure(reason, "create")
                retryGroupCreation(config)
            }
        })
    }

    private fun handleGroupFailure(reason: Int, action: String) {
        val reasonMessage = when (reason) {
            WifiP2pManager.BUSY -> "Framework is busy"
            WifiP2pManager.ERROR -> "Internal error"
            WifiP2pManager.P2P_UNSUPPORTED -> "P2P unsupported"
            else -> "Unknown error"
        }
        updateLog("Failed to $action group: $reasonMessage")
        _isProcessing.value = false
    }

    private fun requestGroupInfo() {
        wifiP2pManager.requestGroupInfo(channel) { group ->
            if (group != null) {
                _groupInfo.value = Pair(group.networkName, group.passphrase)
                _isGroupOwner.value = group.isGroupOwner
                updateLog("Group Info: SSID=${group.networkName}, Passphrase=${group.passphrase}")
            } else {
                _groupInfo.value = null
                updateLog("No group info available.")
            }
        }
    }

    private fun retryGroupCreation(config: WifiP2pConfig) {
        viewModelScope.launch {
            delay(2000L) // Wait 2 seconds before retrying
            createNewGroup(config)
        }
    }

    private fun retryGroupRemoval(config: WifiP2pConfig?) {
        viewModelScope.launch {
            delay(2000L) // Wait 2 seconds before retrying
            wifiP2pManager.removeGroup(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    updateLog("Group removed after retry. Creating new group...")
                    config?.let { createNewGroup(it) }
                }

                override fun onFailure(reason: Int) {
                    updateLog("Failed to remove group after retry: ${getErrorReason(reason)}")
                    _isProcessing.value = false
                }
            })
        }
    }

    // ----- Stop Hotspot -----
    fun onButtonStopTapped() {
        if (!_isHotspotEnabled.value) return

        viewModelScope.launch {
            _isProcessing.value = true
            wifiP2pManager.removeGroup(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    viewModelScope.launch {
                        clearHotspotState()
                        updateLog("Hotspot stopped successfully.")

                        // Release Wi-Fi Lock after stopping the hotspot
                        releaseWifiLock()
                        // Stop HTTP Proxy Server
                        stopHttpProxy()
                    }
                }
                override fun onFailure(reason: Int) {
                    handleGroupFailure(reason, "stop")
                    retryGroupRemoval(null)
                }
            })
        }
    }

    private fun startHttpProxy() {
        try {
            proxyServer = HttpProxyServer(proxyPort, proxyIp)
            proxyServer?.start()
            _proxyStatus.value = "Running on $proxyIp:$proxyPort"
            updateLog("HTTP Proxy started on $proxyIp:$proxyPort")
        } catch (e: Exception) {
            _proxyStatus.value = "Failed to start"
            updateLog("Failed to start HTTP Proxy: ${e.message}")
        }
    }

    private fun stopHttpProxy() {
        try {
            proxyServer?.stop()
            proxyServer = null
            _proxyStatus.value = "Stopped"
            updateLog("HTTP Proxy stopped.")
        } catch (e: Exception) {
            updateLog("Error stopping HTTP Proxy: ${e.message}")
        }
    }


    private fun getErrorReason(reason: Int): String {
        return when (reason) {
            WifiP2pManager.BUSY -> "Framework is busy"
            WifiP2pManager.ERROR -> "Internal error"
            WifiP2pManager.P2P_UNSUPPORTED -> "P2P unsupported"
            WifiP2pManager.NO_SERVICE_REQUESTS -> "No service requests"
            else -> "Unknown error code: $reason"
        }
    }

    private fun clearHotspotState() {
        _isHotspotEnabled.value = false
        _isGroupOwner.value = false
        _groupInfo.value = null
        _connectedDevices.value = emptyList()
        _isProcessing.value = false
    }
    // ----- Handle Disconnection -----
    fun onDisconnected() {
        clearHotspotState()
        updateLog("Group disbanded. All devices disconnected.")
    }
    // ----- Lifecycle Management -----
    override fun onCleared() {
        super.onCleared()
        releaseWifiLock()
        onButtonStopTapped()
        updateLog("ViewModel cleared. Resources released.")
    }

    // ----- Logging Utility -----
    fun updateLog(message: String) {
        viewModelScope.launch {
            _logEntries.value += message
        }
    }

    // Function to update auto shutdown enabled state
    fun updateAutoShutdownEnabled(enabled: Boolean) {
        _autoShutdownEnabled.value = enabled
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[AUTO_SHUTDOWN_ENABLED_KEY] = enabled
            }
        }
    }

    // Function to update idle timeout minutes
    fun updateIdleTimeoutMinutes(minutes: Int) {
        _idleTimeoutMinutes.value = minutes
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[IDLE_TIMEOUT_MINUTES_KEY] = minutes
            }
        }
    }

    // WifiLock Management Functions
    fun updateWifiLockEnabled(enabled: Boolean) {
        _wifiLockEnabled.value = enabled
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[WIFI_LOCK_ENABLED_KEY] = enabled
            }
        }
    }

    // ----- Wi-Fi Lock -----
    fun acquireWifiLock() {
        if (wifiLock == null) {
            val wifiManager = getApplication<Application>().getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "WiFiP2PHotspotLock")
        }
        wifiLock?.takeIf { !it.isHeld }?.acquire()
    }

    fun releaseWifiLock() {
        wifiLock?.takeIf { it.isHeld }?.release()
        wifiLock = null
    }

    fun startIdleMonitoring() {
        viewModelScope.launch {
            var idleStartTime = System.currentTimeMillis() // Track when the hotspot becomes idle
            while (isHotspotEnabled.value && autoShutdownEnabled.value) {
                delay(1000L) // Check every second
                val connectedDevices = _connectedDevices.value // Get the current list of connected devices

                // Determine if the hotspot is idle (no connected devices)
                _isIdle.value = connectedDevices.isEmpty()

                if (_isIdle.value && _autoShutdownEnabled.value) {
                    val elapsedIdleTime = System.currentTimeMillis() - idleStartTime
                    val totalIdleTime = _idleTimeoutMinutes.value * 60 * 1000L

                    // Update the remaining idle time
                    _remainingIdleTime.value = totalIdleTime - elapsedIdleTime

                    // If idle time exceeds the allowed timeout, stop the hotspot
                    if (_remainingIdleTime.value <= 0L) {
                        withContext(Dispatchers.Main) {
                            onButtonStopTapped()
                            _eventFlow.emit(UiEvent.ShowToast("Hotspot turned off due to inactivity"))
                        }
                        break
                    }
                } else {
                    // Reset idle start time if activity is detected
                    idleStartTime = System.currentTimeMillis()
                    _remainingIdleTime.value = _idleTimeoutMinutes.value * 60 * 1000L
                }
            }
            // Reset remaining idle time when monitoring stops
            _remainingIdleTime.value = 0L
        }
    }


    // Function to block a device
    fun blockDevice(deviceAddress: String) {
        // Add to blocked addresses
        val updatedSet = _blockedMacAddresses.value + deviceAddress
        _blockedMacAddresses.value = updatedSet

        // Save to DataStore
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[BLOCKED_MAC_ADDRESSES_KEY] = updatedSet
            }
        }

        // Update blocked devices list
        updateBlockedDevices()

        // Enforce access control
        enforceAccessControl()
    }

    // Function to unblock a device
    fun unblockDevice(deviceAddress: String) {
        // Remove from blocked addresses
        val updatedSet = _blockedMacAddresses.value - deviceAddress
        _blockedMacAddresses.value = updatedSet

        // Save to DataStore
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[BLOCKED_MAC_ADDRESSES_KEY] = updatedSet
            }
        }

        // Update blocked devices list
        updateBlockedDevices()
    }

    // Function to load blocked devices
    private fun updateBlockedDevices() {
        _blockedDeviceInfos.value = _blockedMacAddresses.value.map { macAddress ->
            DeviceInfo(
                device = WifiP2pDevice().apply { deviceAddress = macAddress },
                isBlocked = true
            )
        }
    }

    // Enforce access control based on blocked devices
    private fun enforceAccessControl() {
        val devicesToDisconnect = _connectedDeviceInfos.value.filter { deviceInfo ->
            deviceInfo.isBlocked
        }

        devicesToDisconnect.forEach { deviceInfo ->
            disconnectDevice(deviceInfo)
        }
    }

    fun disconnectDevice(deviceInfo: DeviceInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                // Convert the string MAC address to a MacAddress object for API 33+
                val macAddress = MacAddress.fromString(deviceInfo.device.deviceAddress)

                // Create the Wi-Fi P2P configuration for the specific device
                val config = WifiP2pConfig.Builder()
                    .setDeviceAddress(macAddress)
                    .build()

                cancelConnection(config, deviceInfo)
            } catch (e: IllegalArgumentException) {
                updateLog("Invalid MAC address format for device: ${deviceInfo.device.deviceAddress}")
            }
        } else {
            // For API levels below 33, use the WifiP2pConfig constructor
            val config = WifiP2pConfig().apply {
                deviceAddress = deviceInfo.device.deviceAddress
                wps.setup = WpsInfo.PBC // Set WPS setup method
            }

            cancelConnection(config, deviceInfo)
        }
    }
    private fun cancelConnection(config: WifiP2pConfig, deviceInfo: DeviceInfo) {
        wifiP2pManager.cancelConnect(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                updateLog("Successfully disconnected device: ${deviceInfo.device.deviceName}")

                // Update connected devices list by removing the disconnected device
                _connectedDeviceInfos.value = _connectedDeviceInfos.value.filterNot {
                    it.device.deviceAddress == deviceInfo.device.deviceAddress
                }
            }
            override fun onFailure(reason: Int) {
                val reasonStr = when (reason) {
                    WifiP2pManager.BUSY -> "Framework is busy"
                    WifiP2pManager.ERROR -> "Internal error"
                    WifiP2pManager.P2P_UNSUPPORTED -> "P2P unsupported"
                    else -> "Unknown error"
                }
                updateLog("Failed to disconnect device: ${deviceInfo.device.deviceName}. Reason: $reasonStr")
            }
        })
    }


    //    // Update alias function
    fun updateDeviceAlias(deviceAddress: String, alias: String) {
        // Update aliases map
        val updatedAliases = _deviceAliases.value.toMutableMap()
        updatedAliases[deviceAddress] = alias
        _deviceAliases.value = updatedAliases

        // Save to DataStore
        val aliasesJson = Json.encodeToString(updatedAliases)
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[DEVICE_ALIAS_KEY] = aliasesJson
            }
        }
        // Update connected devices
        _connectedDeviceInfos.value = _connectedDeviceInfos.value.map { deviceInfo ->
            if (deviceInfo.device.deviceAddress == deviceAddress) {
                deviceInfo.copy(alias = alias)
            } else {
                deviceInfo
            }
        }
    }


    fun contactSupport() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("2024@example.com"))
            putExtra(Intent.EXTRA_SUBJECT, "Support Request")
        }
        intent.resolveActivity(getApplication<Application>().packageManager)?.let {
            getApplication<Application>().startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } ?: run {
            viewModelScope.launch {
                _eventFlow.emit(UiEvent.ShowToast("No email client available"))
            }
        }
    }

    fun submitFeedback(feedback: String) {
        // For example, open an email intent
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("support@example.com"))
            putExtra(Intent.EXTRA_SUBJECT, "App Feedback")
            putExtra(Intent.EXTRA_TEXT, feedback)
        }
        intent.resolveActivity(getApplication<Application>().packageManager)?.let {
            getApplication<Application>().startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } ?: run {
            // Handle the case where no email client is available
            viewModelScope.launch {
                _eventFlow.emit(UiEvent.ShowToast("No email client available"))
            }
        }
    }
}
