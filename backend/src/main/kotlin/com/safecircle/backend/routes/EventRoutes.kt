package com.safecircle.backend.routes

import com.safecircle.backend.db.AppUsageEvents
import com.safecircle.backend.db.CallEvents
import com.safecircle.backend.db.DeviceTokens
import com.safecircle.backend.db.KeywordAlerts
import com.safecircle.backend.db.Pairings
import com.safecircle.backend.db.Users
import com.safecircle.backend.security.requireRole
import com.safecircle.backend.security.userPrincipal
import com.safecircle.backend.services.FcmService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

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
    val usage: List<AppUsageEventRequest> = emptyList(),
    val keywordAlerts: List<KeywordAlertRequest> = emptyList(),
    val callEvents: List<CallEventRequest> = emptyList()
)

/** 클라이언트의 5~15분 배치 업로드를 수신한다. 키워드 매치가 포함되면 즉시 FCM 푸시로 이어진다. */
fun Route.eventRoutes() {
    authenticate("auth-jwt") {
        post("/v1/events/batch") {
            val principal = call.userPrincipal()
            principal.requireRole("WARD")
            val body = call.receive<BatchUploadRequest>()

            transaction {
                body.usage.forEach { usage ->
                    AppUsageEvents.insert {
                        it[wardId] = principal.userId
                        it[packageName] = usage.packageName
                        it[foregroundMillis] = usage.foregroundMillis
                        it[lastUsedAt] = Instant.ofEpochMilli(usage.lastUsedEpochMs)
                        it[receivedAt] = Instant.now()
                    }
                }
                body.callEvents.forEach { callEvent ->
                    CallEvents.insert {
                        it[wardId] = principal.userId
                        it[direction] = callEvent.direction
                        it[counterpartNumber] = callEvent.counterpartNumber
                        it[startedAt] = Instant.ofEpochMilli(callEvent.startedAtEpochMs)
                        it[durationSeconds] = callEvent.durationSeconds
                    }
                }
                body.keywordAlerts.forEach { alert ->
                    KeywordAlerts.insert {
                        it[wardId] = principal.userId
                        it[sourceApp] = alert.sourceApp
                        it[matchedKeywords] = alert.matchedKeywords.joinToString(",")
                        it[occurredAt] = Instant.ofEpochMilli(alert.occurredAtEpochMs)
                    }
                }
            }

            if (body.keywordAlerts.isNotEmpty()) {
                notifyGuardians(principal.userId, body.keywordAlerts.flatMap { it.matchedKeywords }.distinct())
            }

            call.respond(HttpStatusCode.Accepted)
        }
    }
}

private fun notifyGuardians(wardId: java.util.UUID, matchedKeywords: List<String>) {
    val (wardEmail, tokens) = transaction {
        val email = Users.select { Users.id eq wardId }.single()[Users.email]
        val guardianIds = Pairings.select { Pairings.wardId eq wardId }.map { it[Pairings.guardianId] }
        val fcmTokens = DeviceTokens.select { DeviceTokens.userId inList guardianIds }.map { it[DeviceTokens.fcmToken] }
        email to fcmTokens
    }

    tokens.forEach { token ->
        runCatching { FcmService.sendKeywordAlert(token, wardEmail, matchedKeywords) }
            .onFailure { /* TODO: 구조화된 로깅 + 만료 토큰 정리 */ }
    }
}
