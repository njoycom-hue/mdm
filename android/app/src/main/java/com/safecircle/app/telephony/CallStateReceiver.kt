package com.safecircle.app.telephony

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.safecircle.app.sync.EventQueue

/**
 * READ_CALL_LOG 없이 통화 상태 브로드캐스트로 발신/수신 번호와 시각만 캡처한다.
 * 통화 내용은 녹음/열람하지 않는다.
 */
class CallStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
        val eventQueue = EventQueue(context.applicationContext)

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                ringingStartedAt = System.currentTimeMillis()
                ringingNumber = number
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                if (callStartedAt == null) callStartedAt = System.currentTimeMillis()
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                val startedAt = callStartedAt ?: ringingStartedAt
                if (startedAt != null) {
                    val durationSeconds = if (callStartedAt != null) {
                        (System.currentTimeMillis() - callStartedAt!!) / 1000
                    } else {
                        0L // 벨만 울리고 끊긴 부재중 전화
                    }
                    eventQueue.enqueueCallEvent(
                        direction = if (callStartedAt != null) "COMPLETED" else "MISSED",
                        counterpartNumber = ringingNumber,
                        startedAtEpochMs = startedAt,
                        durationSeconds = durationSeconds
                    )
                }
                callStartedAt = null
                ringingStartedAt = null
                ringingNumber = null
            }
        }
    }

    companion object {
        // BroadcastReceiver는 매번 새 인스턴스로 생성되므로 통화 상태 추적은 프로세스 전역 상태로 유지한다.
        private var ringingStartedAt: Long? = null
        private var ringingNumber: String? = null
        private var callStartedAt: Long? = null
    }
}
