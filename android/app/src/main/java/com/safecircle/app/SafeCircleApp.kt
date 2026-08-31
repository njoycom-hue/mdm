package com.safecircle.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class SafeCircleApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createMonitoringChannel()
    }

    private fun createMonitoringChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            MONITORING_CHANNEL_ID,
            getString(R.string.monitoring_notification_title),
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val MONITORING_CHANNEL_ID = "safecircle_monitoring"
    }
}
