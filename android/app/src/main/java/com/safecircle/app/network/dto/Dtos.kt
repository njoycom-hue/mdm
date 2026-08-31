package com.safecircle.app.network.dto

data class KeywordAlertDto(
    val sourceApp: String,
    val matchedKeywords: List<String>,
    val occurredAtEpochMs: Long
)

data class AppUsageDto(
    val packageName: String,
    val foregroundMillis: Long,
    val lastUsedEpochMs: Long
)

data class CallEventDto(
    val direction: String, // INCOMING | OUTGOING | MISSED
    val counterpartNumber: String?,
    val startedAtEpochMs: Long,
    val durationSeconds: Long?
)

data class ConsentEventDto(
    val deviceId: String,
    val consentedAtEpochMs: Long,
    val appVersion: String
)

data class BatchUploadRequest(
    val deviceId: String,
    val usage: List<AppUsageDto> = emptyList(),
    val keywordAlerts: List<KeywordAlertDto> = emptyList(),
    val callEvents: List<CallEventDto> = emptyList()
)
