package com.safecircle.app.telephony

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager

/**
 * READ_CALL_LOG 없이 통화 상태 브로드캐스트로 발신/수신 번호와 시각만 캡처한다.
 * 통화 내용은 녹음/열람하지 않는다.
 */
class CallStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                // TODO: EventQueue.enqueueCallEvent(direction = INCOMING, number, timestamp)
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                // TODO: 통화 종료 시각 기록, 통화 시간 계산 후 큐에 적재
            }
        }
    }
}
