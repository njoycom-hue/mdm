package com.safecircle.app.sync

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.safecircle.app.network.dto.KeywordAlertDto
import java.util.concurrent.TimeUnit

/**
 * 이벤트를 즉시 서버로 보내지 않고 로컬에 쌓아 UploadWorker가 배치로 업로드한다.
 * 키워드 매치처럼 긴급한 이벤트는 즉시 업로드용 큐에 별도로 표시한다.
 */
class EventQueue(private val context: Context) {

    fun enqueueKeywordAlert(sourceApp: String, matchedKeywords: List<String>) {
        val alert = KeywordAlertDto(
            sourceApp = sourceApp,
            matchedKeywords = matchedKeywords,
            occurredAtEpochMs = System.currentTimeMillis()
        )
        // TODO: Room DB에 alert 저장 (urgent = true)
        UploadWorker.enqueueImmediate(context)
    }

    fun enqueueUsageSnapshot(/* TODO: UsageStatsSnapshot */) {
        // TODO: Room DB에 배치 저장, PeriodicWorkRequest가 5~15분마다 처리
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
