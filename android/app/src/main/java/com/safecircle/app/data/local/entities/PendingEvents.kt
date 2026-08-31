package com.safecircle.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_usage_events")
data class PendingUsageEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appLabel: String,
    val foregroundMillis: Long,
    val lastUsedEpochMs: Long
)

@Entity(tableName = "pending_keyword_alerts")
data class PendingKeywordAlert(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceApp: String,
    val matchedKeywordsCsv: String,
    val occurredAtEpochMs: Long
)

@Entity(tableName = "pending_call_events")
data class PendingCallEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val direction: String,
    val counterpartNumber: String?,
    val startedAtEpochMs: Long,
    val durationSeconds: Long?
)
