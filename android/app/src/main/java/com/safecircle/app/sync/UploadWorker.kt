package com.safecircle.app.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.safecircle.app.network.ApiClient

/** 로컬 큐에 쌓인 이벤트/사용통계를 배치로 서버에 업로드한다. */
class UploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // TODO: Room DB에서 pending 이벤트 조회
            // TODO: ApiClient.service.uploadEvents(pendingEvents)
            // TODO: 성공 시 로컬 큐에서 제거
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
