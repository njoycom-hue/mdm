package com.safecircle.app.usage

import android.app.usage.UsageStatsManager
import android.content.Context
import com.safecircle.app.network.dto.AppUsageDto

class UsageStatsRepository(private val context: Context) {

    /** 최근 [sinceEpochMs] 이후의 앱별 사용시간 합계를 반환한다. 보호자 화면에 표시할 사람이 읽을 수 있는 앱 이름도 함께 담는다. */
    fun collectSince(sinceEpochMs: Long): List<AppUsageDto> {
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val stats = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, sinceEpochMs, now)

        return stats
            .filter { it.totalTimeInForeground > 0 }
            .map { usage ->
                AppUsageDto(
                    packageName = usage.packageName,
                    appLabel = resolveAppLabel(usage.packageName),
                    foregroundMillis = usage.totalTimeInForeground,
                    lastUsedEpochMs = usage.lastTimeUsed
                )
            }
    }

    private fun resolveAppLabel(packageName: String): String {
        val packageManager = context.packageManager
        return runCatching {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        }.getOrDefault(packageName)
    }
}
