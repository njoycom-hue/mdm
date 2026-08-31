package com.safecircle.app.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant

/**
 * 부모님 프로필의 "생체 신호" 감지. 일반 폰에는 심박 센서가 없으므로, 갤럭시 워치 등
 * 스마트워치가 Health Connect에 기록해 둔 심박수/걸음수를 대신 읽는다. Health Connect
 * 자체가 없거나(구버전 기기), 권한이 없거나, 애초에 연동된 워치가 없는 기기에서는
 * [NoSignalSource]를 반환하고 — 이 경우 InactivityCheckWorker가 기존의 화면 조작
 * 기반 무활동 감지로 자동 대체한다.
 *
 * 주의: 이 클래스는 실제 워치 데이터로 검증되지 않았다 — Health Connect API 자체가
 * 실제 페어링된 워치 없이는 이 사고 환경에서 테스트할 방법이 없다. Health Connect
 * 공식 문서(권한 모델, ReadRecordsRequest 사용법)를 따라 작성했지만, 실기기(워치
 * 연동 후)에서 한 번은 직접 확인이 필요하다.
 */
sealed class HealthSignalResult {
    /** Health Connect가 없거나, 권한이 없거나, 조회 자체가 실패함 — 무활동 감지로 폴백해야 함. */
    object NoSignalSource : HealthSignalResult()
    /** Health Connect 연동은 되어 있음. 조회 구간 내 마지막 신호 시각(없으면 null = 최근엔 신호 없음). */
    data class Available(val lastSignalAtEpochMs: Long?) : HealthSignalResult()
}

class HealthSignalRepository(private val context: Context) {

    private val requiredPermissions = setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class)
    )

    fun permissionsToRequest(): Set<String> = requiredPermissions

    fun isHealthConnectAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    suspend fun hasRequiredPermission(): Boolean {
        if (!isHealthConnectAvailable()) return false
        return runCatching {
            val granted = HealthConnectClient.getOrCreate(context).permissionController.getGrantedPermissions()
            granted.any { it in requiredPermissions }
        }.getOrDefault(false)
    }

    /** [lookbackMs] 구간 안의 심박수/걸음수 기록 중 가장 최근 시각을 반환한다. */
    suspend fun mostRecentSignal(lookbackMs: Long): HealthSignalResult {
        if (!hasRequiredPermission()) return HealthSignalResult.NoSignalSource

        return runCatching {
            val client = HealthConnectClient.getOrCreate(context)
            val now = Instant.now()
            val range = TimeRangeFilter.between(now.minusMillis(lookbackMs), now)

            val heartRateTimes = client.readRecords(ReadRecordsRequest(HeartRateRecord::class, range))
                .records.flatMap { record -> record.samples.map { it.time } }
            val stepsTimes = client.readRecords(ReadRecordsRequest(StepsRecord::class, range))
                .records.map { it.endTime }

            val latest = (heartRateTimes + stepsTimes).maxOrNull()
            HealthSignalResult.Available(latest?.toEpochMilli())
        }.getOrDefault(HealthSignalResult.NoSignalSource)
    }
}
