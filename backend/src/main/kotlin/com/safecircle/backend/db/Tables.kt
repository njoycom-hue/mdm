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

/** 보호자가 설정하는 피감독자별 감시 정책 (키워드, 차단 앱/도메인). */
object WardSettings : UUIDTable("ward_settings") {
    val wardId = reference("ward_id", Users).uniqueIndex()
    val keywordsCsv = text("keywords_csv").default("")
    val blockedPackagesCsv = text("blocked_packages_csv").default("")
    val blockedDomainsCsv = text("blocked_domains_csv").default("")
    val updatedAt = timestamp("updated_at")
}
