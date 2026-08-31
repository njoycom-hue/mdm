package com.safecircle.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.safecircle.app.settings.PolicyRepository
import com.safecircle.app.sync.EventQueue
import com.safecircle.app.sync.KeywordMatcher

/**
 * 앱 전환 감지(차단 대상 앱 실행 시 홈으로 이동) + 화면 텍스트 키워드 매칭을 담당한다.
 * READ_SMS/READ_CALL_LOG 권한을 쓰지 않고 화면에 보이는 텍스트만 훑는 이유는
 * docs/ARCHITECTURE.md의 정책 준수 원칙 참고.
 */
class MonitorAccessibilityService : AccessibilityService() {

    private lateinit var keywordMatcher: KeywordMatcher
    private lateinit var eventQueue: EventQueue
    private lateinit var policyRepository: PolicyRepository
    private var lastBlockedPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        keywordMatcher = KeywordMatcher(applicationContext)
        eventQueue = EventQueue(applicationContext)
        policyRepository = PolicyRepository(applicationContext)
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
        if (packageName == this.packageName) {
            lastBlockedPackage = null
            return
        }
        if (packageName !in policyRepository.blockedPackages()) {
            lastBlockedPackage = null
            return
        }
        // 같은 앱을 계속 재시도하며 홈으로 튕기는 루프를 한 번만 알리도록 방지
        if (lastBlockedPackage != packageName) {
            Toast.makeText(this, "SafeCircle에 의해 차단된 앱입니다.", Toast.LENGTH_SHORT).show()
        }
        lastBlockedPackage = packageName
        performGlobalAction(GLOBAL_ACTION_HOME)
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
