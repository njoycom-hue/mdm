package com.safecircle.app.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.safecircle.app.data.local.AppDatabase
import com.safecircle.app.data.local.PendingEventDao
import com.safecircle.app.data.local.entities.PendingUsageEvent
import com.safecircle.app.network.ApiClient
import com.safecircle.app.network.dto.AppUsageDto
import com.safecircle.app.network.dto.BatchUploadRequest
import com.safecircle.app.network.dto.CallEventDto
import com.safecircle.app.network.dto.KeywordAlertDto
import com.safecircle.app.usage.UsageStatsRepository
import java.util.concurrent.TimeUnit

/** 로컬 큐(Room)에 쌓인 이벤트를 배치로 서버에 업로드하고, 성공한 만큼만 큐에서 제거한다. */
class UploadWorker(private val appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val dao = AppDatabase.get(appContext).pendingEventDao()

        collectUsageSinceLastRun(dao)

        val usage = dao.peekUsage()
        val alerts = dao.peekKeywordAlerts()
        val calls = dao.peekCallEvents()

        if (usage.isEmpty() && alerts.isEmpty() && calls.isEmpty()) return Result.success()

        return try {
            val request = BatchUploadRequest(
                usage = usage.map { AppUsageDto(it.packageName, it.appLabel, it.foregroundMillis, it.lastUsedEpochMs) },
                keywordAlerts = alerts.map { KeywordAlertDto(it.sourceApp, it.matchedKeywordsCsv.split(","), it.occurredAtEpochMs) },
                callEvents = calls.map { CallEventDto(it.direction, it.counterpartNumber, it.startedAtEpochMs, it.durationSeconds) }
            )
            ApiClient.get(appContext).service.uploadEvents(request)

            dao.deleteUsage(usage.map { it.id })
            dao.deleteKeywordAlerts(alerts.map { it.id })
            dao.deleteCallEvents(calls.map { it.id })
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    /**
     * UsageStatsRepository는 그동안 어디서도 호출되지 않아 사용시간이 서버로 전혀 올라가지
     * 않고 있었다(보호자 화면의 "최근 24시간 사용시간"이 항상 비어 보인 원인). 이미 15분마다
     * 도는 이 워커에 얹어, 마지막으로 수집한 시각 이후 구간만 조회해 중복 합산 없이 누적한다.
     */
    private suspend fun collectUsageSinceLastRun(dao: PendingEventDao) {
        val prefs = appContext.getSharedPreferences("usage_collection", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val lastCollectedAt = prefs.getLong(KEY_LAST_COLLECTED_AT, now - TimeUnit.HOURS.toMillis(24))
        if (now <= lastCollectedAt) return

        val collected = UsageStatsRepository(appContext).collectSince(lastCollectedAt)
        collected.forEach { usage ->
            dao.insertUsage(
                PendingUsageEvent(
                    packageName = usage.packageName,
                    appLabel = usage.appLabel,
                    foregroundMillis = usage.foregroundMillis,
                    lastUsedEpochMs = usage.lastUsedEpochMs
                )
            )
        }
        prefs.edit().putLong(KEY_LAST_COLLECTED_AT, now).apply()
    }

    companion object {
        private const val KEY_LAST_COLLECTED_AT = "last_collected_at"

        fun enqueueImmediate(context: Context) {
            val request = OneTimeWorkRequestBuilder<UploadWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
