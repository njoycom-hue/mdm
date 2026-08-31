package com.safecircle.backend.routes

import com.safecircle.backend.db.InstalledApps
import com.safecircle.backend.security.requireRole
import com.safecircle.backend.security.userPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

@Serializable
data class InstalledAppDto(val packageName: String, val appLabel: String)

/**
 * WARD: 자기 기기에 설치된(런처에 노출되는) 앱 전체 목록을 동기화한다(전체 교체).
 * GUARDIAN: 페어링된 ward의 설치 앱 목록을 조회한다 — 패키지명을 직접 몰라도 실제
 * 목록에서 차단/감시/시간제한 대상을 고를 수 있게 하는 것이 목적이다.
 */
fun Route.installedAppsRoutes() {
    authenticate("auth-jwt") {
        post("/v1/apps/mine") {
            val principal = call.userPrincipal()
            principal.requireRole("WARD")
            val body = call.receive<List<InstalledAppDto>>()

            transaction {
                InstalledApps.deleteWhere { InstalledApps.wardId eq principal.userId }
                val now = Instant.now()
                body.forEach { app ->
                    InstalledApps.insert {
                        it[wardId] = principal.userId
                        it[packageName] = app.packageName
                        it[appLabel] = app.appLabel
                        it[updatedAt] = now
                    }
                }
            }
            call.respond(HttpStatusCode.OK)
        }

        get("/v1/guardian/wards/{wardId}/installed-apps") {
            val principal = call.userPrincipal()
            principal.requireRole("GUARDIAN")
            val wardId = UUID.fromString(call.parameters["wardId"])
            if (!isPaired(principal.userId, wardId)) {
                call.respond(HttpStatusCode.Forbidden)
                return@get
            }
            val apps = transaction {
                InstalledApps.select { InstalledApps.wardId eq wardId }
                    .map { InstalledAppDto(it[InstalledApps.packageName], it[InstalledApps.appLabel]) }
                    .sortedBy { it.appLabel.ifBlank { it.packageName } }
            }
            call.respond(apps)
        }
    }
}
