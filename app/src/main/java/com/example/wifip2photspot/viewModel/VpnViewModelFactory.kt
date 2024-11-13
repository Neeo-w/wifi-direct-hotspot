package com.example.wifip2photspot.viewModel

// VpnViewModelFactory.kt

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.example.wifip2photspot.VPN.VpnRepository

class VpnViewModelFactory(
    private val application: Application,
    private val dataStore: DataStore<Preferences>,
    private val vpnRepository: VpnRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VpnViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VpnViewModel(application, dataStore,vpnRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}