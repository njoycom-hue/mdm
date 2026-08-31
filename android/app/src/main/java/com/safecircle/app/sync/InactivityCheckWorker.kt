package com.safecircle.app.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.safecircle.app.network.dto.PROFILE_ELDERLY
import com.safecircle.app.settings.ActivityTracker
import com.safecircle.app.settings.PolicyRepository
import java.util.concurrent.TimeUnit

/**
 * 부모님 프로필 전용: 별도 생체/동작 센서 없이, 일정 시간 이상 기기 조작(접근성 이벤트)이
 * 전혀 없으면 "무활동"으로 보고 보호자에게 알린다. 같은 무활동 구간에서 매 시간 반복
 * 알리지 않도록, 마지막 실제 활동 이후 아직 알리지 않은 경우에만 한 번 보낸다.
 */
class InactivityCheckWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val policy = PolicyRepository(applicationContext)
        if (policy.profileType() != PROFILE_ELDERLY) return Result.success()

        val tracker = ActivityTracker(applicationContext)
        val lastActivity = tracker.lastActivityAtEpochMs()
        val elapsed = System.currentTimeMillis() - lastActivity

        if (elapsed >= INACTIVITY_THRESHOLD_MS && tracker.lastAlertedAtEpochMs() < lastActivity) {
            ActivityReporter.report(applicationContext, "INACTIVITY_DETECTED")
            tracker.recordAlertedNow()
        }
        return Result.success()
    }

    companion object {
        private val INACTIVITY_THRESHOLD_MS = TimeUnit.HOURS.toMillis(12)

        fun schedulePeriodicCheck(context: Context) {
            val request = PeriodicWorkRequestBuilder<InactivityCheckWorker>(1, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "safecircle_inactivity_check",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
