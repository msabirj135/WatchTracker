package com.sabir.watchtracker.data.remote

import com.sabir.watchtracker.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object TmdbApiClient {

    private val authenticationInterceptor = Interceptor { chain ->
        val authenticatedRequest = chain
            .request()
            .newBuilder()
            .header(
                name = "X-WatchTracker-Key",
                value = BuildConfig.WATCHTRACKER_APP_KEY
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
            .baseUrl(
                "${BuildConfig.TMDB_PROXY_BASE_URL.trimEnd('/')}/"
            )
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val service: TmdbApiService by lazy {
        retrofit.create(TmdbApiService::class.java)
    }
}
