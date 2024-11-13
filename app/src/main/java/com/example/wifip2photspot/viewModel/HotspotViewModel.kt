// HotspotViewModel.kt
package com.example.wifip2photspot.viewModel

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.TrafficStats
import android.net.Uri
import android.net.wifi.WifiManager
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.BatteryManager
import android.os.Build
import android.os.Looper
import android.service.controls.ControlsProviderService.TAG
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.contentcapture.ContentCaptureManager.Companion.isEnabled
import androidx.core.app.NotificationCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.wifip2photspot.DeviceInfo
import com.example.wifip2photspot.R
import com.example.wifip2photspot.StartHotspotWorker
import com.example.wifip2photspot.StopHotspotWorker
import com.example.wifip2photspot.VPN.MyVpnService
import com.example.wifip2photspot.VPN.VpnRepository
import com.example.wifip2photspot.socksProxy.SSHServerManager

import com.github.mikephil.charting.data.Entry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@RequiresApi(Build.VERSION_CODES.Q)
class HotspotViewModel(
    application: Context,
    private val dataStore: DataStore<Preferences>,
    private val vpnRepository: VpnRepository
) : AndroidViewModel(application as Application) {
    // ----- DataStore Keys -----
    companion object {
        val SSID_KEY = stringPreferencesKey("ssid")
        val PASSWORD_KEY = stringPreferencesKey("password")
        val AUTO_SHUTDOWN_ENABLED_KEY = booleanPreferencesKey("auto_shutdown_enabled")
        val IDLE_TIMEOUT_MINUTES_KEY = intPreferencesKey("idle_timeout_minutes")
        private val BLOCKED_MAC_ADDRESSES_KEY = stringSetPreferencesKey("blocked_mac_addresses")
        private val DEVICE_ALIAS_KEY = stringPreferencesKey("device_aliases")
        private val WIFI_LOCK_ENABLED_KEY = booleanPreferencesKey("wifi_lock_enabled")
        private val _deviceAliases = MutableStateFlow<Map<String, String>>(emptyMap())
        val BIND_TO_MOBILE_DATA_KEY = booleanPreferencesKey("bind_to_mobile_data") // New Key



    }

    // Preferences Keys
    private object PreferencesKeys {
        val WIFI_LOCK_ENABLED = booleanPreferencesKey("wifi_lock_enabled")
        private val DATA_USAGE_KEY = stringPreferencesKey("device_usage_key")
        private val DARK_THEME_KEY = stringPreferencesKey("dark_theme_key")


        // Other preference keys...
    }

    // Enum for themes
    enum class AppTheme {
        DEFAULT, DARK, AMOLED, CUSTOM
    }

    //     ----- Wi-Fi P2P Manager and Channel -----
    val wifiManager: WifiP2pManager =
        application.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    val channel: WifiP2pManager.Channel =
        wifiManager.initialize(application, Looper.getMainLooper(), null)


    // ----- StateFlows for UI State -----
    private val _ssid = MutableStateFlow("TetherGuard")
    val ssid: StateFlow<String> = _ssid.asStateFlow()

    private val _password = MutableStateFlow("00000000")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _selectedBand = MutableStateFlow("Auto")
    val selectedBand: StateFlow<String> = _selectedBand.asStateFlow()

    private val _isWifiP2pEnabled = MutableStateFlow(false)
    val isWifiP2pEnabled: StateFlow<Boolean> = _isWifiP2pEnabled.asStateFlow()

    private val _isHotspotEnabled = MutableStateFlow(false)
    val isHotspotEnabled: StateFlow<Boolean> = _isHotspotEnabled.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _uploadSpeed = MutableStateFlow(0) // in kbps
    val uploadSpeed: StateFlow<Int> = _uploadSpeed.asStateFlow()

    private val _downloadSpeed = MutableStateFlow(0) // in kbps
    val downloadSpeed: StateFlow<Int> = _downloadSpeed.asStateFlow()

    private var previousTxBytes = TrafficStats.getTotalTxBytes()
    private var previousRxBytes = TrafficStats.getTotalRxBytes()

    private var sessionStartRxBytes = 0L
    private var sessionStartTxBytes = 0L


    // Dark Theme State
    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()


    // ----- Log Entries -----
    private val _logEntries = MutableStateFlow<List<String>>(emptyList())
    val logEntries: StateFlow<List<String>> = _logEntries.asStateFlow()

    // ----- UI Events -----
    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow: SharedFlow<UiEvent> = _eventFlow.asSharedFlow()

    // ----- Connected Devices -----
    private val _connectedDevices = MutableStateFlow<List<WifiP2pDevice>>(emptyList())
    val connectedDevices: StateFlow<List<WifiP2pDevice>> = _connectedDevices.asStateFlow()
    private val _connectedDeviceInfos = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val connectedDeviceInfos: StateFlow<List<DeviceInfo>> = _connectedDeviceInfos.asStateFlow()

    //visible password
    private var passwordVisible by mutableStateOf(false)
    // SSH Server Configuration
    private var sshServerManager: SSHServerManager? = null

    // ----- StateFlows and Variables -----
    private val _bindToMobileData = MutableStateFlow(false)
    val bindToMobileData: StateFlow<Boolean> = _bindToMobileData.asStateFlow()



    // ----- Sealed Class for UI Events -----
    sealed class UiEvent {
        data class ShowToast(val message: String) : UiEvent()
        data class ShowSnackbar(val message: String) : UiEvent()
        object StartProxyService : UiEvent()
        object StopProxyService : UiEvent()
    }

    // StateFlows to hold the lists
//    private val _allowedMacAddresses = MutableStateFlow<Set<String>>(emptySet())

    // Add a new property for blocked devices
    private val _blockedMacAddresses = MutableStateFlow<Set<String>>(emptySet())
    val blockedMacAddresses: StateFlow<Set<String>> = _blockedMacAddresses.asStateFlow()

    private val _blockedDeviceInfos = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val blockedDeviceInfos: StateFlow<List<DeviceInfo>> = _blockedDeviceInfos.asStateFlow()

    private val sshLocalPort = 2222 // SSH Server Port
    private val proxyPort = 8181 // SOCKS Proxy Port


    // Load aliases in init block
//
//    private val _historicalDataUsage = MutableStateFlow<List<DataUsageRecord>>(emptyList())
//    val historicalDataUsage: StateFlow<List<DataUsageRecord>> = _historicalDataUsage.asStateFlow()

    private val _uploadSpeedEntries = MutableStateFlow<List<Entry>>(emptyList())
    val uploadSpeedEntries: StateFlow<List<Entry>> = _uploadSpeedEntries.asStateFlow()

    private val _downloadSpeedEntries = MutableStateFlow<List<Entry>>(emptyList())
    val downloadSpeedEntries: StateFlow<List<Entry>> = _downloadSpeedEntries.asStateFlow()


    // Variables to store current speeds in Kbps
    private val _uploadSpeedKbps = MutableStateFlow(0f)
    val uploadSpeedKbps: StateFlow<Float> = _uploadSpeedKbps.asStateFlow()

    private val _downloadSpeedKbps = MutableStateFlow(0f)
    val downloadSpeedKbps: StateFlow<Float> = _downloadSpeedKbps.asStateFlow()

    private val _dataUsageThreshold = MutableStateFlow<Long>(0L) // in bytes
    val dataUsageThreshold: StateFlow<Long> = _dataUsageThreshold.asStateFlow()

    // Notification Settings State
    private val _notificationEnabled = MutableStateFlow(true)
    val notificationEnabled: StateFlow<Boolean> = _notificationEnabled.asStateFlow()

    private val notificationManager =
        application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val _notificationSoundEnabled = MutableStateFlow(true)
    val notificationSoundEnabled: StateFlow<Boolean> = _notificationSoundEnabled.asStateFlow()

    private val _notificationVibrationEnabled = MutableStateFlow(true)
    val notificationVibrationEnabled: StateFlow<Boolean> =
        _notificationVibrationEnabled.asStateFlow()

    //battery level
    private val _batteryLevel = MutableStateFlow(100)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val _networkQuality = MutableStateFlow("Good")
    val networkQuality: StateFlow<String> = _networkQuality.asStateFlow()


    private val _isIdle = MutableStateFlow(false)
    val isIdle: StateFlow<Boolean> = _isIdle.asStateFlow()

//    private val _remainingIdleTime = MutableStateFlow<Long>(0L)
//    val remainingIdleTime: StateFlow<Long> = _remainingIdleTime.asStateFlow()



    private val idleThresholdBytesPerSecond = 100L // Define your threshold
    private val idleCheckIntervalMillis = 30000L // Check every 30 seconds
    private val idleTimeoutMillis = 600000L // 10 minutes of inactivity
    private var lastTotalBytes: Long = 0L

    //    private var lastTotalBytes: Long = 0L
    private var idleStartTime: Long = 0L
    private var isCountingDown: Boolean = false

    private val _appTheme = MutableStateFlow(AppTheme.DEFAULT)
    val appTheme: StateFlow<AppTheme> = _appTheme.asStateFlow()

    // Idle Settings State
    private val _autoShutdownEnabled = MutableStateFlow(false)
    val autoShutdownEnabled: StateFlow<Boolean> = _autoShutdownEnabled.asStateFlow()

    private val _idleTimeoutMinutes = MutableStateFlow(10)
    val idleTimeoutMinutes: StateFlow<Int> = _idleTimeoutMinutes.asStateFlow()

    // Remaining Idle Time State
    private val _remainingIdleTime = MutableStateFlow<Long>(0L)
    val remainingIdleTime: StateFlow<Long> = _remainingIdleTime.asStateFlow()

    // Wi-Fi Lock Variables
    private var wifiLock: WifiManager.WifiLock? = null

    // Wi-Fi Lock Enabled StateFlow
    private val _wifiLockEnabled = MutableStateFlow(false)
    val wifiLockEnabled: StateFlow<Boolean> = _wifiLockEnabled.asStateFlow()
//    private val socksProxyManager = SocksProxyManager(proxyPort = proxyPort)


//    // Proxy server properties
//    private val _isProxyRunning = MutableStateFlow(false)
//    val isProxyRunning: StateFlow<Boolean> = _isProxyRunning.asStateFlow()
//
//    private val _proxyPort = MutableStateFlow(8080)
//    val proxyPort: StateFlow<Int> = _proxyPort.asStateFlow()


    //    // Update Proxy Port
//    fun updateProxyPort(port: Int) {
//        _proxyPort.value = port
//    }
    // SSH Server Configuration


    // ----- Function to Update Bind to Mobile Data Preference -----
    fun updateBindToMobileData(enabled: Boolean) {
        _bindToMobileData.value = enabled
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[BIND_TO_MOBILE_DATA_KEY] = enabled
            }
            if (_isHotspotEnabled.value) {
                // Restart SSH server with updated binding preference
                sshServerManager?.stopSSHServer()
                sshServerManager?.startSSHServer(
                    localPort = sshLocalPort
//                    bindToMobileData = enabled
                )
                updateLog("SSH Server binding to mobile data: $enabled")
            }
        }
    }



    ///////////////////
    // VPN Control
    private val _isVpnActive = MutableStateFlow(false)
    val isVpnActive: StateFlow<Boolean> = _isVpnActive.asStateFlow()

    // ----- SSH Server Management -----
    // Start SSH Server
    private fun startSSHServer() {
        viewModelScope.launch {
            sshServerManager?.startSSHServer(
                localPort = sshLocalPort,
//                bindToMobileData = _bindToMobileData.value
            )
            updateLog("SSH Server started on port $sshLocalPort")
        }
    }

    // Stop SSH Server
    private fun stopSSHServer() {
        viewModelScope.launch {
            sshServerManager?.stopSSHServer()
            updateLog("SSH Server stopped.")
        }
    }


    init {
        // ----- Load SSID and Password from DataStore -----
        // ----- Load SSID and Password from DataStore -----
        viewModelScope.launch {
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }
                .collect { preferences ->
                    val newSSID = preferences[SSID_KEY] ?: "TetherGuard"
                    val newPassword = preferences[PASSWORD_KEY] ?: "00000000"
                    _ssid.value = newSSID
                    _password.value = newPassword

                    // Initialize SSHServerManager with updated credentials
                    sshServerManager?.stopSSHServer()
                    sshServerManager = SSHServerManager(
                        context = getApplication<Application>().applicationContext,
                        sshUsername = newSSID,
                        sshPassword = newPassword,
                        proxyPort = proxyPort
                    )

                    // If hotspot is already enabled, restart SSH server with new credentials
                    if (_isHotspotEnabled.value) {
                        // Stop and restart SSH server to apply new credentials
                        sshServerManager?.stopSSHServer()
                        sshServerManager?.startSSHServer(
                            localPort = sshLocalPort
//                            bindToMobileData = _bindToMobileData.value
                        )
                        updateLog("SSH Server updated with new SSID and Password.")
                    }
                }
        }

        // Load other preferences
        viewModelScope.launch {
            dataStore.data.collect { preferences ->
                _autoShutdownEnabled.value = preferences[AUTO_SHUTDOWN_ENABLED_KEY] ?: false
                _idleTimeoutMinutes.value = preferences[IDLE_TIMEOUT_MINUTES_KEY] ?: 10
                _wifiLockEnabled.value = preferences[WIFI_LOCK_ENABLED_KEY] ?: false
                _bindToMobileData.value = preferences[BIND_TO_MOBILE_DATA_KEY] ?: false
                // Load other preferences...
            }
        }
        // ----- Start Monitoring Network Speeds -----
        viewModelScope.launch {
            var time = 0f
            while (true) {
                delay(1000) // Update every second
                val currentTxBytes = TrafficStats.getTotalTxBytes()
                val currentRxBytes = TrafficStats.getTotalRxBytes()

                val txBytesDiff = currentTxBytes - previousTxBytes
                val rxBytesDiff = currentRxBytes - previousRxBytes

                previousTxBytes = currentTxBytes
                previousRxBytes = currentRxBytes

                // Convert bytes to kilobits per second (kbps)
                val uploadSpeedKbps = (txBytesDiff * 8) / 1000
                val downloadSpeedKbps = (rxBytesDiff * 8) / 1000

                _uploadSpeed.value = uploadSpeedKbps.toInt()
                _downloadSpeed.value = downloadSpeedKbps.toInt()
                // Update entries
                _uploadSpeedEntries.value += Entry(time, uploadSpeedKbps.toFloat())
                _downloadSpeedEntries.value += Entry(time, downloadSpeedKbps.toFloat())
                time += 1f

                // Limit the number of entries to, e.g., 60
                if (_uploadSpeedEntries.value.size > 60) {
                    _uploadSpeedEntries.value = _uploadSpeedEntries.value.drop(1)
                    _downloadSpeedEntries.value = _downloadSpeedEntries.value.drop(1)
                }

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

        monitorNetworkSpeeds()
//// Start additional monitoring services
        startBatteryMonitoring()
        startNetworkMonitoring()
//        viewModelScope.launch {
//            dataStore.data.collect { preferences ->
//                _dataUsageThreshold.value = preferences[DATA_USAGE_THRESHOLD_KEY] ?: 0L
//            }
//        }
    }

    private fun startVpnService() {
        val vpnIntent = Intent(getApplication(), MyVpnService::class.java)
        getApplication<Application>().startService(vpnIntent)
        updateLog("VPN Service Started via HotspotViewModel.")
    }

    private fun stopVpnService() {
        val vpnIntent = Intent(getApplication(), MyVpnService::class.java)
        getApplication<Application>().stopService(vpnIntent)
        updateLog("VPN Service Stopped via HotspotViewModel.")
    }
//    // ----- Methods to Handle BroadcastReceiver Calls -----
//    fun startTethering(ssid: String, password: String, selectedBand: Int) {
//        viewModelScope.launch {
//            _isProcessing.value = true
//            // Start Wi-Fi Direct group with specified SSID, password, and band
//            // Implement group creation logic here using WifiP2pManager APIs
//            // Once the group is successfully formed, the BroadcastReceiver will handle the rest
//            _isHotspotEnabled.value = true
//            updateLog("Tethering started with SSID: $ssid")
//            // Start VPN via repository
//            vpnRepository.startVpn()
//            _isProcessing.value = false
//        }
//    }
//
//    fun stopTethering() {
//        viewModelScope.launch {
//            _isProcessing.value = true
//            // Stop Wi-Fi Direct group
//            // Implement group disbanding logic here using WifiP2pManager APIs
//            _isHotspotEnabled.value = false
//            updateLog("Tethering stopped.")
//            // Stop VPN via repository
//            vpnRepository.stopVpn()
//            _isProcessing.value = false
//        }
//    }

    ///////////////////////////////////////////////////////
    @Suppress("EXPERIMENTAL_API_USAGE")
    private fun monitorNetworkSpeeds() {
        viewModelScope.launch {
            while (true) {
                delay(1000L)

                val currentRxBytes = TrafficStats.getTotalRxBytes()
                val currentTxBytes = TrafficStats.getTotalTxBytes()

                // Calculate the difference in bytes over the last second
                val rxBytes = currentRxBytes - previousRxBytes
                val txBytes = currentTxBytes - previousTxBytes

                // Update previous bytes for next calculation
                previousRxBytes = currentRxBytes
                previousTxBytes = currentTxBytes

                // Convert bytes to kilobits per second (Kbps)
                val currentDownloadSpeedKbps = (rxBytes * 8) / 1000f
                val currentUploadSpeedKbps = (txBytes * 8) / 1000f

                // Update the StateFlows
                _downloadSpeedKbps.value = currentDownloadSpeedKbps
                _uploadSpeedKbps.value = currentUploadSpeedKbps

                // Update entries for the graph
                val time = (System.currentTimeMillis() / 1000f) // Time in seconds

                _downloadSpeedEntries.value += Entry(time, currentDownloadSpeedKbps)
                _uploadSpeedEntries.value += Entry(time, currentUploadSpeedKbps)

                // Limit the number of entries to, e.g., 60
                if (_downloadSpeedEntries.value.size > 60) {
                    _downloadSpeedEntries.value = _downloadSpeedEntries.value.drop(1)
                    _uploadSpeedEntries.value = _uploadSpeedEntries.value.drop(1)
                }
            }
        }
    }



    // ----- Function to Handle BroadcastReceiver Calls -----
    fun onGroupOwnerChanged(isGroupOwner: Boolean) {
        viewModelScope.launch {
            if (isGroupOwner) {
                _isHotspotEnabled.value = true
                updateLog("Device is Group Owner. Starting SSH Server and VPN.")
                // Start SSH Server
                startSSHServer()
                // Start VPN via repository
                vpnRepository.startVpn()
//                startVpn()
            } else {
                _isHotspotEnabled.value = false
                updateLog("Device is Client. SSH Server and VPN are not required.")
                // Optionally, handle client-specific logic
            }
        }
    }

//    // Function to start VPN
//    private fun startVpn() {
//        val vpnIntent = Intent(getApplication(), MyVpnService::class.java)
//        getApplication<Application>().startService(vpnIntent)
//        _isVpnActive.value = true
//        updateLog("VPN Service Started")
//        viewModelScope.launch {
//            _eventFlow.emit(UiEvent.ShowToast("VPN started successfully."))
//        }
//    }
//
//    // Function to stop VPN
//    private fun stopVpn() {
//        val vpnIntent = Intent(getApplication(), MyVpnService::class.java)
//        getApplication<Application>().stopService(vpnIntent)
//        _isVpnActive.value = false
//        updateLog("VPN Service Stopped")
//        viewModelScope.launch {
//            _eventFlow.emit(UiEvent.ShowToast("VPN stopped successfully."))
//        }
//    }
    ////////*(((((((((((***********))))))))))))///////////////////// //////////////////////////////working fine //////////////////////////////////
    // ----- Helper Methods -----
    fun updateLog(message: String) {
        viewModelScope.launch {
            _logEntries.value += message
            // Alternatively, implement a logging mechanism or update a StateFlow for logs
        }
    }

    // ----- Function to Handle Device List Changes -----
    fun onDevicesChanged(deviceList: Collection<WifiP2pDevice>) {
        val previousDevices = _connectedDevices.value
        updateLog("Connected Devices: ${deviceList.size}")
        _connectedDevices.value = deviceList.toList()
        enforceAccessControl()
        // Check for new connections
        val newDevices = _connectedDevices.value - previousDevices
        val disconnectedDevices = previousDevices - _connectedDevices.value

        newDevices.forEach { device ->
            sendDeviceConnectionNotification(device, connected = true)
        }
        disconnectedDevices.forEach { device ->
            sendDeviceConnectionNotification(device, connected = false)
        }

        Log.d("HotspotViewModel", "Devices changed: ${deviceList.size} devices")
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
    fun onDisconnected() {
        viewModelScope.launch {
            _isHotspotEnabled.value = false
            updateLog("Group disbanded. Stopping SSH Server and VPN.")
            // Stop SSH Server
            stopSSHServer()
            // Stop VPN via repository
            vpnRepository.stopVpn()
//            stopVpn()
        }
    }
    // ----- Lifecycle Management -----
    override fun onCleared() {
        super.onCleared()
        releaseWifiLock()
        stopSSHServer()
        onButtonStopTapped()
        updateLog("SSH Server and Proxy Server cleaned up.")
        // Cancel any ongoing coroutines if necessary
    }


    // ----- Function to Set Wi-Fi P2P Enabled State -----
    @OptIn(ExperimentalComposeUiApi::class)
    fun setWifiP2pEnabled(enabled: Boolean) {
        _isWifiP2pEnabled.value = enabled
        updateLog("Wi-Fi P2P Enabled: $isEnabled")
    }

    // ----- Function to Update Selected Band -----
    fun updateSelectedBand(newBand: String) {
        viewModelScope.launch {
            _selectedBand.value = newBand
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

    // ----- Function to Start Tethering -----
    fun startTethering() {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                // Start Wi-Fi Direct group with SSID and Password
                onButtonStartTapped(ssid.value, password.value, selectedBand.value)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting tethering: ${e.message}")
                _isHotspotEnabled.value = false
                _isProcessing.value = false
                _eventFlow.emit(UiEvent.ShowToast("Error starting tethering: ${e.message}"))
            }
        }
    }
    // ----- Function to Handle Start Hotspot Button Tapped -----
    private fun onButtonStartTapped(
        ssidInput: String,
        passwordInput: String,
        selectedBand: String
    ) {
        onButtonStopTapped()
        viewModelScope.launch {
            if (!_isWifiP2pEnabled.value) {
                updateLog("Error: Cannot start hotspot. Wi-Fi P2P is not enabled.")
                _eventFlow.emit(UiEvent.ShowToast("Wi-Fi P2P is not enabled."))
                _isHotspotEnabled.value = false
                _isProcessing.value = false
                return@launch
            }
            _isProcessing.value = true // Start processing

            val ssidTrimmed = ssidInput.trim()
            val passwordTrimmed = passwordInput.trim()

            // ----- Input Validation -----
            if (ssidTrimmed.isEmpty()) {
                updateLog("Error: SSID cannot be empty.")
                _eventFlow.emit(UiEvent.ShowToast("SSID cannot be empty."))
                _isHotspotEnabled.value = false
                _isProcessing.value = false
                return@launch
            }

            if (passwordTrimmed.length !in 8..63) {
                updateLog("Error: The length of a passphrase must be between 8 and 63.")
                _eventFlow.emit(UiEvent.ShowToast("Password must be between 8 and 63 characters."))
                _isHotspotEnabled.value = false
                _isProcessing.value = false
                return@launch
            }

            val ssid = "DIRECT-hs-$ssidTrimmed"

            val band = when (selectedBand) {
                "2.4GHz" -> WifiP2pConfig.GROUP_OWNER_BAND_2GHZ
                "5GHz" -> WifiP2pConfig.GROUP_OWNER_BAND_5GHZ
                else -> WifiP2pConfig.GROUP_OWNER_BAND_AUTO
            }

            val config = WifiP2pConfig.Builder()
                .setNetworkName(ssid)
                .setPassphrase(passwordTrimmed)
                .enablePersistentMode(false)
                .setGroupOperatingBand(band)
                .build()

            try {
                wifiManager.createGroup(
                    channel,
                    config,
                    object : WifiP2pManager.ActionListener {
                        override fun onSuccess() {
                            viewModelScope.launch {
                                updateLog("Hotspot started successfully.")
                                _isHotspotEnabled.value = true
                                updateLog("------------------- Hotspot Info -------------------")
                                updateLog("SSID: $ssidTrimmed")
                                updateLog("Password: $passwordTrimmed")
                                val bandStr = when (band) {
                                    WifiP2pConfig.GROUP_OWNER_BAND_2GHZ -> "2.4GHz"
                                    WifiP2pConfig.GROUP_OWNER_BAND_5GHZ -> "5GHz"
                                    else -> "Auto"
                                }
                                updateLog("Band: $bandStr")
                                updateLog("---------------------------------------------------")
                                _isProcessing.value = false
                                _eventFlow.emit(UiEvent.ShowToast("Hotspot started successfully."))

                                // Start VPN
                                vpnRepository.startVpn()
//                                startVpn()

                                // Start SSH Server
                                startSSHServer()

                                // Acquire Wi-Fi Lock if enabled
                                acquireWifiLock()

                                // Show Hotspot Status Notification
                                showHotspotStatusNotification()

                                // Start Idle Monitoring
                                startIdleMonitoring()

                                // Start Data Usage Tracking
                                startDataUsageTracking()

                                // Start Battery Monitoring
                                startBatteryMonitoring()

                                // Start Network Monitoring
                                startNetworkMonitoring()
                            }
                        }

                        override fun onFailure(reason: Int) {
                            val reasonStr = when (reason) {
                                WifiP2pManager.ERROR -> "General error"
                                WifiP2pManager.P2P_UNSUPPORTED -> "P2P Unsupported"
                                WifiP2pManager.BUSY -> "System is busy"
                                else -> "Unknown error"
                            }
                            viewModelScope.launch {
                                updateLog("Failed to start hotspot. Reason: $reasonStr")
                                _isHotspotEnabled.value = false
                                _isProcessing.value = false
                                _eventFlow.emit(UiEvent.ShowToast("Failed to start hotspot: $reasonStr"))
                            }
                        }
                    })
            } catch (e: Exception) {
                viewModelScope.launch {
                    updateLog("Exception: ${e.message}")
                    _isHotspotEnabled.value = false
                    _isProcessing.value = false
                    _eventFlow.emit(UiEvent.ShowToast("Exception occurred: ${e.message}"))
                }
            }
        }
    }

    // ----- Function to Stop the Hotspot -----
    fun onButtonStopTapped() {
        viewModelScope.launch {
            if (!_isHotspotEnabled.value) {
                updateLog("Error: Hotspot is not enabled.")
                _eventFlow.emit(UiEvent.ShowToast("Hotspot is not enabled."))
                return@launch
            }

            _isProcessing.value = true // Start processing

            try {
                suspendCancellableCoroutine<Unit> { cont ->
                    wifiManager.removeGroup(channel, object : WifiP2pManager.ActionListener {
                        override fun onSuccess() {
                            viewModelScope.launch {
                                updateLog("Hotspot stopped successfully.")
                                _isHotspotEnabled.value = false
                                _connectedDevices.value = emptyList()
                                _isProcessing.value = false
                                _eventFlow.emit(UiEvent.ShowToast("Hotspot stopped successfully."))

                                Log.d(TAG, "Wi-Fi Direct group removed successfully.")
                                cont.resume(Unit)

                                // Stop VPN
                                vpnRepository.stopVpn()
//                                stopVpn()

                                // Stop SSH Server
                                stopSSHServer()

                                // Release Wi-Fi Lock if acquired
                                releaseWifiLock()

                                // Remove Hotspot Status Notification
                                removeHotspotStatusNotification()

                                // Reset Idle Monitoring
                                _remainingIdleTime.value = 0L
                            }
                        }
                        override fun onFailure(reason: Int) {
                            val reasonStr = when (reason) {
                                WifiP2pManager.ERROR -> "General error"
                                WifiP2pManager.P2P_UNSUPPORTED -> "P2P Unsupported"
                                WifiP2pManager.BUSY -> "System is busy"
                                else -> "Unknown error"
                            }
                            viewModelScope.launch {
                                updateLog("Failed to stop hotspot. Reason: $reasonStr")
                                _isHotspotEnabled.value = true // Assuming it was enabled
                                _isProcessing.value = false
                                _eventFlow.emit(UiEvent.ShowToast("Failed to stop hotspot: $reasonStr"))
                            }
                        }
                    })
                }

            } catch (e: Exception) {
                viewModelScope.launch {
                    updateLog("Exception: ${e.message}")
                    _isHotspotEnabled.value = true // Assuming it was enabled
                    _isProcessing.value = false
                    _eventFlow.emit(UiEvent.ShowToast("Exception occurred: ${e.message}"))
                }
            }
        }
    }

    //*****************************************************Fine****************************************

    fun startNetworkMonitoring() {
        val connectivityManager =
            getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val linkDownstreamBandwidthKbps = networkCapabilities.linkDownstreamBandwidthKbps
                val linkUpstreamBandwidthKbps = networkCapabilities.linkUpstreamBandwidthKbps

                val quality = when {
                    linkDownstreamBandwidthKbps < 150 -> "Poor"
                    linkDownstreamBandwidthKbps < 550 -> "Moderate"
                    linkDownstreamBandwidthKbps < 2000 -> "Good"
                    else -> "Excellent"
                }
                _networkQuality.value = quality
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    fun startBatteryMonitoring() {
        val batteryManager =
            getApplication<Application>().getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = getApplication<Application>().registerReceiver(null, intentFilter)
        batteryStatus?.let {
            val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val batteryPct = level * 100 / scale.toFloat()
            _batteryLevel.value = batteryPct.toInt()
        }

        viewModelScope.launch {
            while (isHotspotEnabled.value) {
                delay(60000) // Check every minute
                val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                _batteryLevel.value = level

                if (level <= 15) {
                    withContext(Dispatchers.Main) {
                        _eventFlow.emit(UiEvent.ShowToast("Battery low ($level%). Consider turning off the hotspot to save power."))
                    }
                }
            }
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

    fun acquireWifiLock() {
        if (_wifiLockEnabled.value) {
            val wifiManager =
                getApplication<Application>().getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiLock = wifiManager.createWifiLock(
                WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                "WiFiP2PHotspotLock"
            )
            wifiLock?.acquire()
        }
    }

    fun releaseWifiLock() {
        wifiLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
    }

    // ---------- Idle Monitoring Logic ----------
    fun startIdleMonitoring() {
        viewModelScope.launch {
            var idleStartTime = System.currentTimeMillis()
            lastTotalBytes = 0L // Initialize lastTotalBytes
            while (isHotspotEnabled.value && autoShutdownEnabled.value) {
                delay(1000L) // Update every second
                val connectedDevices = _connectedDevices.value
                val (rxBytes, txBytes) = getSessionDataUsage()
                val currentTotalBytes = rxBytes + txBytes

                val dataUsageInInterval: Long
                if (lastTotalBytes == 0L) {
                    dataUsageInInterval = 0L // No previous data to compare
                } else {
                    dataUsageInInterval = currentTotalBytes - lastTotalBytes
                }
                lastTotalBytes = currentTotalBytes

                val dataUsagePerSecond = dataUsageInInterval / (1000L) // Since we delay for 1000ms

                _isIdle.value =
                    connectedDevices.isEmpty() || dataUsagePerSecond < idleThresholdBytesPerSecond

                if (_isIdle.value && _autoShutdownEnabled.value) {
                    val elapsedIdleTime = System.currentTimeMillis() - idleStartTime
                    val totalIdleTime = _idleTimeoutMinutes.value * 60 * 1000L
                    _remainingIdleTime.value = totalIdleTime - elapsedIdleTime

                    if (_remainingIdleTime.value <= 0L) {
                        // Idle time exceeded, turn off hotspot
                        withContext(Dispatchers.Main) {
                            onButtonStopTapped()
                            _eventFlow.emit(UiEvent.ShowToast("Hotspot turned off due to inactivity"))
                        }
                        break
                    }
                } else {
                    idleStartTime = System.currentTimeMillis()
                    _remainingIdleTime.value = _idleTimeoutMinutes.value * 60 * 1000L
                }
            }
            // Reset remaining idle time when monitoring stops
            _remainingIdleTime.value = 0L
        }
    }

    // Function to get session data usage (implementation depends on previous steps)
    fun getSessionDataUsage(): Pair<Long, Long> {
        val rxBytes = TrafficStats.getTotalRxBytes()
        val txBytes = TrafficStats.getTotalTxBytes()
        return Pair(rxBytes, txBytes)
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

    // Function to send notifications
    fun sendDeviceConnectionNotification(deviceName: String) {
        viewModelScope.launch {
            val notification: Notification =
                NotificationCompat.Builder(getApplication(), "device_connection_channel")
                    .setSmallIcon(R.drawable.ic_notification) // Ensure you have a valid icon
                    .setContentTitle("Device Connected")
                    .setContentText("$deviceName has connected to the hotspot.")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build()

            notificationManager.notify(1185509172, notification)
        }
    }


    private fun sendDeviceConnectionNotification(device: WifiP2pDevice, connected: Boolean) {
        val notificationManager =
            getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = device.deviceAddress.hashCode()

        val contentText = if (connected) {
            "Device connected: ${device.deviceName}"
        } else {
            "Device disconnected: ${device.deviceName}"
        }

        val notification =
            NotificationCompat.Builder(getApplication<Application>(), "device_connection_channel")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Device Connection")
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

        notificationManager.notify(notificationId, notification)
    }

    fun showHotspotStatusNotification() {
        val notificationManager =
            getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = 1000 // Unique ID for the hotspot status notification

        val notification = NotificationCompat.Builder(getApplication<Application>(), "tether_guard")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Title")
            .setContentText("Content")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .apply {
                if (!notificationSoundEnabled.value) {
                    setSound(null)
                }
                if (!notificationVibrationEnabled.value) {
                    setVibrate(null)
                }
            }
            .build()
    }

    fun removeHotspotStatusNotification() {
        val notificationManager =
            getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = 1000
        notificationManager.cancel(notificationId)
    }


    fun startDataUsageTracking() {
        sessionStartRxBytes = TrafficStats.getTotalRxBytes()
        sessionStartTxBytes = TrafficStats.getTotalTxBytes()
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
        // Wi-Fi P2P does not provide a direct way to disconnect a single device.
        // However, if you are the group owner, you can remove the group to disconnect all devices
        // and then reform the group. Alternatively, you can attempt to cancel the connection.

        // For illustration purposes, we'll attempt to remove the group.
        wifiManager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                updateLog("Disconnected device: ${deviceInfo.device.deviceName}")
                // Remove the device from the connected devices list
                _connectedDeviceInfos.value = _connectedDeviceInfos.value.filterNot {
                    it.device.deviceAddress == deviceInfo.device.deviceAddress
                }
                // Optionally, reinitialize the group to allow other devices to reconnect
                // This is a workaround due to API limitations
//                initializeGroup()
            }

            override fun onFailure(reason: Int) {
                updateLog("Failed to disconnect device: ${deviceInfo.device.deviceName}")
            }
        })
    }
    // Function to initialize the group again after removing it
    private fun initializeGroup() {
        wifiManager.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                updateLog("Group reinitialized after device disconnection.")
            }

            override fun onFailure(reason: Int) {
                updateLog("Failed to reinitialize group after device disconnection.")
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

    fun updateNotificationEnabled(enabled: Boolean) {
        _notificationEnabled.value = enabled
        // Save to DataStore
    }

    fun updateNotificationSoundEnabled(enabled: Boolean) {
        _notificationSoundEnabled.value = enabled
        // Save to DataStore
    }

    fun updateNotificationVibrationEnabled(enabled: Boolean) {
        _notificationVibrationEnabled.value = enabled
        // Save to DataStore
    }

    fun updateTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
        // Save to DataStore if needed
    }

    fun attemptReconnection() {
        viewModelScope.launch {
            var attempts = 0
            val maxAttempts = 5
            val delayBetweenAttempts = 5000L // 5 seconds

            while (attempts < maxAttempts && !isConnected()) {
                attempts++
                connectToGroup()
                delay(delayBetweenAttempts)
            }
            if (!isConnected()) {
                _eventFlow.emit(UiEvent.ShowToast("Failed to reconnect after $attempts attempts."))
            }
        }
    }
    private fun isConnected(): Boolean {
        // Implement logic to check if the hotspot is connected
        return isHotspotEnabled.value && _connectedDevices.value.isNotEmpty()
    }
    private fun connectToGroup() {
        initializeGroup()
    }
}
