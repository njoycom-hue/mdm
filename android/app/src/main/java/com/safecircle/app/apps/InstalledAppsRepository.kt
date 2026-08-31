package com.safecircle.app.apps

import android.content.Context
import android.content.Intent
import com.safecircle.app.network.dto.InstalledAppDto

/**
 * 피감독자 기기에서 "런처에 실제로 노출되는 앱"만 골라낸다 — 시스템 서비스/라이브러리
 * 패키지까지 다 나오면 보호자가 고르기 힘들어지므로, 홈 화면 앱 서랍에 뜨는 것과 같은
 * 기준(ACTION_MAIN + CATEGORY_LAUNCHER)으로 필터링한다.
 */
class InstalledAppsRepository(private val context: Context) {

    fun listLaunchableApps(): List<InstalledAppDto> {
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        return packageManager.queryIntentActivities(launcherIntent, 0)
            .map { it.activityInfo.packageName }
            .distinct()
            .filter { it != context.packageName }
            .mapNotNull { packageName ->
                runCatching {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    InstalledAppDto(packageName, packageManager.getApplicationLabel(appInfo).toString())
                }.getOrNull()
            }
    }
}
