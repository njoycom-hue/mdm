package com.safecircle.backend.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable

@Serializable
data class ConsentEventRequest(
    val deviceId: String,
    val consentedAtEpochMs: Long,
    val appVersion: String
)

/** 통신비밀보호법 대응용 동의 기록 — 절대 삭제하지 않고 revokedAt으로만 상태를 표시한다. */
fun Route.consentRoutes() {
    post("/v1/consents") {
        val body = call.receive<ConsentEventRequest>()
        // TODO: 인증된 ward 사용자 ID를 JWT에서 추출해 ConsentEvents에 insert
        call.respond(HttpStatusCode.Created)
    }
}
