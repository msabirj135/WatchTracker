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

enum class SearchMediaFilter(
    val label: String
) {
    ALL("All"),
    MOVIES("Movies"),
    TV_SHOWS("TV Shows")
}

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
    val tvDetailsErrorMessage: String? = null,
    val mediaFilter: SearchMediaFilter = SearchMediaFilter.ALL,
    val yearFilter: Int? = null,
    val languageFilter: String? = null,
    val recentSearches: List<String> = emptyList(),
    val savedItemKeys: Set<String> = emptySet()
) {
    val availableYears: List<Int>
        get() = results
            .mapNotNull { result ->
                result.displayYear.toIntOrNull()
            }
            .distinct()
            .sortedDescending()

    val availableLanguages: List<Pair<String, String>>
        get() = results
            .mapNotNull { result ->
                result.originalLanguage
                    ?.trim()
                    ?.lowercase()
                    ?.takeIf { code -> code.isNotBlank() }
                    ?.let { code -> code to result.displayLanguage }
            }
            .distinctBy { (code, _) -> code }
            .sortedBy { (_, name) -> name }

    val filteredResults: List<TmdbSearchResult>
        get() = results.filter { result ->
            val matchesMediaType = when (mediaFilter) {
                SearchMediaFilter.ALL -> true
                SearchMediaFilter.MOVIES -> result.mediaType == "movie"
                SearchMediaFilter.TV_SHOWS -> result.mediaType == "tv"
            }

            val matchesYear = yearFilter == null ||
                result.displayYear.toIntOrNull() == yearFilter

            val matchesLanguage = languageFilter == null ||
                result.originalLanguage.equals(
                    languageFilter,
                    ignoreCase = true
                )

            matchesMediaType && matchesYear && matchesLanguage
        }
}

class SearchViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val tmdbRepository = TmdbRepository()

    private val libraryRepository = LibraryRepository(
        context = application.applicationContext
    )

    private val preferences = application.getSharedPreferences(
        "reeltick_search",
        android.content.Context.MODE_PRIVATE
    )

    private val coroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    )

    private var searchJob: Job? = null
    private var saveJob: Job? = null
    private var tvDetailsJob: Job? = null
    private var seasonJob: Job? = null

    var uiState = androidx.compose.runtime.mutableStateOf(
        SearchUiState(
            recentSearches = loadRecentSearches()
        )
    )
        private set

    init {
        coroutineScope.launch {
            libraryRepository.observeAll().collect { items ->
                uiState.value = uiState.value.copy(
                    savedItemKeys = items.map { item ->
                        "${item.mediaType}-${item.tmdbId}"
                    }.toSet()
                )
            }
        }
    }

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
            hasSearched = true,
            yearFilter = null,
            languageFilter = null
        )

        searchJob = coroutineScope.launch {
            try {
                val results = tmdbRepository
                    .searchMoviesAndShows(query)

                saveRecentSearch(query)

                uiState.value = uiState.value.copy(
                    isLoading = false,
                    results = results,
                    errorMessage = null,
                    recentSearches = loadRecentSearches()
                )
            } catch (exception: HttpException) {
                val message = when (exception.code()) {
                    401 -> {
                        "ReelTick proxy authentication failed. Check the app key configuration."
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

    fun searchRecent(query: String) {
        updateQuery(query)
        search()
    }

    fun updateMediaFilter(filter: SearchMediaFilter) {
        uiState.value = uiState.value.copy(
            mediaFilter = filter
        )
    }

    fun updateYearFilter(year: Int?) {
        uiState.value = uiState.value.copy(
            yearFilter = year
        )
    }

    fun updateLanguageFilter(languageCode: String?) {
        uiState.value = uiState.value.copy(
            languageFilter = languageCode
        )
    }

    fun clearResultFilters() {
        uiState.value = uiState.value.copy(
            mediaFilter = SearchMediaFilter.ALL,
            yearFilter = null,
            languageFilter = null
        )
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
        watchMethod: String?,
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
                val movieRuntime = if (result.mediaType == "movie") {
                    tmdbRepository
                        .getMovieDetails(result.id)
                        .runtime
                } else {
                    null
                }

                libraryRepository.saveSearchResult(
                    result = result,
                    status = status,
                    watchDateEpochDay = watchDateEpochDay,
                    personalRating = personalRating,
                    watchMethod = watchMethod,
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
                            ?.numberOfEpisodes,
                    runtimeMinutes = movieRuntime
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

        uiState.value = uiState.value.copy(
            query = "",
            isLoading = false,
            results = emptyList(),
            errorMessage = null,
            hasSearched = false,
            saveMessage = null,
            saveErrorMessage = null,
            lastSavedItemKey = null,
            tvDetails = null,
            seasonDetails = null,
            isLoadingTvDetails = false,
            isLoadingSeason = false,
            tvDetailsErrorMessage = null,
            mediaFilter = SearchMediaFilter.ALL,
            yearFilter = null,
            languageFilter = null
        )
    }

    fun clearRecentSearches() {
        preferences.edit()
            .remove("recent_queries")
            .apply()

        uiState.value = uiState.value.copy(
            recentSearches = emptyList()
        )
    }

    private fun saveRecentSearch(query: String) {
        val updated = listOf(query) + loadRecentSearches()
            .filterNot { existing ->
                existing.equals(query, ignoreCase = true)
            }

        preferences.edit()
            .putString(
                "recent_queries",
                updated.take(6).joinToString("\u001F")
            )
            .apply()
    }

    private fun loadRecentSearches(): List<String> {
        return preferences.getString(
            "recent_queries",
            null
        )
            ?.split("\u001F")
            ?.filter { query -> query.isNotBlank() }
            ?.take(6)
            ?: emptyList()
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

