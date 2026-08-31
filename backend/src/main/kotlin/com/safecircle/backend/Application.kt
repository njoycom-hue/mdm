package com.safecircle.backend

import com.safecircle.backend.db.DatabaseFactory
import com.safecircle.backend.routes.authRoutes
import com.safecircle.backend.routes.consentRoutes
import com.safecircle.backend.routes.deviceRoutes
import com.safecircle.backend.routes.eventRoutes
import com.safecircle.backend.routes.pairingRoutes
import com.safecircle.backend.routes.settingsRoutes
import com.safecircle.backend.security.JwtService
import com.safecircle.backend.services.FcmService
import com.auth0.jwt.JWT
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) { json() }
    install(CallLogging)
    install(StatusPages) {
        exception<com.safecircle.backend.security.ForbiddenRoleException> { call, cause ->
            call.respondText(text = cause.message ?: "forbidden", status = HttpStatusCode.Forbidden)
        }
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: ${cause.message}", status = HttpStatusCode.InternalServerError)
        }
    }

    val jwtSecret = environment.config.property("auth.jwtSecret").getString()
    val jwtIssuer = environment.config.property("auth.jwtIssuer").getString()
    val jwtAudience = environment.config.property("auth.jwtAudience").getString()
    val jwtService = JwtService(jwtSecret, jwtIssuer, jwtAudience)

    install(Authentication) {
        jwt("auth-jwt") {
            verifier(
                JWT.require(jwtService.algorithm)
                    .withIssuer(jwtIssuer)
                    .withAudience(jwtAudience)
                    .build()
            )
            validate { credential -> JWTPrincipal(credential.payload) }
        }
    }

    DatabaseFactory.init(this)
    FcmService.init(System.getenv("FIREBASE_SERVICE_ACCOUNT_PATH"))

    routing {
        authRoutes(jwtService)
        consentRoutes()
        pairingRoutes()
        eventRoutes()
        deviceRoutes()
        settingsRoutes()
    }
}
