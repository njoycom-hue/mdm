package com.safecircle.backend.routes

import com.safecircle.backend.db.DeviceTokens
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

@Serializable
data class RegisterFcmTokenRequest(val fcmToken: String)

/** 보호자/피감독자 공통: 푸시 발송 대상 토큰 등록. 역할 무관하게 자기 자신의 토큰만 등록 가능. */
fun Route.deviceRoutes() {
    authenticate("auth-jwt") {
        post("/v1/devices/fcm-token") {
            val principal = call.userPrincipal()
            val body = call.receive<RegisterFcmTokenRequest>()

            transaction {
                val updated = DeviceTokens.update({
                    (DeviceTokens.userId eq principal.userId) and (DeviceTokens.fcmToken eq body.fcmToken)
                }) {
                    it[updatedAt] = Instant.now()
                }
                if (updated == 0) {
                    DeviceTokens.insert {
                        it[userId] = principal.userId
                        it[fcmToken] = body.fcmToken
                        it[updatedAt] = Instant.now()
                    }
                }
            }
            call.respond(HttpStatusCode.OK)
        }
    }
}
