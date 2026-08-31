package com.safecircle.backend

import com.safecircle.backend.db.DatabaseFactory
import com.safecircle.backend.routes.authRoutes
import com.safecircle.backend.routes.consentRoutes
import com.safecircle.backend.routes.eventRoutes
import com.safecircle.backend.routes.pairingRoutes
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing
import io.ktor.http.HttpStatusCode

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) { json() }
    install(CallLogging)
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: ${cause.message}", status = HttpStatusCode.InternalServerError)
        }
    }

    DatabaseFactory.init(this)
    // TODO: FcmService.init(serviceAccountPath) — 환경변수로 경로 주입, 로컬 개발 시 생략 가능

    routing {
        authRoutes()
        consentRoutes()
        pairingRoutes()
        eventRoutes()
    }
}
