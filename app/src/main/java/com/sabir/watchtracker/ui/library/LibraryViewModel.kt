package com.sabir.watchtracker.ui.library

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.sabir.watchtracker.data.local.EpisodeWatch
import com.sabir.watchtracker.data.local.CustomList
import com.sabir.watchtracker.data.local.CustomListItem
import com.sabir.watchtracker.data.local.LibraryItem
import com.sabir.watchtracker.data.local.LibraryStatus
import com.sabir.watchtracker.data.repository.LibraryRepository
import com.sabir.watchtracker.data.repository.TmdbRepository
import java.time.LocalDate
import java.time.YearMonth
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

data class MonthlyWatchList(
    val year: Int,
    val month: Int,
    val label: String,
    val entries: List<MonthlyGridEntry>,
    val totalMinutes: Int
) {
    val activityCount: Int
        get() = entries.sumOf { it.watchedCount }

    val movieCount: Int
        get() = entries.count { it.item.mediaType == "movie" }

    val tvShowCount: Int
        get() = entries.count { it.item.mediaType == "tv" }
}

data class MonthlyGridEntry(
    val item: LibraryItem,
    val episodes: List<EpisodeWatch>,
    val overallWatchedEpisodes: Int,
    val watchedDateEpochDay: Long,
    val totalMinutes: Int
) {
    val key: String
        get() = "${item.mediaType}-${item.tmdbId}"

    val watchedCount: Int
        get() = if (item.mediaType == "tv") episodes.size else 1
}

data class MonthlyWatchTime(
    val label: String,
    val minutes: Int
)

data class LibraryUiState(
    val isLoading: Boolean = true,
    val items: List<LibraryItem> = emptyList(),
    val episodeWatches: List<EpisodeWatch> = emptyList(),
    val customLists: List<CustomList> = emptyList(),
    val customListItems: List<CustomListItem> = emptyList(),
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

    val longestWatchStreak: Int
        get() {
            val dates = (
                watchedMovies.mapNotNull { it.watchDateEpochDay } +
                    episodeWatches.map { it.watchedDateEpochDay }
                )
                .distinct()
                .sorted()

            var longest = 0
            var current = 0
            var previous: Long? = null

            dates.forEach { date ->
                current = if (previous != null && date == previous!! + 1L) {
                    current + 1
                } else {
                    1
                }
                longest = maxOf(longest, current)
                previous = date
            }

            return longest
        }

    val monthlyWatchTimeTrend: List<MonthlyWatchTime>
        get() {
            val currentMonth = YearMonth.now()
            return (5 downTo 0).map { monthsAgo ->
                val yearMonth = currentMonth.minusMonths(monthsAgo.toLong())
                val minutes = monthlyLists.firstOrNull { month ->
                    month.year == yearMonth.year &&
                        month.month == yearMonth.monthValue
                }?.totalMinutes ?: 0

                MonthlyWatchTime(
                    label = yearMonth.month.name.take(3)
                        .lowercase()
                        .replaceFirstChar { it.uppercase() },
                    minutes = minutes
                )
            }
        }

    val monthlyLists: List<MonthlyWatchList>
        get() = watchHistoryEntries
            .groupBy { entry ->
                val date = LocalDate.ofEpochDay(entry.watchedDateEpochDay)
                date.year to date.monthValue
            }
            .map { (yearMonth, entries) ->
                val (year, month) = yearMonth
                val groupedEntries = entries
                    .groupBy { it.item.mediaType to it.item.tmdbId }
                    .map { (_, titleEntries) ->
                        val item = titleEntries.first().item
                        val monthlyEpisodes = if (item.mediaType == "tv") {
                            episodeWatches.filter { watch ->
                                val date = LocalDate.ofEpochDay(watch.watchedDateEpochDay)
                                watch.tmdbShowId == item.tmdbId &&
                                    date.year == year &&
                                    date.monthValue == month
                            }.sortedWith(
                                compareBy<EpisodeWatch> { it.watchedDateEpochDay }
                                    .thenBy { it.seasonNumber }
                                    .thenBy { it.episodeNumber }
                            )
                        } else {
                            emptyList()
                        }

                        MonthlyGridEntry(
                            item = item,
                            episodes = monthlyEpisodes,
                            overallWatchedEpisodes = if (item.mediaType == "tv") {
                                episodeWatches.count { it.tmdbShowId == item.tmdbId }
                            } else {
                                0
                            },
                            watchedDateEpochDay = titleEntries.maxOf { it.watchedDateEpochDay },
                            totalMinutes = if (item.mediaType == "movie") {
                                item.runtimeMinutes ?: 0
                            } else {
                                monthlyEpisodes.sumOf { it.runtimeMinutes ?: 0 }
                            }
                        )
                    }
                    .sortedByDescending { it.watchedDateEpochDay }
                MonthlyWatchList(
                    year = year,
                    month = month,
                    label = LocalDate.of(year, month, 1)
                        .format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy")),
                    entries = groupedEntries,
                    totalMinutes = groupedEntries.sumOf { it.totalMinutes }
                )
            }
            .sortedWith(compareByDescending<MonthlyWatchList> { it.year }.thenByDescending { it.month })

    fun itemsForList(listId: Long): List<LibraryItem> {
        val keys = customListItems
            .filter { it.listId == listId }
            .map { it.tmdbId to it.mediaType }
            .toSet()
        return items.filter { (it.tmdbId to it.mediaType) in keys }
    }
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
                repository.observeAllEpisodeWatches(),
                repository.observeCustomLists(),
                repository.observeCustomListItems()
            ) { items, episodeWatches, customLists, customListItems ->
                LibraryUiState(
                    isLoading = false,
                    items = items,
                    episodeWatches = episodeWatches,
                    customLists = customLists,
                    customListItems = customListItems,
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

    fun createCustomList(name: String, description: String) {
        coroutineScope.launch {
            repository.createCustomList(name, description)
        }
    }

    fun updateCustomList(list: CustomList, name: String, description: String) {
        coroutineScope.launch {
            repository.updateCustomList(list, name, description)
        }
    }

    fun addToCustomList(listId: Long, item: LibraryItem) {
        coroutineScope.launch { repository.addToCustomList(listId, item) }
    }

    fun removeFromCustomList(listId: Long, item: LibraryItem) {
        coroutineScope.launch { repository.removeFromCustomList(listId, item) }
    }

    fun deleteCustomList(listId: Long) {
        coroutineScope.launch { repository.deleteCustomList(listId) }
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
