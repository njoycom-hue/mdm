package com.safecircle.app.network.dto

data class RegisterRequest(val email: String, val password: String, val role: String)
data class LoginRequest(val email: String, val password: String)
data class AuthResponse(val token: String, val userId: String, val role: String)

data class ConsentEventRequest(val deviceId: String, val appVersion: String)

data class PairingCodeResponse(val code: String, val expiresAtEpochMs: Long)
data class ClaimPairingRequest(val pairingCode: String)

data class RegisterFcmTokenRequest(val fcmToken: String)

data class WardSettingsResponse(
    val keywords: List<String>,
    val blockedPackages: List<String>,
    val blockedDomains: List<String>
)

data class WardSettingsRequest(
    val keywords: List<String>,
    val blockedPackages: List<String>,
    val blockedDomains: List<String>
)

data class AppUsageDto(val packageName: String, val foregroundMillis: Long, val lastUsedEpochMs: Long)
data class KeywordAlertDto(val sourceApp: String, val matchedKeywords: List<String>, val occurredAtEpochMs: Long)
data class CallEventDto(
    val direction: String,
    val counterpartNumber: String?,
    val startedAtEpochMs: Long,
    val durationSeconds: Long?
)

data class BatchUploadRequest(
    val usage: List<AppUsageDto> = emptyList(),
    val keywordAlerts: List<KeywordAlertDto> = emptyList(),
    val callEvents: List<CallEventDto> = emptyList()
)
