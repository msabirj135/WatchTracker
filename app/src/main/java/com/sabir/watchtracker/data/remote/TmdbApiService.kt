package com.sabir.watchtracker.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApiService {

    @GET("3/movie/{movieId}")
    suspend fun getMovieDetails(
        @Path("movieId") movieId: Int,
        @Query("language") language: String = "en-US"
    ): TmdbMovieDetails

    @GET("3/search/multi")
    suspend fun searchMoviesAndShows(
        @Query("query")
        query: String,

        @Query("include_adult")
        includeAdult: Boolean = false,

        @Query("language")
        language: String = "en-US",

        @Query("page")
        page: Int = 1
    ): TmdbSearchResponse

    @GET("3/tv/{seriesId}")
    suspend fun getTvDetails(
        @Path("seriesId")
        seriesId: Int,

        @Query("language")
        language: String = "en-US"
    ): TmdbTvDetails

    @GET("3/tv/{seriesId}/season/{seasonNumber}")
    suspend fun getTvSeasonDetails(
        @Path("seriesId")
        seriesId: Int,

        @Path("seasonNumber")
        seasonNumber: Int,

        @Query("language")
        language: String = "en-US"
    ): TmdbSeasonDetails
}
