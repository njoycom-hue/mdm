package com.safecircle.backend.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
data class ClaimPairingRequest(val pairingCode: String)

@Serializable
data class PairingCodeResponse(val code: String, val expiresAtEpochMs: Long)

/** 피감독자가 코드를 발급하고, 보호자가 그 코드를 입력해 연결한다 (보호자가 임의로 연결 불가). */
fun Route.pairingRoutes() {
    post("/v1/pairing/issue") {
        // TODO: 인증된 ward 사용자에 대해 6자리 코드 생성, 10분 TTL로 캐시(Redis 또는 DB) 저장
        val code = (100000..999999).random(Random).toString()
        call.respond(PairingCodeResponse(code = code, expiresAtEpochMs = System.currentTimeMillis() + 10 * 60_000))
    }

    post("/v1/pairing/claim") {
        val body = call.receive<ClaimPairingRequest>()
        // TODO: 인증된 guardian 사용자가 code로 ward를 찾아 Pairings에 insert
        call.respond(HttpStatusCode.Created)
    }
}
