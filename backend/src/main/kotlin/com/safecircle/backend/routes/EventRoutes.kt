package com.safecircle.backend.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable

@Serializable
data class AppUsageEventRequest(val packageName: String, val foregroundMillis: Long, val lastUsedEpochMs: Long)

@Serializable
data class KeywordAlertRequest(val sourceApp: String, val matchedKeywords: List<String>, val occurredAtEpochMs: Long)

@Serializable
data class CallEventRequest(
    val direction: String,
    val counterpartNumber: String?,
    val startedAtEpochMs: Long,
    val durationSeconds: Long?
)

@Serializable
data class BatchUploadRequest(
    val deviceId: String,
    val usage: List<AppUsageEventRequest> = emptyList(),
    val keywordAlerts: List<KeywordAlertRequest> = emptyList(),
    val callEvents: List<CallEventRequest> = emptyList()
)

/** 클라이언트의 5~15분 배치 업로드를 수신한다. 키워드 매치가 포함되면 즉시 FCM 푸시로 이어진다. */
fun Route.eventRoutes() {
    post("/v1/events/batch") {
        val body = call.receive<BatchUploadRequest>()
        // TODO: JWT에서 ward 사용자 확인 후 각 컬렉션을 해당 Exposed 테이블에 insert
        // TODO: body.keywordAlerts가 비어있지 않으면 페어링된 guardian의 FCM 토큰 조회 후 FcmService.sendKeywordAlert 호출
        call.respond(HttpStatusCode.Accepted)
    }
}
