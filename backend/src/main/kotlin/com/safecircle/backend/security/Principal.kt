package com.safecircle.backend.security

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import java.util.UUID

data class UserPrincipal(val userId: UUID, val role: String)

fun ApplicationCall.userPrincipal(): UserPrincipal {
    val jwt = principal<JWTPrincipal>() ?: error("Missing JWT principal — route must be inside authenticate(\"auth-jwt\")")
    val userId = UUID.fromString(jwt.payload.getClaim("userId").asString())
    val role = jwt.payload.getClaim("role").asString()
    return UserPrincipal(userId, role)
}

class ForbiddenRoleException(message: String) : RuntimeException(message)

fun UserPrincipal.requireRole(expected: String) {
    if (role != expected) throw ForbiddenRoleException("$expected role required, got $role")
}
