package com.safecircle.backend.routes

import com.safecircle.backend.db.ConsentEvents
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
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant

@Serializable
data class ConsentEventRequest(val deviceId: String, val appVersion: String)

/** 통신비밀보호법 대응용 동의 기록 — 절대 삭제하지 않고 revokedAt으로만 상태를 표시한다. */
fun Route.consentRoutes() {
    authenticate("auth-jwt") {
        post("/v1/consents") {
            val principal = call.userPrincipal()
            principal.requireRole("WARD")
            val body = call.receive<ConsentEventRequest>()

            transaction {
                ConsentEvents.insert {
                    it[wardId] = principal.userId
                    it[deviceId] = body.deviceId
                    it[appVersion] = body.appVersion
                    it[consentedAt] = Instant.now()
                }
            }
            call.respond(HttpStatusCode.Created)
        }

        post("/v1/consents/revoke") {
            val principal = call.userPrincipal()
            principal.requireRole("WARD")
            val body = call.receive<ConsentEventRequest>()

            val updated = transaction {
                ConsentEvents.update({
                    (ConsentEvents.wardId eq principal.userId) and
                        (ConsentEvents.deviceId eq body.deviceId) and
                        (ConsentEvents.revokedAt.isNull())
                }) {
                    it[revokedAt] = Instant.now()
                }
            }
            if (updated == 0) call.respond(HttpStatusCode.NotFound) else call.respond(HttpStatusCode.OK)
        }
    }
}
