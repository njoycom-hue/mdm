package com.safecircle.app.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.safecircle.app.data.local.AppDatabase
import com.safecircle.app.network.ApiClient
import com.safecircle.app.network.dto.AppUsageDto
import com.safecircle.app.network.dto.BatchUploadRequest
import com.safecircle.app.network.dto.CallEventDto
import com.safecircle.app.network.dto.KeywordAlertDto

/** 로컬 큐(Room)에 쌓인 이벤트를 배치로 서버에 업로드하고, 성공한 만큼만 큐에서 제거한다. */
class UploadWorker(private val appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val dao = AppDatabase.get(appContext).pendingEventDao()

        val usage = dao.peekUsage()
        val alerts = dao.peekKeywordAlerts()
        val calls = dao.peekCallEvents()

        if (usage.isEmpty() && alerts.isEmpty() && calls.isEmpty()) return Result.success()

        return try {
            val request = BatchUploadRequest(
                usage = usage.map { AppUsageDto(it.packageName, it.foregroundMillis, it.lastUsedEpochMs) },
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

    companion object {
        fun enqueueImmediate(context: Context) {
            val request = OneTimeWorkRequestBuilder<UploadWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
