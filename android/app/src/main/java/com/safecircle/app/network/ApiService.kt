package com.safecircle.app.network

import com.safecircle.app.network.dto.AuthResponse
import com.safecircle.app.network.dto.BatchUploadRequest
import com.safecircle.app.network.dto.ClaimPairingRequest
import com.safecircle.app.network.dto.ConsentEventRequest
import com.safecircle.app.network.dto.LoginRequest
import com.safecircle.app.network.dto.PairingCodeResponse
import com.safecircle.app.network.dto.RegisterFcmTokenRequest
import com.safecircle.app.network.dto.RegisterRequest
import com.safecircle.app.network.dto.WardSettingsResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    @POST("/v1/auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("/v1/consents")
    suspend fun postConsent(@Body body: ConsentEventRequest)

    @POST("/v1/pairing/issue")
    suspend fun issuePairingCode(): PairingCodeResponse

    @POST("/v1/pairing/claim")
    suspend fun claimPairingCode(@Body body: ClaimPairingRequest)

    @POST("/v1/devices/fcm-token")
    suspend fun registerFcmToken(@Body body: RegisterFcmTokenRequest)

    @GET("/v1/settings/mine")
    suspend fun myWardSettings(): WardSettingsResponse

    @POST("/v1/events/batch")
    suspend fun uploadEvents(@Body body: BatchUploadRequest)
}
