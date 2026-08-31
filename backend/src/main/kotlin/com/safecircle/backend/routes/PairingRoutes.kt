package com.safecircle.backend.routes

import com.safecircle.backend.db.PairingCodes
import com.safecircle.backend.db.Pairings
import com.safecircle.backend.security.requireRole
import com.safecircle.backend.security.userPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.temporal.ChronoUnit

@Serializable
data class ClaimPairingRequest(val pairingCode: String)

@Serializable
data class PairingCodeResponse(val code: String, val expiresAtEpochMs: Long)

/** 피감독자가 코드를 발급하고, 보호자가 그 코드를 입력해 연결한다 (보호자가 임의로 연결 불가). */
fun Route.pairingRoutes() {
    authenticate("auth-jwt") {
        post("/v1/pairing/issue") {
            val principal = call.userPrincipal()
            principal.requireRole("WARD")

            val code = (100000..999999).random().toString()
            val expiresAt = Instant.now().plus(10, ChronoUnit.MINUTES)

            transaction {
                PairingCodes.insert {
                    it[PairingCodes.code] = code
                    it[wardId] = principal.userId
                    it[PairingCodes.expiresAt] = expiresAt
                }
            }
            call.respond(PairingCodeResponse(code = code, expiresAtEpochMs = expiresAt.toEpochMilli()))
        }

        post("/v1/pairing/claim") {
            val principal = call.userPrincipal()
            principal.requireRole("GUARDIAN")
            val body = call.receive<ClaimPairingRequest>()

            val result = transaction {
                val row = PairingCodes.select {
                    (PairingCodes.code eq body.pairingCode) and (PairingCodes.claimedAt.isNull())
                }.singleOrNull() ?: return@transaction null

                if (row[PairingCodes.expiresAt].isBefore(Instant.now())) return@transaction null

                PairingCodes.update({ PairingCodes.id eq row[PairingCodes.id] }) {
                    it[claimedAt] = Instant.now()
                }
                Pairings.insert {
                    it[guardianId] = principal.userId
                    it[wardId] = row[PairingCodes.wardId]
                    it[createdAt] = Instant.now()
                }
                row[PairingCodes.wardId]
            }

            if (result == null) {
                call.respond(HttpStatusCode.Gone, "invalid or expired pairing code")
            } else {
                call.respond(HttpStatusCode.Created)
            }
        }
    }
}
