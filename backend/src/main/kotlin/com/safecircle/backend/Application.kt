package com.safecircle.backend

import com.safecircle.backend.db.DatabaseFactory
import com.safecircle.backend.routes.authRoutes
import com.safecircle.backend.routes.consentRoutes
import com.safecircle.backend.routes.deviceRoutes
import com.safecircle.backend.routes.eventRoutes
import com.safecircle.backend.routes.guardianRoutes
import com.safecircle.backend.routes.installedAppsRoutes
import com.safecircle.backend.routes.pairingRoutes
import com.safecircle.backend.routes.settingsRoutes
import com.safecircle.backend.routes.usageRoutes
import com.safecircle.backend.security.JwtService
import com.safecircle.backend.services.FcmService
import com.auth0.jwt.JWT
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json

// EngineMain(비수동 embeddedServer가 아니라)을 써야 resources/application.conf의
// HOCON 설정(auth.jwtSecret 등)이 실제로 로드된다 — embeddedServer(Netty, module=...)를
// 직접 호출하면 application.conf를 아예 안 읽어서 배포 환경에서 "Property not found"로 죽는다.
fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    // ignoreUnknownKeys=true: 안드로이드 앱이 백엔드보다 먼저(또는 나중에) 배포되는 순간이
    // 항상 있을 수밖에 없다 — 요청 바디에 서버가 아직 모르는 필드가 하나만 섞여 있어도
    // kotlinx.serialization 기본값(엄격 모드)은 전체 요청을 500으로 실패시킨다. 실제로
    // profileType/watchedPackages 필드를 추가한 뒤 배포 전 잠깐 이 문제로 정책 저장이
    // 500으로 죽었다.
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
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
        get("/health") { call.respondText("OK") }

        authRoutes(jwtService)
        consentRoutes()
        pairingRoutes()
        eventRoutes()
        deviceRoutes()
        settingsRoutes()
        guardianRoutes()
        usageRoutes()
        installedAppsRoutes()
    }
}
