package com.sabir.watchtracker.data.repository

import com.sabir.watchtracker.data.remote.TmdbApiClient
import com.sabir.watchtracker.data.remote.TmdbSearchResult
import com.sabir.watchtracker.data.remote.TmdbMovieDetails
import com.sabir.watchtracker.data.remote.TmdbSeasonDetails
import com.sabir.watchtracker.data.remote.TmdbTvDetails
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class TmdbRepository {

    private val apiService = TmdbApiClient.service

    suspend fun getMovieDetails(movieId: Int): TmdbMovieDetails {
        return apiService.getMovieDetails(movieId)
    }

    suspend fun searchMoviesAndShows(
        query: String
    ): List<TmdbSearchResult> {
        val normalizedQuery = query.trim()

        if (normalizedQuery.isBlank()) {
            return emptyList()
        }

        if (IMDB_ID_PATTERN.matches(normalizedQuery)) {
            val response = apiService.findByExternalId(
                externalId = normalizedQuery.lowercase()
            )

            return (response.movieResults.map { result ->
                result.copy(mediaType = "movie")
            } + response.tvResults.map { result ->
                result.copy(mediaType = "tv")
            }).distinctBy { result ->
                result.mediaType to result.id
            }
        }

        val parsedQuery = parseQuery(normalizedQuery)
        val rawResults = if (parsedQuery.year != null) {
            coroutineScope {
                val moviesRequest = async {
                    apiService.searchMovies(
                        query = parsedQuery.title,
                        releaseYear = parsedQuery.year
                    ).results.map { result ->
                        result.copy(mediaType = "movie")
                    }
                }
                val tvShowsRequest = async {
                    apiService.searchTvShows(
                        query = parsedQuery.title,
                        firstAirDateYear = parsedQuery.year
                    ).results.map { result ->
                        result.copy(mediaType = "tv")
                    }
                }

                moviesRequest.await() + tvShowsRequest.await()
            }
        } else {
            loadGeneralSearchPages(parsedQuery.title)
        }

        return rawResults
            .filter { result ->
                result.mediaType == "movie" ||
                    result.mediaType == "tv"
            }
            .distinctBy { result ->
                result.mediaType to result.id
            }
            .mapIndexed { index, result ->
                RankedSearchResult(
                    result = result,
                    originalIndex = index,
                    score = rankingScore(
                        result = result,
                        requestedTitle = parsedQuery.title,
                        requestedYear = parsedQuery.year
                    )
                )
            }
            .sortedWith(
                compareByDescending<RankedSearchResult> { ranked ->
                    ranked.score
                }.thenBy { ranked -> ranked.originalIndex }
            )
            .map { ranked -> ranked.result }
    }

    private suspend fun loadGeneralSearchPages(
        title: String
    ): List<TmdbSearchResult> {
        val firstPage = apiService.searchMoviesAndShows(
            query = title,
            page = 1
        )
        val lastPage = firstPage.totalPages
            .coerceAtMost(MAXIMUM_SEARCH_PAGES)

        val additionalResults = if (lastPage > 1) {
            coroutineScope {
                (2..lastPage).map { page ->
                    async {
                        apiService.searchMoviesAndShows(
                            query = title,
                            page = page
                        ).results
                    }
                }.map { request -> request.await() }
                    .flatten()
            }
        } else {
            emptyList()
        }

        return firstPage.results + additionalResults
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

    private fun parseQuery(query: String): ParsedSearchQuery {
        val match = TITLE_WITH_YEAR_PATTERN.matchEntire(query)
            ?: return ParsedSearchQuery(title = query, year = null)

        val title = match.groupValues[1]
            .trim()
            .ifBlank { query }
        val year = match.groupValues[2].toIntOrNull()

        return ParsedSearchQuery(title = title, year = year)
    }

    private fun rankingScore(
        result: TmdbSearchResult,
        requestedTitle: String,
        requestedYear: Int?
    ): Int {
        val normalizedRequestedTitle = normalizeTitle(requestedTitle)
        val candidateTitles = listOfNotNull(
            result.movieTitle,
            result.showTitle,
            result.originalMovieTitle,
            result.originalShowTitle
        ).map(::normalizeTitle)

        val titleScore = when {
            candidateTitles.any { title ->
                title == normalizedRequestedTitle
            } -> 1_000

            candidateTitles.any { title ->
                title.startsWith(normalizedRequestedTitle)
            } -> 500

            candidateTitles.any { title ->
                normalizedRequestedTitle in title
            } -> 250

            else -> 0
        }

        val yearScore = if (
            requestedYear != null &&
            result.displayYear.toIntOrNull() == requestedYear
        ) {
            200
        } else {
            0
        }

        return titleScore + yearScore
    }

    private fun normalizeTitle(title: String): String {
        return title
            .trim()
            .lowercase()
            .replace(NON_ALPHANUMERIC_PATTERN, "")
    }

    private data class ParsedSearchQuery(
        val title: String,
        val year: Int?
    )

    private data class RankedSearchResult(
        val result: TmdbSearchResult,
        val originalIndex: Int,
        val score: Int
    )

    private companion object {
        const val MAXIMUM_SEARCH_PAGES = 5

        val IMDB_ID_PATTERN = Regex(
            pattern = "^tt\\d{7,10}$",
            option = RegexOption.IGNORE_CASE
        )

        val TITLE_WITH_YEAR_PATTERN = Regex(
            pattern = "^(.+?)\\s*\\(?((?:19|20)\\d{2})\\)?$"
        )

        val NON_ALPHANUMERIC_PATTERN = Regex("[^\\p{L}\\p{N}]+")
    }
}
