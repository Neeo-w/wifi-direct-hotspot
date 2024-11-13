package com.example.wifip2photspot.viewModel

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wifip2photspot.VPN.VpnRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


class VpnViewModel(
    application: Application,
    private val dataStore: DataStore<Preferences>,
    private val vpnRepository: VpnRepository
) : AndroidViewModel(application) {

    // ----- DataStore Keys -----
    companion object {
        val VPN_ENABLED_KEY = booleanPreferencesKey("vpn_enabled")
    }

    // ----- StateFlows for UI State -----
    private val _isVpnActive = MutableStateFlow(false)
    val isVpnActive: StateFlow<Boolean> = _isVpnActive.asStateFlow()

    private val _vpnStatusMessage = MutableStateFlow<String>("")
    val vpnStatusMessage: StateFlow<String> = _vpnStatusMessage.asStateFlow()

    // ----- UI Events -----
    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow: SharedFlow<UiEvent> = _eventFlow.asSharedFlow()

    // ----- Sealed Class for UI Events -----
    sealed class UiEvent {
        data class ShowToast(val message: String) : UiEvent()
        object StartVpnService : UiEvent()
        object StopVpnService : UiEvent()
    }



//    init {
//        // Load VPN Enabled Preference from DataStore
//        viewModelScope.launch {
//            dataStore.data.collect { preferences ->
//                val vpnEnabled = preferences[VPN_ENABLED_KEY] ?: false
//                if (vpnEnabled && !_isVpnActive.value) {
//                    startVpn()
//                } else if (!vpnEnabled && _isVpnActive.value) {
//                    stopVpn()
//                }
//            }
//        }
//    }

    //    // ----- Functions to Manage VPN -----
//    fun toggleVpn(enabled: Boolean) {
//        viewModelScope.launch {
//            dataStore.edit { preferences ->
//                preferences[VPN_ENABLED_KEY] = enabled
//            }
//            if (enabled) {
//                startVpn()
//            } else {
//                stopVpn()
//            }
//        }
//    }
    // ----- Functions to Manage VPN -----
    fun toggleVpn(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                startVpn()
            } else {
                stopVpn()
            }
        }
    }

    private fun startVpn() {
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.StartVpnService)
            _isVpnActive.value = true
            _vpnStatusMessage.value = "VPN Started"
            _eventFlow.emit(UiEvent.ShowToast("VPN started successfully."))
        }
    }

    private fun stopVpn() {
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.StopVpnService)
            _isVpnActive.value = false
            _vpnStatusMessage.value = "VPN Stopped"
            _eventFlow.emit(UiEvent.ShowToast("VPN stopped successfully."))
        }
    }
}

