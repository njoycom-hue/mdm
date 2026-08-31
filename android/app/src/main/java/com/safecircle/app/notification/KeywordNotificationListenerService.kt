package com.safecircle.app.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.safecircle.app.sync.EventQueue
import com.safecircle.app.sync.KeywordMatcher

/**
 * 은행/대출/도박 앱 알림의 텍스트만 훑어 키워드 매치 여부를 판단한다.
 * 알림 원문은 저장하지 않고 매치 결과만 큐에 적재한다 (LEGAL.md 2번 원칙).
 */
class KeywordNotificationListenerService : NotificationListenerService() {

    private lateinit var keywordMatcher: KeywordMatcher
    private lateinit var eventQueue: EventQueue

    override fun onCreate() {
        super.onCreate()
        keywordMatcher = KeywordMatcher(applicationContext)
        eventQueue = EventQueue(applicationContext)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val text = buildString {
            append(extras.getCharSequence("android.title") ?: "")
            append(" ")
            append(extras.getCharSequence("android.text") ?: "")
        }
        val matched = keywordMatcher.findMatches(text)
        if (matched.isNotEmpty()) {
            eventQueue.enqueueKeywordAlert(sourceApp = sbn.packageName, matchedKeywords = matched)
        }
    }
}
