package com.safecircle.backend.routes

import com.safecircle.backend.db.ActivityAlerts
import com.safecircle.backend.db.AppTimeLimits
import com.safecircle.backend.db.AppUsageEvents
import com.safecircle.backend.db.KeywordAlerts
import com.safecircle.backend.db.Pairings
import com.safecircle.backend.db.Users
import com.safecircle.backend.security.requireRole
import com.safecircle.backend.security.userPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
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

@Serializable
data class AppTimeLimitSummary(val packageName: String, val appLabel: String, val dailyLimitMinutes: Int)

@Serializable
data class ActivityAlertSummary(val type: String, val detail: String, val occurredAtEpochMs: Long)

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
            call.respond(loadUsageSummary(wardId))
        }

        get("/v1/guardian/wards/{wardId}/time-limits") {
            val principal = call.userPrincipal()
            principal.requireRole("GUARDIAN")
            val wardId = UUID.fromString(call.parameters["wardId"])
            if (!isPaired(principal.userId, wardId)) {
                call.respond(HttpStatusCode.Forbidden)
                return@get
            }
            call.respond(loadTimeLimits(wardId))
        }

        put("/v1/guardian/wards/{wardId}/time-limits") {
            val principal = call.userPrincipal()
            principal.requireRole("GUARDIAN")
            val wardId = UUID.fromString(call.parameters["wardId"])
            if (!isPaired(principal.userId, wardId)) {
                call.respond(HttpStatusCode.Forbidden)
                return@put
            }

            val body = call.receive<List<AppTimeLimitSummary>>()
            transaction {
                // 매번 전체를 대체 저장한다 — 개별 patch보다 클라이언트(체크박스+숫자 목록) 모델과 맞는다.
                AppTimeLimits.deleteWhere { AppTimeLimits.wardId eq wardId }
                body.filter { it.dailyLimitMinutes > 0 }.forEach { limit ->
                    AppTimeLimits.insert {
                        it[AppTimeLimits.wardId] = wardId
                        it[packageName] = limit.packageName
                        it[appLabel] = limit.appLabel
                        it[dailyLimitMinutes] = limit.dailyLimitMinutes
                    }
                }
            }
            call.respond(HttpStatusCode.OK)
        }

        get("/v1/guardian/wards/{wardId}/activity-alerts") {
            val principal = call.userPrincipal()
            principal.requireRole("GUARDIAN")
            val wardId = UUID.fromString(call.parameters["wardId"])
            if (!isPaired(principal.userId, wardId)) {
                call.respond(HttpStatusCode.Forbidden)
                return@get
            }

            val events = transaction {
                ActivityAlerts
                    .select { ActivityAlerts.wardId eq wardId }
                    .orderBy(ActivityAlerts.occurredAt, SortOrder.DESC)
                    .limit(50)
                    .map {
                        ActivityAlertSummary(
                            type = it[ActivityAlerts.type],
                            detail = it[ActivityAlerts.detail],
                            occurredAtEpochMs = it[ActivityAlerts.occurredAt].toEpochMilli()
                        )
                    }
            }
            call.respond(events)
        }
    }
}

/** WARD 본인용: 자기 자신의 최근 24시간 앱별 사용시간을 조회한다 (보호자가 뭘 보는지 투명하게 공개). */
fun Route.usageRoutes() {
    authenticate("auth-jwt") {
        get("/v1/usage/mine") {
            val principal = call.userPrincipal()
            principal.requireRole("WARD")
            call.respond(loadUsageSummary(principal.userId))
        }
    }
}

/** WARD 본인 조회(/v1/usage/mine)와 GUARDIAN 조회에서 공유하는 최근 24시간 사용시간 집계. */
fun loadUsageSummary(wardId: UUID): List<AppUsageSummary> {
    val since = Instant.now().minus(24, ChronoUnit.HOURS)
    return transaction {
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
}

fun loadTimeLimits(wardId: UUID): List<AppTimeLimitSummary> = transaction {
    AppTimeLimits
        .select { AppTimeLimits.wardId eq wardId }
        .map {
            AppTimeLimitSummary(
                packageName = it[AppTimeLimits.packageName],
                appLabel = it[AppTimeLimits.appLabel],
                dailyLimitMinutes = it[AppTimeLimits.dailyLimitMinutes]
            )
        }
}
