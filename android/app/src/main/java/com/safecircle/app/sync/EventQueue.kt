package com.safecircle.app.sync

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.safecircle.app.data.local.AppDatabase
import com.safecircle.app.data.local.entities.PendingCallEvent
import com.safecircle.app.data.local.entities.PendingKeywordAlert
import com.safecircle.app.data.local.entities.PendingUsageEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * 이벤트를 즉시 서버로 보내지 않고 Room에 쌓아 UploadWorker가 배치로 업로드한다.
 * 키워드 매치처럼 긴급한 이벤트는 저장 직후 즉시 업로드를 트리거한다.
 */
class EventQueue(private val context: Context) {

    private val dao = AppDatabase.get(context).pendingEventDao()
    private val scope = CoroutineScope(Dispatchers.IO)

    fun enqueueKeywordAlert(sourceApp: String, matchedKeywords: List<String>) {
        scope.launch {
            dao.insertKeywordAlert(
                PendingKeywordAlert(
                    sourceApp = sourceApp,
                    matchedKeywordsCsv = matchedKeywords.joinToString(","),
                    occurredAtEpochMs = System.currentTimeMillis()
                )
            )
            UploadWorker.enqueueImmediate(context)
        }
    }

    fun enqueueUsageSnapshot(packageName: String, foregroundMillis: Long, lastUsedEpochMs: Long) {
        scope.launch {
            dao.insertUsage(PendingUsageEvent(packageName = packageName, foregroundMillis = foregroundMillis, lastUsedEpochMs = lastUsedEpochMs))
        }
    }

    fun enqueueCallEvent(direction: String, counterpartNumber: String?, startedAtEpochMs: Long, durationSeconds: Long?) {
        scope.launch {
            dao.insertCallEvent(
                PendingCallEvent(
                    direction = direction,
                    counterpartNumber = counterpartNumber,
                    startedAtEpochMs = startedAtEpochMs,
                    durationSeconds = durationSeconds
                )
            )
        }
    }

    companion object {
        fun schedulePeriodicUpload(context: Context) {
            val request = PeriodicWorkRequestBuilder<UploadWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "safecircle_periodic_upload",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
