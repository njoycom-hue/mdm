package com.safecircle.app.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.safecircle.app.health.HealthSignalRepository
import com.safecircle.app.health.HealthSignalResult
import com.safecircle.app.network.dto.PROFILE_ELDERLY
import com.safecircle.app.settings.ActivityTracker
import com.safecircle.app.settings.PolicyRepository
import java.util.concurrent.TimeUnit

/**
 * 부모님 프로필 전용. 워치가 Health Connect에 심박수/걸음수를 기록하고 있으면 그 신호를
 * 우선 쓰고("생체 신호 없음" 감지), 워치/Health Connect가 없는 기기에서는 화면 조작
 * (접근성 이벤트) 여부로 대신 판단한다("무활동" 감지). 어느 쪽이든 같은 무활동 구간에서
 * 매 시간 반복 알리지 않도록, 마지막 신호 이후 아직 알리지 않은 경우에만 한 번 보낸다.
 */
class InactivityCheckWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val policy = PolicyRepository(applicationContext)
        if (policy.profileType() != PROFILE_ELDERLY) return Result.success()

        val tracker = ActivityTracker(applicationContext)
        val healthRepo = HealthSignalRepository(applicationContext)

        when (val signal = healthRepo.mostRecentSignal(INACTIVITY_THRESHOLD_MS)) {
            is HealthSignalResult.Available -> checkBiometricSignal(tracker, signal)
            HealthSignalResult.NoSignalSource -> checkTouchActivity(tracker)
        }
        return Result.success()
    }

    private fun checkBiometricSignal(tracker: ActivityTracker, signal: HealthSignalResult.Available) {
        signal.lastSignalAtEpochMs?.let { tracker.recordBiometricSignalAt(it) }
        val lastSignal = tracker.lastBiometricSignalAtEpochMs()
        val elapsed = System.currentTimeMillis() - lastSignal
        if (elapsed >= INACTIVITY_THRESHOLD_MS && tracker.lastBiometricAlertedAtEpochMs() < lastSignal) {
            ActivityReporter.report(applicationContext, "BIOMETRIC_SIGNAL_MISSING")
            tracker.recordBiometricAlertedNow()
        }
    }

    private fun checkTouchActivity(tracker: ActivityTracker) {
        val lastActivity = tracker.lastActivityAtEpochMs()
        val elapsed = System.currentTimeMillis() - lastActivity
        if (elapsed >= INACTIVITY_THRESHOLD_MS && tracker.lastAlertedAtEpochMs() < lastActivity) {
            ActivityReporter.report(applicationContext, "INACTIVITY_DETECTED")
            tracker.recordAlertedNow()
        }
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
