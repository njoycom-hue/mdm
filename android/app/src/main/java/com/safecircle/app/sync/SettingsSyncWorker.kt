package com.safecircle.app.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.safecircle.app.network.ApiClient
import com.safecircle.app.settings.PolicyRepository
import java.util.concurrent.TimeUnit

/** 보호자가 설정한 키워드/차단앱/차단도메인 정책을 주기적으로 내려받아 로컬 캐시를 갱신한다. */
class SettingsSyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val settings = ApiClient.get(applicationContext).service.myWardSettings()
            PolicyRepository(applicationContext).update(
                profileType = settings.profileType,
                keywords = settings.keywords,
                blockedPackages = settings.blockedPackages,
                blockedDomains = settings.blockedDomains,
                watchedPackages = settings.watchedPackages,
                appTimeLimits = settings.appTimeLimits
            )
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        fun schedulePeriodicSync(context: Context) {
            val request = PeriodicWorkRequestBuilder<SettingsSyncWorker>(30, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "safecircle_settings_sync",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
