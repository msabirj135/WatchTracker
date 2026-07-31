package com.sabir.watchtracker.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.sabir.watchtracker.data.local.LibraryStatus
import com.sabir.watchtracker.data.remote.TmdbEpisode
import com.sabir.watchtracker.data.remote.TmdbSearchResult
import com.sabir.watchtracker.data.remote.TmdbSeasonDetails
import com.sabir.watchtracker.data.remote.TmdbTvDetails
import com.sabir.watchtracker.data.repository.LibraryRepository
import com.sabir.watchtracker.data.repository.TmdbRepository
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<TmdbSearchResult> = emptyList(),
    val errorMessage: String? = null,
    val hasSearched: Boolean = false,
    val isSaving: Boolean = false,
    val saveMessage: String? = null,
    val saveErrorMessage: String? = null,
    val lastSavedItemKey: String? = null,
    val tvDetails: TmdbTvDetails? = null,
    val seasonDetails: TmdbSeasonDetails? = null,
    val isLoadingTvDetails: Boolean = false,
    val isLoadingSeason: Boolean = false,
    val tvDetailsErrorMessage: String? = null
)

class SearchViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val tmdbRepository = TmdbRepository()

    private val libraryRepository = LibraryRepository(
        context = application.applicationContext
    )

    private val coroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    )

    private var searchJob: Job? = null
    private var saveJob: Job? = null
    private var tvDetailsJob: Job? = null
    private var seasonJob: Job? = null

    var uiState = androidx.compose.runtime.mutableStateOf(
        SearchUiState()
    )
        private set

    fun updateQuery(
        query: String
    ) {
        uiState.value = uiState.value.copy(
            query = query,
            errorMessage = null
        )
    }

    fun search() {
        val query = uiState.value.query.trim()

        if (query.length < 2) {
            uiState.value = uiState.value.copy(
                isLoading = false,
                results = emptyList(),
                errorMessage = "Enter at least two characters.",
                hasSearched = false
            )
            return
        }

        searchJob?.cancel()

        uiState.value = uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            hasSearched = true
        )

        searchJob = coroutineScope.launch {
            try {
                val results = tmdbRepository
                    .searchMoviesAndShows(query)

                uiState.value = uiState.value.copy(
                    isLoading = false,
                    results = results,
                    errorMessage = null
                )
            } catch (exception: HttpException) {
                val message = when (exception.code()) {
                    401 -> {
                        "TMDB rejected the access token. Check local.properties."
                    }

                    404 -> {
                        "The requested TMDB resource could not be found."
                    }

                    429 -> {
                        "Too many requests. Please wait and try again."
                    }

                    else -> {
                        "TMDB request failed with error ${exception.code()}."
                    }
                }

                uiState.value = uiState.value.copy(
                    isLoading = false,
                    results = emptyList(),
                    errorMessage = message
                )
            } catch (exception: IOException) {
                uiState.value = uiState.value.copy(
                    isLoading = false,
                    results = emptyList(),
                    errorMessage = "Unable to connect. Check your internet connection."
                )
            } catch (exception: Exception) {
                uiState.value = uiState.value.copy(
                    isLoading = false,
                    results = emptyList(),
                    errorMessage = exception.message
                        ?: "An unexpected error occurred."
                )
            }
        }
    }

    fun prepareResult(
        result: TmdbSearchResult
    ) {
        tvDetailsJob?.cancel()
        seasonJob?.cancel()

        uiState.value = uiState.value.copy(
            tvDetails = null,
            seasonDetails = null,
            isLoadingTvDetails =
                result.mediaType == "tv",
            isLoadingSeason = false,
            tvDetailsErrorMessage = null
        )

        if (result.mediaType != "tv") {
            return
        }

        tvDetailsJob = coroutineScope.launch {
            try {
                val tvDetails = tmdbRepository.getTvDetails(
                    seriesId = result.id
                )

                uiState.value = uiState.value.copy(
                    tvDetails = tvDetails,
                    isLoadingTvDetails = false,
                    tvDetailsErrorMessage = null
                )

                val firstSeason = tvDetails
                    .regularSeasons
                    .firstOrNull()

                if (firstSeason != null) {
                    loadSeason(
                        seriesId = result.id,
                        seasonNumber =
                            firstSeason.seasonNumber
                    )
                }
            } catch (exception: Exception) {
                uiState.value = uiState.value.copy(
                    tvDetails = null,
                    seasonDetails = null,
                    isLoadingTvDetails = false,
                    isLoadingSeason = false,
                    tvDetailsErrorMessage =
                        exception.message
                            ?: "Unable to load TV-show details."
                )
            }
        }
    }

    fun loadSeason(
        seriesId: Int,
        seasonNumber: Int
    ) {
        seasonJob?.cancel()

        uiState.value = uiState.value.copy(
            seasonDetails = null,
            isLoadingSeason = true,
            tvDetailsErrorMessage = null
        )

        seasonJob = coroutineScope.launch {
            try {
                val seasonDetails =
                    tmdbRepository.getTvSeasonDetails(
                        seriesId = seriesId,
                        seasonNumber = seasonNumber
                    )

                uiState.value = uiState.value.copy(
                    seasonDetails = seasonDetails,
                    isLoadingSeason = false,
                    tvDetailsErrorMessage = null
                )
            } catch (exception: Exception) {
                uiState.value = uiState.value.copy(
                    seasonDetails = null,
                    isLoadingSeason = false,
                    tvDetailsErrorMessage =
                        exception.message
                            ?: "Unable to load season episodes."
                )
            }
        }
    }

    fun saveToLibrary(
        result: TmdbSearchResult,
        status: LibraryStatus,
        watchDateEpochDay: Long?,
        personalRating: Double?,
        notes: String,
        selectedEpisode: TmdbEpisode?
    ) {
        if (uiState.value.isSaving) {
            return
        }

        saveJob?.cancel()

        uiState.value = uiState.value.copy(
            isSaving = true,
            saveMessage = null,
            saveErrorMessage = null,
            lastSavedItemKey = null
        )

        saveJob = coroutineScope.launch {
            try {
                libraryRepository.saveSearchResult(
                    result = result,
                    status = status,
                    watchDateEpochDay = watchDateEpochDay,
                    personalRating = personalRating,
                    notes = notes,
                    currentSeason =
                        selectedEpisode?.seasonNumber,
                    currentEpisode =
                        selectedEpisode?.episodeNumber,
                    totalSeasons =
                        uiState.value
                            .tvDetails
                            ?.numberOfSeasons,
                    totalEpisodes =
                        uiState.value
                            .tvDetails
                            ?.numberOfEpisodes
                )

                if (
                    result.mediaType == "tv" &&
                    selectedEpisode != null &&
                    watchDateEpochDay != null
                ) {
                    val savedShow =
                        libraryRepository.getItem(
                            tmdbId = result.id,
                            mediaType = "tv"
                        )

                    if (savedShow != null) {
                        libraryRepository.markEpisodeWatched(
                            show = savedShow,
                            episode = selectedEpisode,
                            watchedDateEpochDay =
                                watchDateEpochDay
                        )
                    }
                }

                uiState.value = uiState.value.copy(
                    isSaving = false,
                    saveMessage =
                        "${result.displayTitle} added to your library.",
                    saveErrorMessage = null,
                    lastSavedItemKey =
                        "${result.mediaType}-${result.id}"
                )
            } catch (exception: Exception) {
                uiState.value = uiState.value.copy(
                    isSaving = false,
                    saveMessage = null,
                    saveErrorMessage = exception.message
                        ?: "Unable to save this title."
                )
            }
        }
    }

    fun clearPreparedResult() {
        tvDetailsJob?.cancel()
        seasonJob?.cancel()

        uiState.value = uiState.value.copy(
            tvDetails = null,
            seasonDetails = null,
            isLoadingTvDetails = false,
            isLoadingSeason = false,
            tvDetailsErrorMessage = null
        )
    }

    fun clearSaveFeedback() {
        uiState.value = uiState.value.copy(
            saveMessage = null,
            saveErrorMessage = null,
            lastSavedItemKey = null
        )
    }

    fun clearSearch() {
        searchJob?.cancel()
        tvDetailsJob?.cancel()
        seasonJob?.cancel()

        uiState.value = SearchUiState()
    }

    override fun onCleared() {
        searchJob?.cancel()
        saveJob?.cancel()
        tvDetailsJob?.cancel()
        seasonJob?.cancel()
        coroutineScope.cancel()
        super.onCleared()
    }
}

