package com.safecircle.app.network

import com.safecircle.app.network.dto.ActivityAlertSummary
import com.safecircle.app.network.dto.ActivityEventRequest
import com.safecircle.app.network.dto.AppTimeLimitSummary
import com.safecircle.app.network.dto.AppUsageSummary
import com.safecircle.app.network.dto.AuthResponse
import com.safecircle.app.network.dto.BatchUploadRequest
import com.safecircle.app.network.dto.ClaimPairingRequest
import com.safecircle.app.network.dto.ConsentEventRequest
import com.safecircle.app.network.dto.InstalledAppDto
import com.safecircle.app.network.dto.KeywordAlertSummary
import com.safecircle.app.network.dto.LoginRequest
import com.safecircle.app.network.dto.PairingCodeResponse
import com.safecircle.app.network.dto.RegisterFcmTokenRequest
import com.safecircle.app.network.dto.RegisterRequest
import com.safecircle.app.network.dto.WardSettingsRequest
import com.safecircle.app.network.dto.WardSettingsResponse
import com.safecircle.app.network.dto.WardSummary
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

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

    @GET("/v1/settings/ward/{wardId}")
    suspend fun wardSettings(@Path("wardId") wardId: String): WardSettingsResponse

    @PUT("/v1/settings/ward/{wardId}")
    suspend fun updateWardSettings(@Path("wardId") wardId: String, @Body body: WardSettingsRequest)

    @POST("/v1/events/batch")
    suspend fun uploadEvents(@Body body: BatchUploadRequest)

    @GET("/v1/guardian/wards")
    suspend fun myWards(): List<WardSummary>

    @GET("/v1/guardian/wards/{wardId}/alerts")
    suspend fun wardAlerts(@Path("wardId") wardId: String): List<KeywordAlertSummary>

    @GET("/v1/guardian/wards/{wardId}/usage")
    suspend fun wardUsage(@Path("wardId") wardId: String): List<AppUsageSummary>

    @GET("/v1/guardian/wards/{wardId}/time-limits")
    suspend fun wardTimeLimits(@Path("wardId") wardId: String): List<AppTimeLimitSummary>

    @PUT("/v1/guardian/wards/{wardId}/time-limits")
    suspend fun updateWardTimeLimits(@Path("wardId") wardId: String, @Body body: List<AppTimeLimitSummary>)

    @GET("/v1/guardian/wards/{wardId}/activity-alerts")
    suspend fun wardActivityAlerts(@Path("wardId") wardId: String): List<ActivityAlertSummary>

    @POST("/v1/events/activity")
    suspend fun reportActivityEvent(@Body body: ActivityEventRequest)

    @GET("/v1/usage/mine")
    suspend fun myUsage(): List<AppUsageSummary>

    @POST("/v1/apps/mine")
    suspend fun syncInstalledApps(@Body body: List<InstalledAppDto>)

    @GET("/v1/guardian/wards/{wardId}/installed-apps")
    suspend fun wardInstalledApps(@Path("wardId") wardId: String): List<InstalledAppDto>
}
