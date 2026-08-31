package com.safecircle.app.onboarding

import android.content.Context

/** 로컬 동의 상태 저장. 서버에도 동일 이벤트를 타임스탬프와 함께 기록한다 (LEGAL.md 참고). */
class ConsentStore(context: Context) {
    private val prefs = context.getSharedPreferences("consent", Context.MODE_PRIVATE)

    fun hasConsented(): Boolean = prefs.getBoolean(KEY_CONSENTED, false)

    fun recordConsent() {
        prefs.edit()
            .putBoolean(KEY_CONSENTED, true)
            .putLong(KEY_CONSENTED_AT, System.currentTimeMillis())
            .apply()
    }

    fun revokeConsent() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_CONSENTED = "consented"
        private const val KEY_CONSENTED_AT = "consented_at"
    }
}
