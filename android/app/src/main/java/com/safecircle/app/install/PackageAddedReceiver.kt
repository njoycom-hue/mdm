package com.safecircle.app.install

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.safecircle.app.network.dto.PROFILE_ADDICT
import com.safecircle.app.settings.PolicyRepository
import com.safecircle.app.sync.ActivityReporter

/**
 * 중독자 프로필 전용: 새 앱이 설치되면 보호자에게 즉시 알린다 (도박/대출 앱 재설치 등 감지).
 * ACTION_PACKAGE_ADDED는 패키지 변경 관련 브로드캐스트라 백그라운드 제한 예외 대상이라
 * 매니페스트에 등록된 정적 리시버로도 잘 수신된다.
 */
class PackageAddedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_PACKAGE_ADDED) return
        // 같은 앱의 업데이트(재설치)는 제외하고 진짜 신규 설치만 본다.
        if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return

        val packageName = intent.data?.schemeSpecificPart ?: return
        if (packageName == context.packageName) return

        if (PolicyRepository(context).profileType() != PROFILE_ADDICT) return

        val appLabel = runCatching {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        }.getOrDefault(packageName)

        ActivityReporter.report(context, "APP_INSTALLED", appLabel)
    }
}
