package com.safecircle.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.safecircle.app.sync.EventQueue
import com.safecircle.app.sync.KeywordMatcher

/**
 * 앱 전환 감지(차단 대상 앱 실행 시 오버레이) + 화면 텍스트 키워드 매칭을 담당한다.
 * READ_SMS/READ_CALL_LOG 권한을 쓰지 않고 화면에 보이는 텍스트만 훑는 이유는
 * docs/ARCHITECTURE.md의 정책 준수 원칙 참고.
 */
class MonitorAccessibilityService : AccessibilityService() {

    private lateinit var keywordMatcher: KeywordMatcher
    private lateinit var eventQueue: EventQueue

    override fun onServiceConnected() {
        super.onServiceConnected()
        keywordMatcher = KeywordMatcher(applicationContext)
        eventQueue = EventQueue(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val packageName = event.packageName?.toString() ?: return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> handleForegroundApp(packageName)
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> handleScreenText(packageName, event)
        }
    }

    private fun handleForegroundApp(packageName: String) {
        // TODO: BlockList.isBlocked(packageName) 이면 performGlobalAction(GLOBAL_ACTION_HOME) 또는 차단 오버레이 표시
    }

    private fun handleScreenText(packageName: String, event: AccessibilityEvent) {
        val text = event.text?.joinToString(" ") ?: return
        val matched = keywordMatcher.findMatches(text)
        if (matched.isNotEmpty()) {
            // 원문은 저장하지 않고 매치된 키워드/앱/시각만 큐에 적재
            eventQueue.enqueueKeywordAlert(sourceApp = packageName, matchedKeywords = matched)
        }
    }

    override fun onInterrupt() {}
}
