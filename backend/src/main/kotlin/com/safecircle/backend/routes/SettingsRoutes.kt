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

/** 아동=유해사이트차단+시간제한, 중독자=차단+실행알림+신규설치알림, 부모님=위험키워드감지+무활동감지. */
private val VALID_PROFILE_TYPES = setOf("CHILD", "ADDICT", "ELDERLY")

@Serializable
data class WardSettingsResponse(
    val profileType: String,
    val keywords: List<String>,
    val blockedPackages: List<String>,
    val blockedDomains: List<String>,
    val watchedPackages: List<String>,
    val appTimeLimits: List<AppTimeLimitDto> = emptyList()
)

@Serializable
data class WardSettingsRequest(
    val profileType: String,
    val keywords: List<String>,
    val blockedPackages: List<String>,
    val blockedDomains: List<String>,
    val watchedPackages: List<String>
)

@Serializable
data class AppTimeLimitDto(val packageName: String, val dailyLimitMinutes: Int)

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
            if (body.profileType !in VALID_PROFILE_TYPES) {
                call.respond(HttpStatusCode.BadRequest, "profileType must be one of $VALID_PROFILE_TYPES")
                return@put
            }
            transaction {
                val updated = WardSettings.update({ WardSettings.wardId eq wardId }) {
                    it[profileType] = body.profileType
                    it[keywordsCsv] = csv(body.keywords)
                    it[blockedPackagesCsv] = csv(body.blockedPackages)
                    it[blockedDomainsCsv] = csv(body.blockedDomains)
                    it[watchedPackagesCsv] = csv(body.watchedPackages)
                    it[updatedAt] = Instant.now()
                }
                if (updated == 0) {
                    WardSettings.insert {
                        it[WardSettings.wardId] = wardId
                        it[profileType] = body.profileType
                        it[keywordsCsv] = csv(body.keywords)
                        it[blockedPackagesCsv] = csv(body.blockedPackages)
                        it[blockedDomainsCsv] = csv(body.blockedDomains)
                        it[watchedPackagesCsv] = csv(body.watchedPackages)
                        it[updatedAt] = Instant.now()
                    }
                }
            }
            call.respond(HttpStatusCode.OK)
        }
    }
}

private fun loadSettings(wardId: UUID): WardSettingsResponse {
    val row = transaction { WardSettings.select { WardSettings.wardId eq wardId }.singleOrNull() }
    val timeLimits = loadTimeLimits(wardId).map { AppTimeLimitDto(it.packageName, it.dailyLimitMinutes) }
    return if (row == null) {
        WardSettingsResponse(
            profileType = "ADDICT",
            keywords = emptyList(),
            blockedPackages = emptyList(),
            blockedDomains = emptyList(),
            watchedPackages = emptyList(),
            appTimeLimits = timeLimits
        )
    } else {
        WardSettingsResponse(
            profileType = row[WardSettings.profileType],
            keywords = fromCsv(row[WardSettings.keywordsCsv]),
            blockedPackages = fromCsv(row[WardSettings.blockedPackagesCsv]),
            blockedDomains = fromCsv(row[WardSettings.blockedDomainsCsv]),
            watchedPackages = fromCsv(row[WardSettings.watchedPackagesCsv]),
            appTimeLimits = timeLimits
        )
    }
}
