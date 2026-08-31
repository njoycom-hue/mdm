package com.safecircle.backend.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(val email: String, val password: String, val role: String) // GUARDIAN | WARD

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class AuthResponse(val token: String)

fun Route.authRoutes() {
    post("/v1/auth/register") {
        val body = call.receive<RegisterRequest>()
        // TODO: bcrypt로 password 해싱 후 Users에 insert, role 검증(GUARDIAN|WARD)
        call.respond(HttpStatusCode.Created)
    }

    post("/v1/auth/login") {
        val body = call.receive<LoginRequest>()
        // TODO: 사용자 조회 + bcrypt 검증 후 JWT 발급 (auth.jwtSecret 사용)
        call.respond(AuthResponse(token = "TODO"))
    }
}
