package com.safecircle.app.settings

import android.content.Context
import com.safecircle.app.network.dto.AppTimeLimitDto
import com.safecircle.app.network.dto.PROFILE_ADDICT

/**
 * 보호자가 서버에서 설정한 감시 정책(프로필 종류, 키워드, 차단/감시 앱, 차단 도메인, 앱별 시간제한)의
 * 로컬 캐시. SettingsSyncWorker가 주기적으로 서버와 동기화하고, Accessibility/VPN/Notification
 * 서비스와 신규 설치·무활동 감지 로직이 여기서 읽는다.
 */
class PolicyRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("policy_cache", Context.MODE_PRIVATE)

    fun profileType(): String = prefs.getString(KEY_PROFILE_TYPE, PROFILE_ADDICT) ?: PROFILE_ADDICT
    fun keywords(): List<String> = csvOrDefault(KEY_KEYWORDS, DEFAULT_KEYWORDS)
    fun blockedPackages(): Set<String> = csvOrDefault(KEY_BLOCKED_PACKAGES, emptyList()).toSet()
    fun blockedDomains(): Set<String> = csvOrDefault(KEY_BLOCKED_DOMAINS, emptyList()).toSet()
    /** 차단이 아니라 "실행 시 보호자에게 알림만" 가는 목록 (중독자 프로필용). */
    fun watchedPackages(): Set<String> = csvOrDefault(KEY_WATCHED_PACKAGES, emptyList()).toSet()
    /** 패키지명 -> 하루 제한(분). (아동 프로필용) */
    fun appTimeLimits(): Map<String, Int> {
        val raw = prefs.getString(KEY_TIME_LIMITS, null) ?: return emptyMap()
        return raw.split(",").mapNotNull { entry ->
            val parts = entry.split(":")
            val minutes = parts.getOrNull(1)?.toIntOrNull()
            if (parts.size == 2 && minutes != null) parts[0] to minutes else null
        }.toMap()
    }

    fun update(
        profileType: String,
        keywords: List<String>,
        blockedPackages: List<String>,
        blockedDomains: List<String>,
        watchedPackages: List<String>,
        appTimeLimits: List<AppTimeLimitDto>
    ) {
        prefs.edit()
            .putString(KEY_PROFILE_TYPE, profileType)
            .putString(KEY_KEYWORDS, keywords.joinToString(","))
            .putString(KEY_BLOCKED_PACKAGES, blockedPackages.joinToString(","))
            .putString(KEY_BLOCKED_DOMAINS, blockedDomains.joinToString(","))
            .putString(KEY_WATCHED_PACKAGES, watchedPackages.joinToString(","))
            .putString(KEY_TIME_LIMITS, appTimeLimits.joinToString(",") { "${it.packageName}:${it.dailyLimitMinutes}" })
            .apply()
    }

    private fun csvOrDefault(key: String, default: List<String>): List<String> {
        val raw = prefs.getString(key, null) ?: return default
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    companion object {
        private const val KEY_PROFILE_TYPE = "profile_type"
        private const val KEY_KEYWORDS = "keywords_csv"
        private const val KEY_BLOCKED_PACKAGES = "blocked_packages_csv"
        private const val KEY_BLOCKED_DOMAINS = "blocked_domains_csv"
        private const val KEY_WATCHED_PACKAGES = "watched_packages_csv"
        private const val KEY_TIME_LIMITS = "time_limits_csv"
        // 서버 최초 동기화 전 안전한 기본값 (도박/대출 관련 대표 키워드)
        private val DEFAULT_KEYWORDS = listOf("대출", "베팅", "환전", "카지노", "토토")
    }
}
