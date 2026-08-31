package com.safecircle.backend.routes

import com.safecircle.backend.db.Users
import com.safecircle.backend.security.JwtService
import com.safecircle.backend.security.PasswordHasher
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

@Serializable
data class RegisterRequest(val email: String, val password: String, val role: String) // GUARDIAN | WARD

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class AuthResponse(val token: String, val userId: String, val role: String)

fun Route.authRoutes(jwtService: JwtService) {
    post("/v1/auth/register") {
        val body = call.receive<RegisterRequest>()
        if (body.role !in setOf("GUARDIAN", "WARD")) {
            call.respond(HttpStatusCode.BadRequest, "role must be GUARDIAN or WARD")
            return@post
        }
        if (body.password.length < 8) {
            call.respond(HttpStatusCode.BadRequest, "password must be at least 8 characters")
            return@post
        }

        val existing = transaction { Users.select { Users.email eq body.email }.singleOrNull() }
        if (existing != null) {
            call.respond(HttpStatusCode.Conflict, "email already registered")
            return@post
        }

        val userId = transaction {
            Users.insertAndGetId {
                it[email] = body.email
                it[passwordHash] = PasswordHasher.hash(body.password)
                it[role] = body.role
                it[createdAt] = Instant.now()
            }
        }

        val token = jwtService.issueToken(userId.value, body.role)
        call.respond(HttpStatusCode.Created, AuthResponse(token, userId.value.toString(), body.role))
    }

    post("/v1/auth/login") {
        val body = call.receive<LoginRequest>()
        val row = transaction { Users.select { Users.email eq body.email }.singleOrNull() }
        if (row == null || !PasswordHasher.verify(body.password, row[Users.passwordHash])) {
            call.respond(HttpStatusCode.Unauthorized, "invalid credentials")
            return@post
        }

        val token = jwtService.issueToken(row[Users.id].value, row[Users.role])
        call.respond(AuthResponse(token, row[Users.id].value.toString(), row[Users.role]))
    }
}
