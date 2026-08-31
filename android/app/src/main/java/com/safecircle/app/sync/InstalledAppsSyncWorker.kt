package com.safecircle.app.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.safecircle.app.apps.InstalledAppsRepository
import com.safecircle.app.network.ApiClient
import java.util.concurrent.TimeUnit

/**
 * 피감독자 기기의 설치 앱 목록을 서버와 동기화한다. 보호자가 정책 편집 화면에서
 * 패키지명을 직접 몰라도 실제 목록에서 차단/감시/시간제한 대상을 고를 수 있게 하는
 * 것이 목적이라, 사용시간 데이터처럼 쌓이길 기다릴 필요 없이 페어링 직후 즉시(그리고
 * 앱 설치/삭제 시, 주기적으로) 동기화한다.
 */
class InstalledAppsSyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val apps = InstalledAppsRepository(applicationContext).listLaunchableApps()
            ApiClient.get(applicationContext).service.syncInstalledApps(apps)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        fun schedulePeriodicSync(context: Context) {
            val request = PeriodicWorkRequestBuilder<InstalledAppsSyncWorker>(12, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "safecircle_installed_apps_sync",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun enqueueImmediate(context: Context) {
            WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<InstalledAppsSyncWorker>().build())
        }
    }
}
