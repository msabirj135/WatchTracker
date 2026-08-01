package com.sabir.watchtracker.ui.library

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.sabir.watchtracker.data.backup.BackupImportMode
import com.sabir.watchtracker.data.backup.BackupPreview
import com.sabir.watchtracker.data.backup.ReelTickBackupManager
import com.sabir.watchtracker.data.local.EpisodeWatch
import com.sabir.watchtracker.data.local.CustomList
import com.sabir.watchtracker.data.local.CustomListItem
import com.sabir.watchtracker.data.local.LibraryItem
import com.sabir.watchtracker.data.local.LibraryStatus
import com.sabir.watchtracker.data.local.RewatchRecord
import com.sabir.watchtracker.data.remote.TmdbEpisode
import com.sabir.watchtracker.data.repository.LibraryRepository
import com.sabir.watchtracker.data.repository.TmdbRepository
import java.time.LocalDate
import java.time.YearMonth
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UpNextEntry(
    val item: LibraryItem,
    val episode: TmdbEpisode
) {
    val key: String
        get() = "${item.tmdbId}-${episode.seasonNumber}-${episode.episodeNumber}"
}

data class UpcomingEpisodeEntry(
    val item: LibraryItem,
    val episode: TmdbEpisode,
    val productionStatus: String
) {
    val key: String
        get() = "upcoming-${item.tmdbId}-${episode.seasonNumber}-${episode.episodeNumber}"
}

private data class TvQueueResult(
    val nextAvailable: UpNextEntry?,
    val upcoming: UpcomingEpisodeEntry?
)

data class UpNextUiState(
    val isLoading: Boolean = false,
    val entries: List<UpNextEntry> = emptyList(),
    val upcomingEntries: List<UpcomingEpisodeEntry> = emptyList(),
    val savingShowIds: Set<Int> = emptySet(),
    val errorMessage: String? = null
)

data class BackupUiState(
    val isWorking: Boolean = false,
    val pendingPreview: BackupPreview? = null,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val lastBackupEpochMillis: Long? = null,
    val safetyBackupEpochMillis: Long? = null
)

data class DataHealthUiState(
    val hasRun: Boolean = false,
    val isWorking: Boolean = false,
    val orphanedRecords: Int = 0,
    val futureDatedRecords: Int = 0,
    val missingMetadataTitles: Int = 0,
    val message: String? = null
) {
    val isHealthy: Boolean
        get() = hasRun && orphanedRecords == 0 && futureDatedRecords == 0
}

data class WatchHistoryEntry(
    val key: String,
    val item: LibraryItem,
    val detailText: String,
    val watchedDateEpochDay: Long,
    val runtimeMinutes: Int?,
    val isRewatch: Boolean = false
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
    val totalMinutes: Int,
    val watchCount: Int
) {
    val key: String
        get() = "${item.mediaType}-${item.tmdbId}"

    val watchedCount: Int
        get() = watchCount
}

data class MonthlyWatchTime(
    val label: String,
    val minutes: Int
)

