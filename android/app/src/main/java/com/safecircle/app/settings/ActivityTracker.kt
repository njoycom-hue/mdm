package com.safecircle.app.settings

import android.content.Context

/**
 * 마지막으로 기기 사용(접근성 이벤트)이 감지된 시각을 기록한다. 별도 생체/동작 센서 없이,
 * "화면에서 일어나는 모든 조작"을 활동 신호로 대신 쓴다 — 부모님 프로필의 무활동 감지가
 * InactivityCheckWorker에서 이 값을 읽어 판단한다.
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

    companion object {
        private const val KEY_LAST_ACTIVITY_AT = "last_activity_at"
        private const val KEY_LAST_ALERTED_AT = "last_alerted_at"
    }
}
