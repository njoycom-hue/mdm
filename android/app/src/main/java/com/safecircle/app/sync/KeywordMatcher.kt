package com.safecircle.app.sync

import android.content.Context

/**
 * 보호자가 서버 대시보드에서 설정한 키워드 목록(대출, 도박 관련 등)을 로컬 캐시에서 읽어 매칭한다.
 * 목록 자체는 주기적으로 서버와 동기화하며, 매칭 대상 원문은 절대 보관하지 않는다.
 */
class KeywordMatcher(private val context: Context) {

    fun findMatches(text: String): List<String> {
        val keywords = loadCachedKeywords()
        if (keywords.isEmpty()) return emptyList()
        val lower = text.lowercase()
        return keywords.filter { lower.contains(it.lowercase()) }
    }

    private fun loadCachedKeywords(): List<String> {
        val prefs = context.getSharedPreferences("keyword_cache", Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_LIST, null) ?: return DEFAULT_KEYWORDS
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    companion object {
        private const val KEY_LIST = "keywords_csv"
        // 서버 동기화 전 안전한 기본값 (도박/대출 관련 대표 키워드)
        private val DEFAULT_KEYWORDS = listOf("대출", "베팅", "환전", "카지노", "토토")
    }
}
