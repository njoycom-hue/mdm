package com.safecircle.backend.db

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp

/** 보호자/피감독자 공용 계정 테이블. role로 구분한다. */
object Users : UUIDTable("users") {
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val role = varchar("role", 16) // GUARDIAN | WARD
    val createdAt = timestamp("created_at")
}

/** 보호자-피감독자 페어링. 피감독자가 발급받은 코드를 보호자가 입력해 연결한다. */
object Pairings : UUIDTable("pairings") {
    val guardianId = reference("guardian_id", Users)
    val wardId = reference("ward_id", Users)
    val createdAt = timestamp("created_at")
}

/** 피감독자 기기별 동의 기록 (통신비밀보호법 대응, 삭제 금지). */
object ConsentEvents : UUIDTable("consent_events") {
    val wardId = reference("ward_id", Users)
    val deviceId = varchar("device_id", 128)
    val appVersion = varchar("app_version", 32)
    val consentedAt = timestamp("consented_at")
    val revokedAt = timestamp("revoked_at").nullable()
}

/** 배치 업로드되는 앱 사용시간. */
object AppUsageEvents : UUIDTable("app_usage_events") {
    val wardId = reference("ward_id", Users)
    val packageName = varchar("package_name", 255)
    val appLabel = varchar("app_label", 255).default("")
    val foregroundMillis = long("foreground_millis")
    val lastUsedAt = timestamp("last_used_at")
    val receivedAt = timestamp("received_at")
}

/** 키워드 매치 알림. 원문은 저장하지 않고 매치된 키워드만 저장한다. */
object KeywordAlerts : UUIDTable("keyword_alerts") {
    val wardId = reference("ward_id", Users)
    val sourceApp = varchar("source_app", 255)
    val matchedKeywords = varchar("matched_keywords", 512) // comma-joined
    val occurredAt = timestamp("occurred_at")
}

/** 통화 이벤트 (번호+시간만, 내용 없음). */
object CallEvents : UUIDTable("call_events") {
    val wardId = reference("ward_id", Users)
    val direction = varchar("direction", 16)
    val counterpartNumber = varchar("counterpart_number", 32).nullable()
    val startedAt = timestamp("started_at")
    val durationSeconds = long("duration_seconds").nullable()
}

/** 피감독자가 발급한 1회용 페어링 코드. TTL 만료 또는 claimed 이후 재사용 불가. */
object PairingCodes : UUIDTable("pairing_codes") {
    val code = varchar("code", 6).uniqueIndex()
    val wardId = reference("ward_id", Users)
    val expiresAt = timestamp("expires_at")
    val claimedAt = timestamp("claimed_at").nullable()
}

/** 사용자별 FCM 토큰. 한 사용자가 여러 기기를 쓸 수 있어 (userId, token) 복합 유니크. */
object DeviceTokens : UUIDTable("device_tokens") {
    val userId = reference("user_id", Users)
    val fcmToken = varchar("fcm_token", 512)
    val updatedAt = timestamp("updated_at")

    init {
        uniqueIndex(userId, fcmToken)
    }
}

/**
 * 보호자가 설정하는 피감독자별 감시 정책. profileType에 따라 안드로이드 앱이 어떤 기능을
 * 활성화할지 스스로 판단한다 (아동=유해사이트차단+시간제한, 중독자=차단+실행알림+신규설치알림,
 * 부모님=위험키워드감지+무활동감지). watchedPackagesCsv는 blockedPackagesCsv와 달리
 * "차단"이 아니라 "실행 시 보호자에게 알림만" 가는 목록이다 (중독자 프로필용).
 */
object WardSettings : UUIDTable("ward_settings") {
    val wardId = reference("ward_id", Users).uniqueIndex()
    val profileType = varchar("profile_type", 16).default("ADDICT") // CHILD | ADDICT | ELDERLY
    val keywordsCsv = text("keywords_csv").default("")
    val blockedPackagesCsv = text("blocked_packages_csv").default("")
    val blockedDomainsCsv = text("blocked_domains_csv").default("")
    val watchedPackagesCsv = text("watched_packages_csv").default("")
    val updatedAt = timestamp("updated_at")
}

/** 보호자가 앱별로 설정하는 하루 사용시간 제한(분). 초과 시 접근성 서비스가 차단한다. (아동 프로필용) */
object AppTimeLimits : UUIDTable("app_time_limits") {
    val wardId = reference("ward_id", Users)
    val packageName = varchar("package_name", 255)
    val appLabel = varchar("app_label", 255).default("")
    val dailyLimitMinutes = integer("daily_limit_minutes")

    init {
        uniqueIndex(wardId, packageName)
    }
}

/**
 * 즉시(배치 아님) 보호자에게 푸시되는 활동 알림 로그. 관리자 권한 해제 시도, 감시 대상 앱 실행,
 * 신규 앱 설치, 장시간 무활동 등 profileType별로 서로 다른 종류의 이벤트가 여기 쌓인다.
 */
object ActivityAlerts : UUIDTable("activity_alerts") {
    val wardId = reference("ward_id", Users)
    val type = varchar("type", 64) // DEVICE_ADMIN_DISABLE_REQUESTED | WATCHED_APP_LAUNCHED | APP_INSTALLED | INACTIVITY_DETECTED
    val detail = varchar("detail", 255).default("")
    val occurredAt = timestamp("occurred_at")
}
