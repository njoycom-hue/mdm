package com.safecircle.app.network

import android.content.Context
import com.safecircle.app.auth.TokenStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class ApiClient(context: Context) {

    // OCI 서버(duruone과 동일 인스턴스)에 mdm.duruone.com으로 nginx 리버스 프록시 연결됨
    // (.github/workflows/deploy-oci-nginx-setup.yml 참고)
    private val baseUrl = "https://mdm.duruone.com"

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(TokenStore(context)))
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .build()

    val service: ApiService = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(ApiService::class.java)

    companion object {
        @Volatile private var instance: ApiClient? = null

        fun get(context: Context): ApiClient =
            instance ?: synchronized(this) {
                instance ?: ApiClient(context.applicationContext).also { instance = it }
            }
    }
}
