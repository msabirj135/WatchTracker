package com.sabir.watchtracker.data.repository

import com.sabir.watchtracker.data.remote.TmdbApiClient
import com.sabir.watchtracker.data.remote.TmdbSearchResult
import com.sabir.watchtracker.data.remote.TmdbSeasonDetails
import com.sabir.watchtracker.data.remote.TmdbTvDetails

class TmdbRepository {

    private val apiService = TmdbApiClient.service

    suspend fun searchMoviesAndShows(
        query: String
    ): List<TmdbSearchResult> {
        val normalizedQuery = query.trim()

        if (normalizedQuery.isBlank()) {
            return emptyList()
        }

        return apiService
            .searchMoviesAndShows(
                query = normalizedQuery
            )
            .results
            .filter { result ->
                result.mediaType == "movie" ||
                    result.mediaType == "tv"
            }
    }

    suspend fun getTvDetails(
        seriesId: Int
    ): TmdbTvDetails {
        return apiService.getTvDetails(
            seriesId = seriesId
        )
    }

    suspend fun getTvSeasonDetails(
        seriesId: Int,
        seasonNumber: Int
    ): TmdbSeasonDetails {
        return apiService.getTvSeasonDetails(
            seriesId = seriesId,
            seasonNumber = seasonNumber
        )
    }
}
