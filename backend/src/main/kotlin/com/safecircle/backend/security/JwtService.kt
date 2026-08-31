package com.safecircle.backend.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit

class JwtService(private val secret: String, private val issuer: String, private val audience: String) {

    val algorithm: Algorithm = Algorithm.HMAC256(secret)

    fun issueToken(userId: UUID, role: String): String =
        JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("userId", userId.toString())
            .withClaim("role", role)
            .withExpiresAt(Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30)))
            .sign(algorithm)
}
