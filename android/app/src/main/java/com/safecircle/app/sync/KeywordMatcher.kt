package com.safecircle.app.sync

import android.content.Context
import com.safecircle.app.settings.PolicyRepository

class KeywordMatcher(context: Context) {
    private val policyRepository = PolicyRepository(context)

    fun findMatches(text: String): List<String> {
        val keywords = policyRepository.keywords()
        if (keywords.isEmpty()) return emptyList()
        val lower = text.lowercase()
        return keywords.filter { lower.contains(it.lowercase()) }
    }
}
