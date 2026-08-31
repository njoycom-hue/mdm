package com.safecircle.app.settings

import android.content.Context

/**
 * 부모님 프로필의 "무활동/생체 신호 없음" 감지에 쓰는 두 종류의 마지막 신호 시각을
 * 기록한다. 워치가 연동되어 있으면 생체 신호(심박수/걸음수) 기준을, 없으면 화면 조작
 * (접근성 이벤트) 기준을 InactivityCheckWorker가 선택해서 쓴다.
 */
class ActivityTracker(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("activity_tracker", Context.MODE_PRIVATE)

    fun recordActivityNow() {
        prefs.edit().putLong(KEY_LAST_ACTIVITY_AT, System.currentTimeMillis()).apply()
    }

    fun lastActivityAtEpochMs(): Long = prefs.getLong(KEY_LAST_ACTIVITY_AT, System.currentTimeMillis())

    /** 같은 무활동 구간에 대해 알림을 반복해서 보내지 않도록 마지막으로 알린 시각을 기록한다. */
    fun lastAlertedAtEpochMs(): Long = prefs.getLong(KEY_LAST_ALERTED_AT, 0L)

    fun recordAlertedNow() {
        prefs.edit().putLong(KEY_LAST_ALERTED_AT, System.currentTimeMillis()).apply()
    }

    fun recordBiometricSignalAt(epochMs: Long) {
        prefs.edit().putLong(KEY_LAST_BIOMETRIC_SIGNAL_AT, epochMs).apply()
    }

    fun lastBiometricSignalAtEpochMs(): Long = prefs.getLong(KEY_LAST_BIOMETRIC_SIGNAL_AT, System.currentTimeMillis())

    fun lastBiometricAlertedAtEpochMs(): Long = prefs.getLong(KEY_LAST_BIOMETRIC_ALERTED_AT, 0L)

    fun recordBiometricAlertedNow() {
        prefs.edit().putLong(KEY_LAST_BIOMETRIC_ALERTED_AT, System.currentTimeMillis()).apply()
    }

    companion object {
        private const val KEY_LAST_ACTIVITY_AT = "last_activity_at"
        private const val KEY_LAST_ALERTED_AT = "last_alerted_at"
        private const val KEY_LAST_BIOMETRIC_SIGNAL_AT = "last_biometric_signal_at"
        private const val KEY_LAST_BIOMETRIC_ALERTED_AT = "last_biometric_alerted_at"
    }
}
