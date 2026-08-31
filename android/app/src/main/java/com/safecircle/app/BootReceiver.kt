package com.safecircle.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import com.safecircle.app.auth.TokenStore
import com.safecircle.app.onboarding.ConsentStore
import com.safecircle.app.vpn.DomainFilterVpnService

/** 재부팅 후 VPN 필터를 재시작한다. 이미 사용자가 승인했던 경우 VpnService.prepare()는 null을 반환해 재동의 없이 바로 시작 가능. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!TokenStore(context).isLoggedIn() || !ConsentStore(context).hasConsented()) return

        if (VpnService.prepare(context) == null) {
            context.startService(Intent(context, DomainFilterVpnService::class.java))
        }
    }
}
