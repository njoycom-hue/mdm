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
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Serializable
data class WardSummary(val wardId: String, val email: String, val pairedAtEpochMs: Long)

@Serializable
data class KeywordAlertSummary(val sourceApp: String, val matchedKeywords: List<String>, val occurredAtEpochMs: Long)

@Serializable
data class AppUsageSummary(val packageName: String, val appLabel: String, val totalForegroundMillis: Long)

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
                // appLabel은 패키지명당 하나로 묶이지 않을 수 있어(과거 데이터엔 빈 값) DB에서
                // groupBy(packageName, appLabel)로 합산하면 같은 앱이 두 줄로 쪼개질 수 있다.
                // 그래서 행 단위로 가져와 최신순으로 정렬한 뒤 코틀린에서 패키지명 기준으로 합산하고,
                // 그중 가장 최근에 수신된 비어있지 않은 appLabel을 대표 이름으로 쓴다.
                AppUsageEvents
                    .select { AppUsageEvents.wardId eq wardId }
                    .andWhere { AppUsageEvents.receivedAt greaterEq since }
                    .orderBy(AppUsageEvents.receivedAt, SortOrder.DESC)
                    .groupBy { it[AppUsageEvents.packageName] }
                    .map { (packageName, rows) ->
                        val label = rows.firstOrNull { it[AppUsageEvents.appLabel].isNotBlank() }
                            ?.get(AppUsageEvents.appLabel) ?: packageName
                        AppUsageSummary(
                            packageName = packageName,
                            appLabel = label,
                            totalForegroundMillis = rows.sumOf { it[AppUsageEvents.foregroundMillis] }
                        )
                    }
                    .sortedByDescending { it.totalForegroundMillis }
            }
            call.respond(usage)
        }
    }
}
