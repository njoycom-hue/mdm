package com.safecircle.app.install

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.safecircle.app.network.dto.PROFILE_ADDICT
import com.safecircle.app.settings.PolicyRepository
import com.safecircle.app.sync.ActivityReporter
import com.safecircle.app.sync.InstalledAppsSyncWorker

/**
 * 앱이 설치/삭제될 때마다 보호자가 보는 "설치 앱 목록"을 즉시 다시 동기화한다 —
 * 사용시간 데이터처럼 쌓이길 기다리지 않고, 새로 깐 앱도 지운 앱도 바로 반영되도록.
 * 중독자 프로필에서는 추가로 신규 설치를 즉시 알림으로도 보낸다(도박/대출 앱
 * 재설치 등 감지 목적).
 * ACTION_PACKAGE_ADDED/REMOVED는 패키지 변경 관련 브로드캐스트라 백그라운드 제한
 * 예외 대상이라 매니페스트에 등록된 정적 리시버로도 잘 수신된다.
 */
class PackageChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart ?: return
        if (packageName == context.packageName) return

        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED -> {
                InstalledAppsSyncWorker.enqueueImmediate(context)
                // 같은 앱의 업데이트(재설치)는 신규 설치 알림 대상에서 제외한다.
                if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return
                if (PolicyRepository(context).profileType() != PROFILE_ADDICT) return

                val appLabel = runCatching {
                    val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
                    context.packageManager.getApplicationLabel(appInfo).toString()
                }.getOrDefault(packageName)

                ActivityReporter.report(context, "APP_INSTALLED", appLabel)
            }
            Intent.ACTION_PACKAGE_REMOVED -> {
                if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return
                InstalledAppsSyncWorker.enqueueImmediate(context)
            }
        }
    }
}
