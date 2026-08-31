package com.safecircle.app.usage

import android.app.usage.UsageStatsManager
import android.content.Context
import com.safecircle.app.network.dto.AppUsageDto

class UsageStatsRepository(private val context: Context) {

    /** 최근 [sinceEpochMs] 이후의 앱별 사용시간 합계를 반환한다. */
    fun collectSince(sinceEpochMs: Long): List<AppUsageDto> {
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val stats = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, sinceEpochMs, now)

        return stats
            .filter { it.totalTimeInForeground > 0 }
            .map { usage ->
                AppUsageDto(
                    packageName = usage.packageName,
                    foregroundMillis = usage.totalTimeInForeground,
                    lastUsedEpochMs = usage.lastTimeUsed
                )
            }
    }
}
