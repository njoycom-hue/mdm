package com.safecircle.backend.routes

import com.safecircle.backend.db.WardSettings
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
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

@Serializable
data class WardSettingsResponse(
    val keywords: List<String>,
    val blockedPackages: List<String>,
    val blockedDomains: List<String>
)

@Serializable
data class WardSettingsRequest(
    val keywords: List<String>,
    val blockedPackages: List<String>,
    val blockedDomains: List<String>
)

private fun csv(list: List<String>) = list.joinToString(",")
private fun fromCsv(csv: String) = if (csv.isBlank()) emptyList() else csv.split(",")

/**
 * WARD: 자기 자신의 정책 조회(클라이언트 동기화용).
 * GUARDIAN: 페어링된 ward의 정책 조회/수정.
 */
fun Route.settingsRoutes() {
    authenticate("auth-jwt") {
        get("/v1/settings/mine") {
            val principal = call.userPrincipal()
            principal.requireRole("WARD")
            call.respond(loadSettings(principal.userId))
        }

        get("/v1/settings/ward/{wardId}") {
            val principal = call.userPrincipal()
            principal.requireRole("GUARDIAN")
            val wardId = UUID.fromString(call.parameters["wardId"])
            if (!isPaired(principal.userId, wardId)) {
                call.respond(HttpStatusCode.Forbidden)
                return@get
            }
            call.respond(loadSettings(wardId))
        }

        put("/v1/settings/ward/{wardId}") {
            val principal = call.userPrincipal()
            principal.requireRole("GUARDIAN")
            val wardId = UUID.fromString(call.parameters["wardId"])
            if (!isPaired(principal.userId, wardId)) {
                call.respond(HttpStatusCode.Forbidden)
                return@put
            }

            val body = call.receive<WardSettingsRequest>()
            transaction {
                val updated = WardSettings.update({ WardSettings.wardId eq wardId }) {
                    it[keywordsCsv] = csv(body.keywords)
                    it[blockedPackagesCsv] = csv(body.blockedPackages)
                    it[blockedDomainsCsv] = csv(body.blockedDomains)
                    it[updatedAt] = Instant.now()
                }
                if (updated == 0) {
                    WardSettings.insert {
                        it[WardSettings.wardId] = wardId
                        it[keywordsCsv] = csv(body.keywords)
                        it[blockedPackagesCsv] = csv(body.blockedPackages)
                        it[blockedDomainsCsv] = csv(body.blockedDomains)
                        it[updatedAt] = Instant.now()
                    }
                }
            }
            call.respond(HttpStatusCode.OK)
        }
    }
}

private fun loadSettings(wardId: UUID): WardSettingsResponse = transaction {
    val row = WardSettings.select { WardSettings.wardId eq wardId }.singleOrNull()
    if (row == null) {
        WardSettingsResponse(emptyList(), emptyList(), emptyList())
    } else {
        WardSettingsResponse(
            keywords = fromCsv(row[WardSettings.keywordsCsv]),
            blockedPackages = fromCsv(row[WardSettings.blockedPackagesCsv]),
            blockedDomains = fromCsv(row[WardSettings.blockedDomainsCsv])
        )
    }
}
