package com.shellytask.app.gallery.api

import com.shellytask.app.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Response as RetrofitResponse

interface UnsplashApi {
    @GET("photos")
    suspend fun listPhotos(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): RetrofitResponse<List<UnsplashPhoto>>

    companion object {
        private const val BASE_URL = "https://api.unsplash.com/"

        fun create(): UnsplashApi {
            val authInterceptor = object : Interceptor {
                override fun intercept(chain: Interceptor.Chain): Response {
                    val request = chain.request().newBuilder()
                        .addHeader("Authorization", "Client-ID ${BuildConfig.UNSPLASH_ACCESS_KEY}")
                        .build()
                    return chain.proceed(request)
                }
            }

            val logger = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(logger)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(UnsplashApi::class.java)
        }
    }
}