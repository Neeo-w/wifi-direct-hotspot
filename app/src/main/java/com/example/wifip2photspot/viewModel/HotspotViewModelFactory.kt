// HotspotViewModelFactory.kt
package com.example.wifip2photspot.viewModel

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.datastore.preferences.core.Preferences
import com.example.wifip2photspot.VPN.VpnRepository

class HotspotViewModelFactory(
    private val application: Application,
    private val dataStore: DataStore<Preferences>,
    private val vpnRepository: VpnRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HotspotViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HotspotViewModel(application, dataStore, vpnRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}



//
//
//class HotspotViewModelFactory(
//    private val application: Application,
//    private val dataStore: DataStore<Preferences>
//) : ViewModelProvider.Factory {
//    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
//        if (modelClass.isAssignableFrom(HotspotViewModel::class.java)) {
//            @Suppress("UNCHECKED_CAST")
//            return HotspotViewModel(application, dataStore) as T
//        }
//        throw IllegalArgumentException("Unknown ViewModel class")
//    }
//}
