package com.sabir.watchtracker.ui.library

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.sabir.watchtracker.data.local.EpisodeWatch
import com.sabir.watchtracker.data.local.LibraryItem
import com.sabir.watchtracker.data.local.LibraryStatus
import com.sabir.watchtracker.data.repository.LibraryRepository
import com.sabir.watchtracker.data.repository.TmdbRepository
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class WatchHistoryEntry(
    val key: String,
    val item: LibraryItem,
    val detailText: String,
    val watchedDateEpochDay: Long
)

data class LibraryUiState(
    val isLoading: Boolean = true,
    val items: List<LibraryItem> = emptyList(),
    val episodeWatches: List<EpisodeWatch> = emptyList(),
    val errorMessage: String? = null
) {
    val movies: List<LibraryItem>
        get() = items.filter { item ->
            item.mediaType == "movie"
        }

    val tvShows: List<LibraryItem>
        get() = items.filter { item ->
            item.mediaType == "tv"
        }

    val planToWatch: List<LibraryItem>
        get() = items.filter { item ->
            item.status == LibraryStatus.PLAN_TO_WATCH
        }

    val watching: List<LibraryItem>
        get() = items.filter { item ->
            item.status == LibraryStatus.WATCHING
        }

    val continueWatching: List<LibraryItem>
        get() = watching
            .filter { item -> item.mediaType == "tv" }
            .sortedWith(
                compareByDescending<LibraryItem> { item ->
                    item.watchDateEpochDay ?: Long.MIN_VALUE
                }.thenByDescending { item ->
                    item.updatedAt
                }
            )

    val completed: List<LibraryItem>
        get() = items.filter { item ->
            item.status == LibraryStatus.COMPLETED
        }

    val dropped: List<LibraryItem>
        get() = items.filter { item ->
            item.status == LibraryStatus.DROPPED
        }

    val watchHistoryEntries: List<WatchHistoryEntry>
        get() {
            val itemsByKey = items.associateBy { item ->
                item.mediaType to item.tmdbId
            }

            val movieEntries = movies.mapNotNull { movie ->
                val watchedDate = movie.watchDateEpochDay
                    ?: return@mapNotNull null

                WatchHistoryEntry(
                    key = "movie-${movie.tmdbId}-$watchedDate",
                    item = movie,
                    detailText = "Movie",
                    watchedDateEpochDay = watchedDate
                )
            }

            val episodeEntries = episodeWatches.mapNotNull { watch ->
                val show = itemsByKey["tv" to watch.tmdbShowId]
                    ?: return@mapNotNull null

                WatchHistoryEntry(
                    key = buildString {
                        append("episode-")
                        append(watch.tmdbShowId)
                        append("-")
                        append(watch.seasonNumber)
                        append("-")
                        append(watch.episodeNumber)
                    },
                    item = show,
                    detailText = buildString {
                        append(watch.episodeCode)
                        append(" • ")
                        append(watch.episodeName)
                    },
                    watchedDateEpochDay =
                        watch.watchedDateEpochDay
                )
            }

            return (movieEntries + episodeEntries)
                .sortedWith(
                    compareByDescending<WatchHistoryEntry> { entry ->
                        entry.watchedDateEpochDay
                    }.thenByDescending { entry ->
                        entry.item.updatedAt
                    }
                )
        }

    val recentlyAdded: List<LibraryItem>
        get() = items.sortedByDescending { item ->
            item.addedAt
        }

    fun watchedEpisodeCount(tmdbShowId: Int): Int {
        return episodeWatches.count { watch ->
            watch.tmdbShowId == tmdbShowId
        }
    }

    val movieCount: Int
        get() = movies.size

    val tvShowCount: Int
        get() = tvShows.size

    val completedCount: Int
        get() = completed.size

    val planToWatchCount: Int
        get() = planToWatch.size

    val droppedCount: Int
        get() = dropped.size

    val totalCount: Int
        get() = items.size

    val watchedMovies: List<LibraryItem>
        get() = movies.filter { it.watchDateEpochDay != null }

    val watchedMovieCount: Int
        get() = watchedMovies.size

    val watchedEpisodeCount: Int
        get() = episodeWatches.size

    val movieWatchMinutes: Int
        get() = watchedMovies.sumOf { it.runtimeMinutes ?: 0 }

    val tvWatchMinutes: Int
        get() = episodeWatches.sumOf { it.runtimeMinutes ?: 0 }

    val totalWatchMinutes: Int
        get() = movieWatchMinutes + tvWatchMinutes

    val thisMonthCount: Int
        get() {
            val firstDay = LocalDate.now()
                .withDayOfMonth(1)
                .toEpochDay()

            return watchedMovies.count {
                (it.watchDateEpochDay ?: Long.MIN_VALUE) >= firstDay
            } + episodeWatches.count {
                it.watchedDateEpochDay >= firstDay
            }
        }

    val averagePersonalRating: Double?
        get() = items.mapNotNull { it.personalRating }
            .takeIf { it.isNotEmpty() }
            ?.average()
}

class LibraryViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = LibraryRepository(
        context = application.applicationContext
    )

    private val tmdbRepository = TmdbRepository()

    private val attemptedRuntimeIds = mutableSetOf<Int>()

    private val coroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    )

    var uiState = mutableStateOf(
        LibraryUiState()
    )
        private set

    init {
        observeLibrary()
    }

    private fun observeLibrary() {
        coroutineScope.launch {
            combine(
                repository.observeAll(),
                repository.observeAllEpisodeWatches()
            ) { items, episodeWatches ->
                LibraryUiState(
                    isLoading = false,
                    items = items,
                    episodeWatches = episodeWatches,
                    errorMessage = null
                )
            }
                .catch { exception ->
                    emit(
                        LibraryUiState(
                            isLoading = false,
                            errorMessage = exception.message
                                ?: "Unable to load your library."
                        )
                    )
                }
                .collect { state ->
                    uiState.value = state
                    backfillMovieRuntimes(state.items)
                }
        }
    }

    private fun backfillMovieRuntimes(items: List<LibraryItem>) {
        items.filter { item ->
            item.mediaType == "movie" &&
                item.watchDateEpochDay != null &&
                item.runtimeMinutes == null &&
                attemptedRuntimeIds.add(item.tmdbId)
        }.forEach { movie ->
            coroutineScope.launch(Dispatchers.IO) {
                runCatching {
                    tmdbRepository.getMovieDetails(movie.tmdbId).runtime
                }.getOrNull()
                    ?.takeIf { it > 0 }
                    ?.let { runtime ->
                        repository.updateMovieRuntime(
                            tmdbId = movie.tmdbId,
                            runtimeMinutes = runtime
                        )
                    }
            }
        }
    }

    fun deleteItem(
        item: LibraryItem
    ) {
        coroutineScope.launch {
            try {
                repository.deleteItem(item)
            } catch (exception: Exception) {
                uiState.value = uiState.value.copy(
                    errorMessage = exception.message
                        ?: "Unable to delete this title."
                )
            }
        }
    }

    fun clearError() {
        uiState.value = uiState.value.copy(
            errorMessage = null
        )
    }

    override fun onCleared() {
        coroutineScope.cancel()
        super.onCleared()
    }
}
