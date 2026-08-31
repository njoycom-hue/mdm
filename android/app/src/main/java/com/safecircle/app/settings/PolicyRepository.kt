package com.safecircle.app.settings

import android.content.Context

/**
 * 보호자가 서버에서 설정한 감시 정책(키워드, 차단 앱, 차단 도메인)의 로컬 캐시.
 * SettingsSyncWorker가 주기적으로 서버와 동기화하고, Accessibility/VPN/Notification 서비스가 여기서 읽는다.
 */
class PolicyRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("policy_cache", Context.MODE_PRIVATE)

    fun keywords(): List<String> = csvOrDefault(KEY_KEYWORDS, DEFAULT_KEYWORDS)
    fun blockedPackages(): Set<String> = csvOrDefault(KEY_BLOCKED_PACKAGES, emptyList()).toSet()
    fun blockedDomains(): Set<String> = csvOrDefault(KEY_BLOCKED_DOMAINS, emptyList()).toSet()

    fun update(keywords: List<String>, blockedPackages: List<String>, blockedDomains: List<String>) {
        prefs.edit()
            .putString(KEY_KEYWORDS, keywords.joinToString(","))
            .putString(KEY_BLOCKED_PACKAGES, blockedPackages.joinToString(","))
            .putString(KEY_BLOCKED_DOMAINS, blockedDomains.joinToString(","))
            .apply()
    }

    private fun csvOrDefault(key: String, default: List<String>): List<String> {
        val raw = prefs.getString(key, null) ?: return default
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    companion object {
        private const val KEY_KEYWORDS = "keywords_csv"
        private const val KEY_BLOCKED_PACKAGES = "blocked_packages_csv"
        private const val KEY_BLOCKED_DOMAINS = "blocked_domains_csv"
        // 서버 최초 동기화 전 안전한 기본값 (도박/대출 관련 대표 키워드)
        private val DEFAULT_KEYWORDS = listOf("대출", "베팅", "환전", "카지노", "토토")
    }
}
