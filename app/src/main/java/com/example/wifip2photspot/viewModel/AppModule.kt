package com.example.wifip2photspot.viewModel
//
//// AppModule.kt
//
//
//import android.app.NotificationManager
//import android.content.Context
//import android.net.wifi.p2p.WifiP2pManager
//import dagger.Module
//import dagger.Provides
//import dagger.hilt.InstallIn
//import dagger.hilt.components.SingletonComponent
//import javax.inject.Singleton
//
//@androidx.test.espresso.core.internal.deps.dagger.Module
//@InstallIn(SingletonComponent::class)
//object AppModule {
//
//    @Provides
//    @Singleton
//    fun provideWifiP2pManager(application: Application): WifiP2pManager {
//        return application.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
//    }
//
//    @Provides
//    @Singleton
//    fun provideNotificationManager(application: Application): NotificationManager {
//        return application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//    }
//
//    @Provides
//    @Singleton
//    fun provideVpnRepository(application: Application): VpnRepository {
//        return VpnRepository(application)
//    }
//}
