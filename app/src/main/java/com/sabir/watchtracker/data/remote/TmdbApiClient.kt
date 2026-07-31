package com.sabir.watchtracker.data.remote

import com.sabir.watchtracker.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private const val TMDB_BASE_URL = "https://api.themoviedb.org/"

object TmdbApiClient {

    private val authenticationInterceptor = Interceptor { chain ->
        val authenticatedRequest = chain
            .request()
            .newBuilder()
            .header(
                name = "Authorization",
                value = "Bearer ${BuildConfig.TMDB_ACCESS_TOKEN}"
            )
            .header(
                name = "Accept",
                value = "application/json"
            )
            .build()

        chain.proceed(authenticatedRequest)
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authenticationInterceptor)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(TMDB_BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val service: TmdbApiService by lazy {
        retrofit.create(TmdbApiService::class.java)
    }
}