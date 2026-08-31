package com.safecircle.app.sync

import android.content.Context
import com.safecircle.app.network.ApiClient
import com.safecircle.app.network.dto.ActivityEventRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 관리자 권한 해제 시도, 감시 대상 앱 실행, 신규 앱 설치 같은 즉시성 이벤트를 배치 큐를
 * 거치지 않고 바로 서버로 보낸다 (보호자에게 지금 바로 알림이 가야 의미가 있는 신호들).
 * BroadcastReceiver/AccessibilityService처럼 suspend 컨텍스트가 아닌 곳에서 쓰기 위한
 * fire-and-forget 헬퍼 — 실패해도 재시도하지 않는다(다음 발생 시 다시 시도되는 성격의 이벤트들).
 */
object ActivityReporter {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun report(context: Context, type: String, detail: String = "") {
        val appContext = context.applicationContext
        scope.launch {
            runCatching { ApiClient.get(appContext).service.reportActivityEvent(ActivityEventRequest(type, detail)) }
        }
    }
}
