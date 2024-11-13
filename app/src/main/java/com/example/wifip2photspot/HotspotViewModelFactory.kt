// HotspotViewModelFactory.kt
package com.example.wifip2photspot

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.datastore.preferences.core.Preferences

class HotspotViewModelFactory(
    private val application: Application,
    private val dataStore: DataStore<Preferences>
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HotspotViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HotspotViewModel(application, dataStore) as T
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
