package com.kittyai.pet.bedtime

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

/** Retrofit service interface for the Kitty bedtime story API. */
interface KittyApiService {

    @POST(ApiConfig.BEDTIME_ENDPOINT)
    suspend fun generateBedtimeStory(
        @Body request: BedtimeRequest
    ): Response<BedtimeResponse>

    @GET(ApiConfig.HEALTH_ENDPOINT)
    suspend fun healthCheck(): Response<HealthResponse>

    companion object {
        fun create(): KittyApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            return Retrofit.Builder()
                .baseUrl(ApiConfig.BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(KittyApiService::class.java)
        }
    }
}