data class LibraryUiState(
    val isLoading: Boolean = true,
    val items: List<LibraryItem> = emptyList(),
    val episodeWatches: List<EpisodeWatch> = emptyList(),
    val rewatchRecords: List<RewatchRecord> = emptyList(),
    val customLists: List<CustomList> = emptyList(),
    val customListItems: List<CustomListItem> = emptyList(),
    val errorMessage: String? = null
) {
    val movies: List<LibraryItem>
        get() = items
            .filter { item -> item.mediaType == "movie" }
            .sortedWith(latestWatchedFirst())

    val tvShows: List<LibraryItem>
        get() = items
            .filter { item -> item.mediaType == "tv" }
            .sortedWith(latestWatchedFirst())

    private fun latestWatchedFirst(): Comparator<LibraryItem> {
        return compareByDescending<LibraryItem> { item ->
            item.watchDateEpochDay ?: Long.MIN_VALUE
        }.thenByDescending { item ->
            item.updatedAt
        }
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

    val tvQueueCandidates: List<LibraryItem>
        get() = tvShows.filter { item ->
            item.status == LibraryStatus.WATCHING ||
                item.status == LibraryStatus.COMPLETED
        }.sortedWith(latestWatchedFirst())

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
                    watchedDateEpochDay = watchedDate,
                    runtimeMinutes = movie.runtimeMinutes
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
                        watch.watchedDateEpochDay,
                    runtimeMinutes = watch.runtimeMinutes
                )
            }

            val rewatchEntries = rewatchRecords.mapNotNull { record ->
                val item = itemsByKey[record.mediaType to record.tmdbId]
                    ?: return@mapNotNull null

                val detail = if (record.mediaType == "movie") {
                    "Movie • Rewatch"
                } else {
                    buildString {
                        append(record.episodeCode ?: "Episode")
                        if (record.episodeName.isNotBlank()) {
                            append(" • ")
                            append(record.episodeName)
                        }
                        append(" • Rewatch")
                    }
                }

                WatchHistoryEntry(
                    key = "rewatch-${record.id}",
                    item = item,
                    detailText = detail,
                    watchedDateEpochDay = record.watchedDateEpochDay,
                    runtimeMinutes = record.runtimeMinutes,
                    isRewatch = true
                )
            }

            return (movieEntries + episodeEntries + rewatchEntries)
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

    val rewatchCount: Int
        get() = rewatchRecords.size

    val movieWatchMinutes: Int
        get() = watchedMovies.sumOf { it.runtimeMinutes ?: 0 } +
            rewatchRecords
                .filter { it.mediaType == "movie" }
                .sumOf { it.runtimeMinutes ?: 0 }

    val tvWatchMinutes: Int
        get() = episodeWatches.sumOf { it.runtimeMinutes ?: 0 } +
            rewatchRecords
                .filter { it.mediaType == "tv" }
                .sumOf { it.runtimeMinutes ?: 0 }

    val totalWatchMinutes: Int
        get() = movieWatchMinutes + tvWatchMinutes

    val thisMonthCount: Int
        get() {
            val firstDay = LocalDate.now()
                .withDayOfMonth(1)
                .toEpochDay()

            return watchHistoryEntries.count { entry ->
                entry.watchedDateEpochDay >= firstDay
            }
        }

    val averagePersonalRating: Double?
        get() = items.mapNotNull { it.personalRating }
            .takeIf { it.isNotEmpty() }
            ?.average()

    val longestWatchStreak: Int
        get() {
            val dates = watchHistoryEntries
                .map { it.watchedDateEpochDay }
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
                            totalMinutes = titleEntries.sumOf { entry ->
                                entry.runtimeMinutes ?: 0
                            },
                            watchCount = titleEntries.size
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
        val itemByKey = items.associateBy { item ->
            item.tmdbId to item.mediaType
        }
        return customListItems
            .filter { it.listId == listId }
            .sortedByDescending { it.addedAt }
            .mapNotNull { listItem ->
                itemByKey[listItem.tmdbId to listItem.mediaType]
            }
    }
}

class LibraryViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = LibraryRepository(
        context = application.applicationContext
    )

    private val tmdbRepository = TmdbRepository()

    private val backupManager = ReelTickBackupManager(
        context = application.applicationContext
    )

    private val backupPreferences = application.getSharedPreferences(
        "reeltick_backup",
        android.content.Context.MODE_PRIVATE
    )

    private val safetyBackupFile = File(
        application.filesDir,
        "reeltick-safety-backup.json"
    )

    private var pendingBackupJson: String? = null

    private var upNextJob: Job? = null
    private var upNextSignature: String? = null

    private val attemptedRuntimeIds = mutableSetOf<Int>()
    private val attemptedGenreKeys = mutableSetOf<String>()

    private val coroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    )

    var uiState = mutableStateOf(
        LibraryUiState()
    )
        private set

    var backupUiState = mutableStateOf(
        BackupUiState(
            lastBackupEpochMillis = backupPreferences
                .getLong("last_backup", 0L)
                .takeIf { value -> value > 0L },
            safetyBackupEpochMillis = safetyBackupFile
                .takeIf { file -> file.exists() }
                ?.lastModified()
                ?.takeIf { value -> value > 0L }
        )
    )
        private set

    var upNextUiState = mutableStateOf(
        UpNextUiState()
    )
        private set

    var dataHealthUiState = mutableStateOf(DataHealthUiState())
        private set

    init {
        observeLibrary()
    }

    private fun observeLibrary() {
        coroutineScope.launch {
            combine(
                repository.observeAll(),
                repository.observeAllEpisodeWatches(),
                repository.observeAllRewatches(),
                repository.observeCustomLists(),
                repository.observeCustomListItems()
            ) { items, episodeWatches, rewatches, customLists, customListItems ->
                LibraryUiState(
                    isLoading = false,
                    items = items,
                    episodeWatches = episodeWatches,
                    rewatchRecords = rewatches,
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
                    backfillGenreMetadata(state.items)
                    refreshUpNextIfNeeded(state)
                }
        }
    }

    private fun refreshUpNextIfNeeded(state: LibraryUiState) {
        val signature = state.tvQueueCandidates.joinToString("|") { item ->
            buildString {
                append(item.tmdbId)
                append(":")
                append(item.currentSeason)
                append(":")
                append(item.currentEpisode)
                append(":")
                append(item.updatedAt)
            }
        }

        if (signature == upNextSignature) return
        upNextSignature = signature
        loadUpNext(state)
    }

    private fun loadUpNext(state: LibraryUiState = uiState.value) {
        upNextJob?.cancel()

        val shows = state.tvQueueCandidates
        if (shows.isEmpty()) {
            upNextUiState.value = UpNextUiState()
            return
        }

        upNextUiState.value = upNextUiState.value.copy(
            isLoading = true,
            errorMessage = null
        )

        upNextJob = coroutineScope.launch {
            val attempts = withContext(Dispatchers.IO) {
                shows.map { show ->
                    runCatching {
                        findShowQueue(
                            show = show,
                            watchedEpisodes = state.episodeWatches
                                .filter { watch ->
                                    watch.tmdbShowId == show.tmdbId
                                }
                        )
                    }
                }
            }

            val results = attempts.mapNotNull { attempt -> attempt.getOrNull() }
            val entries = results.mapNotNull { result -> result.nextAvailable }
            val upcomingEntries = results
                .mapNotNull { result -> result.upcoming }
                .sortedBy { entry -> entry.episode.parsedAirDate }

            val failedCount = attempts.count { attempt ->
                attempt.isFailure
            }

            upNextUiState.value = upNextUiState.value.copy(
                isLoading = false,
                entries = entries,
                upcomingEntries = upcomingEntries,
                errorMessage = if (
                    entries.isEmpty() &&
                    upcomingEntries.isEmpty() &&
                    failedCount == shows.size
                ) {
                    "Unable to load upcoming episodes."
                } else {
                    null
                }
            )
        }
    }

    private suspend fun findShowQueue(
        show: LibraryItem,
        watchedEpisodes: List<EpisodeWatch>
    ): TvQueueResult {
        val watchedKeys = watchedEpisodes.map { watch ->
            watch.seasonNumber to watch.episodeNumber
        }.toSet()

        val details = tmdbRepository.getTvDetails(show.tmdbId)

        val allEpisodes = mutableListOf<TmdbEpisode>()

        details.regularSeasons.forEach { seasonSummary ->
            val season = tmdbRepository.getTvSeasonDetails(
                seriesId = show.tmdbId,
                seasonNumber = seasonSummary.seasonNumber
            )

            allEpisodes += season.episodes
                .sortedBy { episode -> episode.episodeNumber }
        }

        val unwatched = allEpisodes.filter { episode ->
            (episode.seasonNumber to episode.episodeNumber) !in watchedKeys
        }

        val nextAvailable = unwatched.firstOrNull { episode ->
            episode.hasAired
        }?.let { episode ->
            UpNextEntry(item = show, episode = episode)
        }

        val upcoming = unwatched
            .filter { episode -> !episode.hasAired }
            .minByOrNull { episode ->
                episode.parsedAirDate ?: LocalDate.MAX
            }
            ?.let { episode ->
                UpcomingEpisodeEntry(
                    item = show,
                    episode = episode,
                    productionStatus = details.productionStatus
                )
            }

        return TvQueueResult(
            nextAvailable = nextAvailable,
            upcoming = upcoming
        )
    }

    fun markUpNextWatched(
        entry: UpNextEntry,
        watchedDateEpochDay: Long
    ) {
        if (entry.item.tmdbId in upNextUiState.value.savingShowIds) {
            return
        }

        upNextUiState.value = upNextUiState.value.copy(
            savingShowIds = upNextUiState.value.savingShowIds +
                entry.item.tmdbId,
            errorMessage = null
        )

        coroutineScope.launch {
            try {
                repository.markEpisodeWatched(
                    show = entry.item,
                    episode = entry.episode,
                    watchedDateEpochDay = watchedDateEpochDay
                )
            } catch (exception: Exception) {
                upNextUiState.value = upNextUiState.value.copy(
                    errorMessage = exception.message
                        ?: "Unable to save the episode."
                )
            } finally {
                upNextUiState.value = upNextUiState.value.copy(
                    savingShowIds = upNextUiState.value.savingShowIds -
                        entry.item.tmdbId
                )
            }
        }
    }

    fun retryUpNext() {
        upNextSignature = null
        refreshUpNextIfNeeded(uiState.value)
    }

    fun createCustomList(
        name: String,
        description: String,
        colorKey: String,
        iconKey: String
    ) {
        coroutineScope.launch {
            repository.createCustomList(name, description, colorKey, iconKey)
        }
    }

    fun updateCustomList(
        list: CustomList,
        name: String,
        description: String,
        colorKey: String,
        iconKey: String
    ) {
        coroutineScope.launch {
            repository.updateCustomList(list, name, description, colorKey, iconKey)
        }
    }

    fun duplicateCustomList(list: CustomList) {
        coroutineScope.launch { repository.duplicateCustomList(list) }
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

    fun runDataHealthCheck() {
        dataHealthUiState.value = calculateDataHealth(uiState.value)
    }

    fun repairDataHealth() {
        if (dataHealthUiState.value.isWorking) return
        dataHealthUiState.value = dataHealthUiState.value.copy(
            isWorking = true,
            message = null
        )
        coroutineScope.launch {
            try {
                val removed = withContext(Dispatchers.IO) {
                    repository.repairOrphanedData()
                }
                dataHealthUiState.value = calculateDataHealth(uiState.value).copy(
                    orphanedRecords = 0,
                    message = if (removed == 0) {
                        "No orphaned records needed repair."
                    } else {
                        "Removed $removed orphaned ${if (removed == 1) "record" else "records"}."
                    }
                )
            } catch (exception: Exception) {
                dataHealthUiState.value = dataHealthUiState.value.copy(
                    isWorking = false,
                    message = exception.message ?: "Unable to repair the database."
                )
            }
        }
    }

    private fun calculateDataHealth(state: LibraryUiState): DataHealthUiState {
        val titleKeys = state.items.map { it.tmdbId to it.mediaType }.toSet()
        val listIds = state.customLists.map { it.id }.toSet()
        val tvIds = state.tvShows.map { it.tmdbId }.toSet()
        val orphaned = state.customListItems.count { item ->
            item.listId !in listIds || (item.tmdbId to item.mediaType) !in titleKeys
        } + state.episodeWatches.count { it.tmdbShowId !in tvIds } +
            state.rewatchRecords.count { (it.tmdbId to it.mediaType) !in titleKeys }
        val today = LocalDate.now().toEpochDay()
        val futureDated = state.items.count { (it.watchDateEpochDay ?: Long.MIN_VALUE) > today } +
            state.episodeWatches.count { it.watchedDateEpochDay > today } +
            state.rewatchRecords.count { it.watchedDateEpochDay > today }
        val missingMetadata = state.items.count { item ->
            item.genres.isEmpty() ||
                (item.mediaType == "movie" && (item.runtimeMinutes ?: 0) <= 0)
        }
        return DataHealthUiState(
            hasRun = true,
            orphanedRecords = orphaned,
            futureDatedRecords = futureDated,
            missingMetadataTitles = missingMetadata
        )
    }

    fun exportBackup(uri: Uri) {
        if (backupUiState.value.isWorking) return

        backupUiState.value = backupUiState.value.copy(
            isWorking = true,
            successMessage = null,
            errorMessage = null
        )

        coroutineScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    backupManager.createBackupJson()
                }

                withContext(Dispatchers.IO) {
                    val outputStream = getApplication<Application>()
                        .contentResolver
                        .openOutputStream(uri, "wt")
                        ?: error("Unable to open the selected file.")

                    outputStream.bufferedWriter().use { writer ->
                        writer.write(json)
                    }
                }

                val now = System.currentTimeMillis()
                backupPreferences.edit()
                    .putLong("last_backup", now)
                    .apply()

                backupUiState.value = backupUiState.value.copy(
                    isWorking = false,
                    successMessage = "Backup exported successfully.",
                    lastBackupEpochMillis = now
                )
            } catch (exception: Exception) {
                backupUiState.value = backupUiState.value.copy(
                    isWorking = false,
                    errorMessage = exception.message
                        ?: "Unable to export the backup."
                )
            }
        }
    }

    fun inspectBackup(uri: Uri) {
        if (backupUiState.value.isWorking) return

        backupUiState.value = backupUiState.value.copy(
            isWorking = true,
            successMessage = null,
            errorMessage = null
        )

        coroutineScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    val inputStream = getApplication<Application>()
                        .contentResolver
                        .openInputStream(uri)
                        ?: error("Unable to open the selected file.")

                    inputStream.bufferedReader().use { reader ->
                        reader.readText()
                    }
                }

                val preview = withContext(Dispatchers.IO) {
                    backupManager.inspectBackupJson(json)
                }

                pendingBackupJson = json
                backupUiState.value = backupUiState.value.copy(
                    isWorking = false,
                    pendingPreview = preview
                )
            } catch (exception: Exception) {
                pendingBackupJson = null
                backupUiState.value = backupUiState.value.copy(
                    isWorking = false,
                    errorMessage = exception.message
                        ?: "Unable to read the backup."
                )
            }
        }
    }

    fun restoreBackup(mode: BackupImportMode) {
        val json = pendingBackupJson ?: return
        if (backupUiState.value.isWorking) return

        backupUiState.value = backupUiState.value.copy(
            isWorking = true,
            errorMessage = null,
            successMessage = null
        )

        coroutineScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    if (mode == BackupImportMode.REPLACE) {
                        createInternalSafetyBackup()
                    }

                    backupManager.restoreBackupJson(
                        json = json,
                        mode = mode
                    )
                }

                pendingBackupJson = null
                val safetyTimestamp = safetyBackupFile
                    .takeIf { file -> file.exists() }
                    ?.lastModified()

                backupUiState.value = backupUiState.value.copy(
                    isWorking = false,
                    pendingPreview = null,
                    safetyBackupEpochMillis = safetyTimestamp,
                    successMessage = buildString {
                        append("Backup restored: ")
                        append(result.titleCount)
                        append(" titles, ")
                        append(result.episodeCount)
                        append(" episodes, ")
                        append(result.rewatchCount)
                        append(" rewatches and ")
                        append(result.customListCount)
                        append(" lists.")
                    }
                )
            } catch (exception: Exception) {
                backupUiState.value = backupUiState.value.copy(
                    isWorking = false,
                    errorMessage = exception.message
                        ?: "Unable to restore the backup."
                )
            }
        }
    }

    fun dismissBackupPreview() {
        if (backupUiState.value.isWorking) return
        pendingBackupJson = null
        backupUiState.value = backupUiState.value.copy(
            pendingPreview = null,
            successMessage = null,
            errorMessage = null
        )
    }

    fun clearBackupMessage() {
        if (backupUiState.value.isWorking) return
        backupUiState.value = backupUiState.value.copy(
            successMessage = null,
            errorMessage = null
        )
    }

    fun exportHistoryCsv(uri: Uri) {
        if (backupUiState.value.isWorking) return

        backupUiState.value = backupUiState.value.copy(
            isWorking = true,
            successMessage = null,
            errorMessage = null
        )

        coroutineScope.launch {
            try {
                val csv = withContext(Dispatchers.IO) {
                    backupManager.createHistoryCsv()
                }

                withContext(Dispatchers.IO) {
                    val outputStream = getApplication<Application>()
                        .contentResolver
                        .openOutputStream(uri, "wt")
                        ?: error("Unable to open the selected file.")

                    outputStream.bufferedWriter().use { writer ->
                        writer.write(csv)
                    }
                }

                backupUiState.value = backupUiState.value.copy(
                    isWorking = false,
                    successMessage = "Watch history exported as CSV."
                )
            } catch (exception: Exception) {
                backupUiState.value = backupUiState.value.copy(
                    isWorking = false,
                    errorMessage = exception.message
                        ?: "Unable to export watch history."
                )
            }
        }
    }

    fun restoreSafetyBackup() {
        if (
            backupUiState.value.isWorking ||
            !safetyBackupFile.exists()
        ) return

        backupUiState.value = backupUiState.value.copy(
            isWorking = true,
            successMessage = null,
            errorMessage = null
        )

        coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    backupManager.restoreBackupJson(
                        json = safetyBackupFile.readText(),
                        mode = BackupImportMode.REPLACE
                    )
                }

                backupUiState.value = backupUiState.value.copy(
                    isWorking = false,
                    successMessage = "Safety backup restored successfully."
                )
            } catch (exception: Exception) {
                backupUiState.value = backupUiState.value.copy(
                    isWorking = false,
                    errorMessage = exception.message
                        ?: "Unable to restore the safety backup."
                )
            }
        }
    }

    fun clearAllData() {
        if (backupUiState.value.isWorking) return

        backupUiState.value = backupUiState.value.copy(
            isWorking = true,
            successMessage = null,
            errorMessage = null
        )

        coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    createInternalSafetyBackup()
                    backupManager.clearAllData()
                }

                backupUiState.value = backupUiState.value.copy(
                    isWorking = false,
                    safetyBackupEpochMillis = safetyBackupFile.lastModified(),
                    successMessage = "All library data cleared. A safety backup was kept."
                )
            } catch (exception: Exception) {
                backupUiState.value = backupUiState.value.copy(
                    isWorking = false,
                    errorMessage = exception.message
                        ?: "Unable to clear the library."
                )
            }
        }
    }

    private suspend fun createInternalSafetyBackup() {
        safetyBackupFile.writeText(
            backupManager.createBackupJson()
        )
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

    private fun backfillGenreMetadata(items: List<LibraryItem>) {
        val missing = items.filter { item ->
            item.genres.isEmpty() &&
                attemptedGenreKeys.add("${item.mediaType}-${item.tmdbId}")
        }

        if (missing.isEmpty()) return

        coroutineScope.launch(Dispatchers.IO) {
            missing.forEach { item ->
                val names = runCatching {
                    if (item.mediaType == "movie") {
                        tmdbRepository.getMovieDetails(item.tmdbId)
                            .genres
                            .map { genre -> genre.name }
                    } else {
                        tmdbRepository.getTvDetails(item.tmdbId)
                            .genres
                            .map { genre -> genre.name }
                    }
                }.getOrNull().orEmpty()

                if (names.isNotEmpty()) {
                    repository.updateGenres(
                        tmdbId = item.tmdbId,
                        mediaType = item.mediaType,
                        genreNames = names
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
