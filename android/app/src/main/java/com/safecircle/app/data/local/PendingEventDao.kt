package com.safecircle.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.safecircle.app.data.local.entities.PendingCallEvent
import com.safecircle.app.data.local.entities.PendingKeywordAlert
import com.safecircle.app.data.local.entities.PendingUsageEvent

/** UploadWorker가 배치로 읽어가는 로컬 큐. 업로드 성공 시 해당 id들만 삭제한다. */
@Dao
interface PendingEventDao {

    @Insert
    suspend fun insertUsage(event: PendingUsageEvent)

    @Insert
    suspend fun insertKeywordAlert(alert: PendingKeywordAlert)

    @Insert
    suspend fun insertCallEvent(event: PendingCallEvent)

    @Query("SELECT * FROM pending_usage_events ORDER BY id LIMIT :limit")
    suspend fun peekUsage(limit: Int = 500): List<PendingUsageEvent>

    @Query("SELECT * FROM pending_keyword_alerts ORDER BY id LIMIT :limit")
    suspend fun peekKeywordAlerts(limit: Int = 500): List<PendingKeywordAlert>

    @Query("SELECT * FROM pending_call_events ORDER BY id LIMIT :limit")
    suspend fun peekCallEvents(limit: Int = 500): List<PendingCallEvent>

    @Query("DELETE FROM pending_usage_events WHERE id IN (:ids)")
    suspend fun deleteUsage(ids: List<Long>)

    @Query("DELETE FROM pending_keyword_alerts WHERE id IN (:ids)")
    suspend fun deleteKeywordAlerts(ids: List<Long>)

    @Query("DELETE FROM pending_call_events WHERE id IN (:ids)")
    suspend fun deleteCallEvents(ids: List<Long>)
}
