package com.safecircle.app.network

import com.safecircle.app.network.dto.BatchUploadRequest
import com.safecircle.app.network.dto.ConsentEventDto
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("/v1/consents")
    suspend fun postConsent(@Body body: ConsentEventDto)

    @POST("/v1/events/batch")
    suspend fun uploadEvents(@Body body: BatchUploadRequest)

    // TODO: 페어링(POST /v1/pairing/claim), FCM 토큰 등록(POST /v1/devices/fcm-token) 추가
}
