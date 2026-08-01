package com.sabir.watchtracker.ui.library

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sabir.watchtracker.data.local.EpisodeWatch
import com.sabir.watchtracker.data.local.LibraryItem
import com.sabir.watchtracker.data.local.RewatchRecord
import com.sabir.watchtracker.data.remote.TmdbEpisode
import com.sabir.watchtracker.data.remote.TmdbSeasonDetails
import com.sabir.watchtracker.data.remote.TmdbTvDetails
import com.sabir.watchtracker.data.repository.LibraryRepository
import com.sabir.watchtracker.data.repository.TmdbRepository
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

data class LibraryDetailUiState(
    val item: LibraryItem? = null,
    val tvDetails: TmdbTvDetails? = null,
    val seasons: List<TmdbSeasonDetails> = emptyList(),
    val episodeWatches: List<EpisodeWatch> = emptyList(),
    val rewatchRecords: List<RewatchRecord> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
) {
    val allEpisodes: List<TmdbEpisode>
        get() = seasons
            .sortedBy { season -> season.seasonNumber }
            .flatMap { season ->
                season.episodes.sortedBy { episode ->
                    episode.episodeNumber
                }
            }

    val watchedEpisodeKeys: Set<Pair<Int, Int>>
        get() = episodeWatches
            .map { watch ->
                watch.seasonNumber to watch.episodeNumber
            }
            .toSet()

    val watchedCount: Int
        get() = episodeWatches.size

    val airedEpisodes: List<TmdbEpisode>
        get() = allEpisodes.filter { episode -> episode.hasAired }

    val availableEpisodeCount: Int
        get() = airedEpisodes.size

    val totalEpisodeCount: Int
        get() = allEpisodes.size
            .takeIf { count -> count > 0 }
            ?: item?.totalEpisodes
            ?: 0

    val progress: Float
        get() = if (totalEpisodeCount > 0) {
            watchedCount.toFloat() / totalEpisodeCount.toFloat()
        } else {
            0f
        }

    val nextEpisode: TmdbEpisode?
        get() = airedEpisodes.firstOrNull { episode ->
            (episode.seasonNumber to episode.episodeNumber) !in
                watchedEpisodeKeys
        }

    val nextUpcomingEpisode: TmdbEpisode?
        get() = allEpisodes
            .filter { episode -> !episode.hasAired }
            .sortedBy { episode -> episode.parsedAirDate }
            .firstOrNull()

    val isCaughtUp: Boolean
        get() = episodeWatches.isNotEmpty() && nextEpisode == null
}

class LibraryDetailViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val libraryRepository = LibraryRepository(
        context = application.applicationContext
    )

    private val tmdbRepository = TmdbRepository()

    private var episodeObservationJob: Job? = null
    private var rewatchObservationJob: Job? = null
    private var loadedItemKey: String? = null

    var uiState = mutableStateOf(
        LibraryDetailUiState()
    )
        private set

    fun loadItem(item: LibraryItem) {
        val itemKey = "${item.mediaType}-${item.tmdbId}"

        if (loadedItemKey == itemKey) {
            uiState.value = uiState.value.copy(
                item = item
            )
            return
        }

        loadedItemKey = itemKey
        episodeObservationJob?.cancel()
        rewatchObservationJob?.cancel()

        uiState.value = LibraryDetailUiState(
            item = item,
            isLoading = item.mediaType == "tv"
        )

        observeRewatches(item)

        if (item.mediaType == "tv") {
            observeEpisodes(item.tmdbId)
            loadTvDetails(item)
        }
    }

    private fun observeRewatches(item: LibraryItem) {
        rewatchObservationJob = viewModelScope.launch {
            libraryRepository.observeRewatches(
                tmdbId = item.tmdbId,
                mediaType = item.mediaType
            ).collect { records ->
                uiState.value = uiState.value.copy(
                    rewatchRecords = records
                )
            }
        }
    }

    private fun observeEpisodes(tmdbShowId: Int) {
        episodeObservationJob = viewModelScope.launch {
            libraryRepository
                .observeEpisodeWatches(tmdbShowId)
                .collect { watches ->
                    uiState.value = uiState.value.copy(
                        episodeWatches = watches
                    )
                }
        }
    }

    private fun loadTvDetails(item: LibraryItem) {
        viewModelScope.launch {
            try {
                val details = tmdbRepository.getTvDetails(
                    seriesId = item.tmdbId
                )

                val seasons = details.regularSeasons
                    .map { season ->
                        async {
                            tmdbRepository.getTvSeasonDetails(
                                seriesId = item.tmdbId,
                                seasonNumber = season.seasonNumber
                            )
                        }
                    }
                    .awaitAll()
                    .sortedBy { season ->
                        season.seasonNumber
                    }

                libraryRepository.updateTvMetadata(
                    tmdbShowId = item.tmdbId,
                    totalSeasons = details.regularSeasons.size,
                    totalEpisodes = seasons.sumOf { season ->
                        season.episodes.size
                    }
                )

                libraryRepository.synchronizeTvProgress(
                    tmdbShowId = item.tmdbId
                )

                refreshItem()

                uiState.value = uiState.value.copy(
                    tvDetails = details,
                    seasons = seasons,
                    isLoading = false,
                    errorMessage = null
                )
            } catch (exception: Exception) {
                uiState.value = uiState.value.copy(
                    isLoading = false,
                    errorMessage = exception.message
                        ?: "Unable to load the episode list."
                )
            }
        }
    }

    fun retryLoadTvDetails() {
        val item = uiState.value.item
            ?.takeIf { current -> current.mediaType == "tv" }
            ?: return

        uiState.value = uiState.value.copy(
            isLoading = true,
            errorMessage = null
        )
        loadTvDetails(item)
    }

    fun markEpisodeWatched(
        episode: TmdbEpisode,
        watchedDateEpochDay: Long
    ) {
        val item = uiState.value.item ?: return

        viewModelScope.launch {
            setSaving(true)

            try {
                libraryRepository.markEpisodeWatched(
                    show = item,
                    episode = episode,
                    watchedDateEpochDay = watchedDateEpochDay
                )

                refreshItem()
                setSaving(false)
            } catch (exception: Exception) {
                setError(
                    exception.message
                        ?: "Unable to save this episode."
                )
            }
        }
    }

    fun markNextEpisodeWatched() {
        val nextEpisode = uiState.value.nextEpisode
            ?: return

        markEpisodeWatched(
            episode = nextEpisode,
            watchedDateEpochDay = LocalDate.now().toEpochDay()
        )
    }

    fun markSeriesCompleted() {
        val state = uiState.value
        val show = state.item ?: return
        val remainingEpisodes = state.airedEpisodes.filter { episode ->
            (episode.seasonNumber to episode.episodeNumber) !in
                state.watchedEpisodeKeys
        }

        if (remainingEpisodes.isEmpty()) return

        viewModelScope.launch {
            setSaving(true)
            try {
                libraryRepository.markEpisodesWatched(
                    show = show,
                    episodes = remainingEpisodes,
                    watchedDateEpochDay = LocalDate.now().toEpochDay()
                )
                refreshItem()
                setSaving(false)
            } catch (exception: Exception) {
                setError(
                    exception.message
                        ?: "Unable to mark this series completed."
                )
            }
        }
    }

    fun markSeasonWatched(
        season: TmdbSeasonDetails,
        watchedDateEpochDay: Long
    ) {
        val state = uiState.value
        val show = state.item ?: return
        val remainingEpisodes = season.episodes.filter { episode ->
            episode.hasAired &&
                (episode.seasonNumber to episode.episodeNumber) !in
                    state.watchedEpisodeKeys
        }

        if (remainingEpisodes.isEmpty()) return

        viewModelScope.launch {
            setSaving(true)
            try {
                libraryRepository.markEpisodesWatched(
                    show = show,
                    episodes = remainingEpisodes,
                    watchedDateEpochDay = watchedDateEpochDay
                )
                refreshItem()
                setSaving(false)
            } catch (exception: Exception) {
                setError(
                    exception.message
                        ?: "Unable to mark this season watched."
                )
            }
        }
    }

    fun unmarkEpisodeWatched(
        episode: TmdbEpisode
    ) {
        val item = uiState.value.item ?: return

        viewModelScope.launch {
            setSaving(true)

            try {
                libraryRepository.unmarkEpisodeWatched(
                    show = item,
                    seasonNumber = episode.seasonNumber,
                    episodeNumber = episode.episodeNumber
                )

                refreshItem()
                setSaving(false)
            } catch (exception: Exception) {
                setError(
                    exception.message
                        ?: "Unable to update this episode."
                )
            }
        }
    }

    fun updateMovieWatchDate(
        watchedDateEpochDay: Long?
    ) {
        val movie = uiState.value.item ?: return

        viewModelScope.launch {
            setSaving(true)

            try {
                libraryRepository.updateMovieWatchDate(
                    movie = movie,
                    watchedDateEpochDay = watchedDateEpochDay
                )

                refreshItem()
                setSaving(false)
            } catch (exception: Exception) {
                setError(
                    exception.message
                        ?: "Unable to update the watched date."
                )
            }
        }
    }

    fun addMovieRewatch(watchedDateEpochDay: Long) {
        val movie = uiState.value.item ?: return

        viewModelScope.launch {
            setSaving(true)
            try {
                libraryRepository.addMovieRewatch(
                    movie = movie,
                    watchedDateEpochDay = watchedDateEpochDay
                )
                refreshItem()
                setSaving(false)
            } catch (exception: Exception) {
                setError(
                    exception.message ?: "Unable to save this rewatch."
                )
            }
        }
    }

    fun addEpisodeRewatch(
        episode: TmdbEpisode,
        watchedDateEpochDay: Long
    ) {
        val show = uiState.value.item ?: return

        viewModelScope.launch {
            setSaving(true)
            try {
                libraryRepository.addEpisodeRewatch(
                    show = show,
                    episode = episode,
                    watchedDateEpochDay = watchedDateEpochDay
                )
                refreshItem()
                setSaving(false)
            } catch (exception: Exception) {
                setError(
                    exception.message ?: "Unable to save this episode rewatch."
                )
            }
        }
    }

    fun deleteRewatch(recordId: Long) {
        viewModelScope.launch {
            setSaving(true)
            try {
                libraryRepository.deleteRewatch(recordId)
                setSaving(false)
            } catch (exception: Exception) {
                setError(
                    exception.message ?: "Unable to remove this rewatch."
                )
            }
        }
    }

    fun updatePersonalRating(rating: Double?) {
        val item = uiState.value.item ?: return

        viewModelScope.launch {
            setSaving(true)

            try {
                libraryRepository.updateItem(
                    item.copy(personalRating = rating)
                )
                refreshItem()
                setSaving(false)
            } catch (exception: Exception) {
                setError(
                    exception.message
                        ?: "Unable to update your rating."
                )
            }
        }
    }

    private suspend fun refreshItem() {
        val currentItem = uiState.value.item ?: return

        val refreshedItem = libraryRepository.getItem(
            tmdbId = currentItem.tmdbId,
            mediaType = currentItem.mediaType
        )

        uiState.value = uiState.value.copy(
            item = refreshedItem ?: currentItem
        )
    }

    private fun setSaving(isSaving: Boolean) {
        uiState.value = uiState.value.copy(
            isSaving = isSaving,
            errorMessage = null
        )
    }

    private fun setError(message: String) {
        uiState.value = uiState.value.copy(
            isSaving = false,
            errorMessage = message
        )
    }

    fun clearError() {
        uiState.value = uiState.value.copy(
            errorMessage = null
        )
    }
}
