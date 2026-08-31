package com.safecircle.backend.routes

import com.safecircle.backend.db.AppUsageEvents
import com.safecircle.backend.db.KeywordAlerts
import com.safecircle.backend.db.Pairings
import com.safecircle.backend.db.Users
import com.safecircle.backend.security.requireRole
import com.safecircle.backend.security.userPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Serializable
data class WardSummary(val wardId: String, val email: String, val pairedAtEpochMs: Long)

@Serializable
data class KeywordAlertSummary(val sourceApp: String, val matchedKeywords: List<String>, val occurredAtEpochMs: Long)

@Serializable
data class AppUsageSummary(val packageName: String, val totalForegroundMillis: Long)

/** GUARDIAN 전용: 연결된 피보호자 목록, 최근 키워드 알림, 최근 사용시간 요약을 조회한다. */
fun Route.guardianRoutes() {
    authenticate("auth-jwt") {
        get("/v1/guardian/wards") {
            val principal = call.userPrincipal()
            principal.requireRole("GUARDIAN")

            val wards = transaction {
                Pairings.join(Users, JoinType.INNER, onColumn = Pairings.wardId, otherColumn = Users.id)
                    .select { Pairings.guardianId eq principal.userId }
                    .map {
                        WardSummary(
                            wardId = it[Pairings.wardId].value.toString(),
                            email = it[Users.email],
                            pairedAtEpochMs = it[Pairings.createdAt].toEpochMilli()
                        )
                    }
            }
            call.respond(wards)
        }

        get("/v1/guardian/wards/{wardId}/alerts") {
            val principal = call.userPrincipal()
            principal.requireRole("GUARDIAN")
            val wardId = UUID.fromString(call.parameters["wardId"])
            if (!isPaired(principal.userId, wardId)) {
                call.respond(HttpStatusCode.Forbidden)
                return@get
            }

            val alerts = transaction {
                KeywordAlerts
                    .select { KeywordAlerts.wardId eq wardId }
                    .orderBy(KeywordAlerts.occurredAt, SortOrder.DESC)
                    .limit(50)
                    .map {
                        KeywordAlertSummary(
                            sourceApp = it[KeywordAlerts.sourceApp],
                            matchedKeywords = it[KeywordAlerts.matchedKeywords].split(","),
                            occurredAtEpochMs = it[KeywordAlerts.occurredAt].toEpochMilli()
                        )
                    }
            }
            call.respond(alerts)
        }

        get("/v1/guardian/wards/{wardId}/usage") {
            val principal = call.userPrincipal()
            principal.requireRole("GUARDIAN")
            val wardId = UUID.fromString(call.parameters["wardId"])
            if (!isPaired(principal.userId, wardId)) {
                call.respond(HttpStatusCode.Forbidden)
                return@get
            }

            val since = Instant.now().minus(24, ChronoUnit.HOURS)
            val usage = transaction {
                val sumColumn = AppUsageEvents.foregroundMillis.sum()
                AppUsageEvents
                    .slice(AppUsageEvents.packageName, sumColumn)
                    .selectAll()
                    .andWhere { AppUsageEvents.wardId eq wardId }
                    .andWhere { AppUsageEvents.receivedAt greaterEq since }
                    .groupBy(AppUsageEvents.packageName)
                    .orderBy(sumColumn to SortOrder.DESC)
                    .map { AppUsageSummary(it[AppUsageEvents.packageName], it[sumColumn] ?: 0L) }
            }
            call.respond(usage)
        }
    }
}
