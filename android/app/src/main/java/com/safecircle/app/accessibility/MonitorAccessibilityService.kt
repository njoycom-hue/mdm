package com.safecircle.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.app.usage.UsageStatsManager
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.safecircle.app.settings.ActivityTracker
import com.safecircle.app.settings.PolicyRepository
import com.safecircle.app.sync.ActivityReporter
import com.safecircle.app.sync.EventQueue
import com.safecircle.app.sync.KeywordMatcher
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * 앱 전환 감지(차단 대상 앱 실행 시 홈으로 이동, 감시 대상 앱 실행 시 알림, 시간제한 초과 시 차단)
 * + 화면 텍스트 키워드 매칭을 담당한다. 모든 이벤트 수신 자체를 "기기 활동"으로 기록해
 * 부모님 프로필의 무활동 감지(InactivityCheckWorker)가 사용한다.
 * READ_SMS/READ_CALL_LOG 권한을 쓰지 않고 화면에 보이는 텍스트만 훑는 이유는
 * docs/ARCHITECTURE.md의 정책 준수 원칙 참고.
 */
class MonitorAccessibilityService : AccessibilityService() {

    private lateinit var keywordMatcher: KeywordMatcher
    private lateinit var eventQueue: EventQueue
    private lateinit var policyRepository: PolicyRepository
    private lateinit var activityTracker: ActivityTracker
    private var lastBlockedPackage: String? = null
    private var lastWatchedPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        keywordMatcher = KeywordMatcher(applicationContext)
        eventQueue = EventQueue(applicationContext)
        policyRepository = PolicyRepository(applicationContext)
        activityTracker = ActivityTracker(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        activityTracker.recordActivityNow()
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
            lastWatchedPackage = null
            return
        }

        val hardBlocked = packageName in policyRepository.blockedPackages()
        val overLimit = !hardBlocked && isOverDailyLimit(packageName)
        if (hardBlocked || overLimit) {
            // 같은 앱을 계속 재시도하며 홈으로 튕기는 루프를 한 번만 알리도록 방지
            if (lastBlockedPackage != packageName) {
                val message = if (overLimit) "오늘 사용 시간 제한을 초과했습니다." else "SafeCircle에 의해 차단된 앱입니다."
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
            lastBlockedPackage = packageName
            performGlobalAction(GLOBAL_ACTION_HOME)
            return
        }
        lastBlockedPackage = null

        if (packageName in policyRepository.watchedPackages()) {
            if (lastWatchedPackage != packageName) {
                ActivityReporter.report(applicationContext, "WATCHED_APP_LAUNCHED", packageName)
            }
            lastWatchedPackage = packageName
        } else {
            lastWatchedPackage = null
        }
    }

    /** 아동 프로필: 오늘(자정 이후) 해당 앱의 누적 사용시간이 보호자가 정한 제한(분)을 넘었는지 확인한다. */
    private fun isOverDailyLimit(packageName: String): Boolean {
        val limitMinutes = policyRepository.appTimeLimits()[packageName] ?: return false
        if (limitMinutes <= 0) return false

        val manager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        val usedMillis = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startOfDay, now)
            .firstOrNull { it.packageName == packageName }?.totalTimeInForeground ?: 0L
        return usedMillis >= TimeUnit.MINUTES.toMillis(limitMinutes.toLong())
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
