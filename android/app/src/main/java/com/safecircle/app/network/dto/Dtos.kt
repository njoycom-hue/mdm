package com.safecircle.app.network.dto

data class RegisterRequest(val email: String, val password: String, val role: String)
data class LoginRequest(val email: String, val password: String)
data class AuthResponse(val token: String, val userId: String, val role: String)

data class ConsentEventRequest(val deviceId: String, val appVersion: String)

data class PairingCodeResponse(val code: String, val expiresAtEpochMs: Long)
data class ClaimPairingRequest(val pairingCode: String)

data class RegisterFcmTokenRequest(val fcmToken: String)

/** 아동=유해사이트차단+시간제한, 중독자=차단+실행알림+신규설치알림, 부모님=위험키워드감지+무활동감지 */
const val PROFILE_CHILD = "CHILD"
const val PROFILE_ADDICT = "ADDICT"
const val PROFILE_ELDERLY = "ELDERLY"

data class WardSettingsResponse(
    val profileType: String,
    val keywords: List<String>,
    val blockedPackages: List<String>,
    val blockedDomains: List<String>,
    val watchedPackages: List<String>,
    val appTimeLimits: List<AppTimeLimitDto>
)

data class WardSettingsRequest(
    val profileType: String,
    val keywords: List<String>,
    val blockedPackages: List<String>,
    val blockedDomains: List<String>,
    val watchedPackages: List<String>
)

/** WARD 로컬 동기화용(라벨 불필요). 보호자 편집 화면에서는 [AppTimeLimitSummary]를 쓴다. */
data class AppTimeLimitDto(val packageName: String, val dailyLimitMinutes: Int)

data class AppTimeLimitSummary(val packageName: String, val appLabel: String, val dailyLimitMinutes: Int)

data class ActivityEventRequest(val type: String, val detail: String = "")
data class ActivityAlertSummary(val type: String, val detail: String, val occurredAtEpochMs: Long)

/** WARD -> 서버 동기화, 서버 -> GUARDIAN 조회 양쪽에서 같은 모양을 쓴다. */
data class InstalledAppDto(val packageName: String, val appLabel: String)

data class AppUsageDto(
    val packageName: String,
    val appLabel: String,
    val foregroundMillis: Long,
    val lastUsedEpochMs: Long
)
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

data class WardSummary(val wardId: String, val email: String, val pairedAtEpochMs: Long)
data class KeywordAlertSummary(val sourceApp: String, val matchedKeywords: List<String>, val occurredAtEpochMs: Long)
data class AppUsageSummary(val packageName: String, val appLabel: String, val totalForegroundMillis: Long)
