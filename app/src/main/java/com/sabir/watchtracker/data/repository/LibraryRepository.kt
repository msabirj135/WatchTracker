package com.sabir.watchtracker.data.repository

import android.content.Context
import com.sabir.watchtracker.data.local.EpisodeWatch
import com.sabir.watchtracker.data.local.CustomList
import com.sabir.watchtracker.data.local.CustomListItem
import com.sabir.watchtracker.data.local.LibraryItem
import com.sabir.watchtracker.data.local.LibraryStatus
import com.sabir.watchtracker.data.local.RewatchRecord
import com.sabir.watchtracker.data.local.WatchTrackerDatabase
import com.sabir.watchtracker.data.remote.TmdbEpisode
import com.sabir.watchtracker.data.remote.TmdbSearchResult
import kotlinx.coroutines.flow.Flow

class LibraryRepository(
    context: Context
) {
    private val database = WatchTrackerDatabase
        .getInstance(context)

    private val libraryItemDao =
        database.libraryItemDao()

    private val episodeWatchDao =
        database.episodeWatchDao()

    private val customListDao = database.customListDao()

    private val rewatchRecordDao = database.rewatchRecordDao()

    fun observeCustomLists(): Flow<List<CustomList>> =
        customListDao.observeLists()

    fun observeCustomListItems(): Flow<List<CustomListItem>> =
        customListDao.observeItems()

    suspend fun createCustomList(
        name: String,
        description: String,
        colorKey: String,
        iconKey: String
    ) {
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) return
        val now = System.currentTimeMillis()
        customListDao.upsertList(
            CustomList(
                name = normalizedName,
                description = description.trim(),
                createdAt = now,
                updatedAt = now,
                colorKey = colorKey,
                iconKey = iconKey
            )
        )
    }

    suspend fun updateCustomList(
        list: CustomList,
        name: String,
        description: String,
        colorKey: String,
        iconKey: String
    ) {
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) return
        customListDao.upsertList(
            list.copy(
                name = normalizedName,
                description = description.trim(),
                updatedAt = System.currentTimeMillis(),
                colorKey = colorKey,
                iconKey = iconKey
            )
        )
    }

    suspend fun duplicateCustomList(list: CustomList) {
        val now = System.currentTimeMillis()
        val newListId = customListDao.insertList(
            list.copy(
                id = 0,
                name = "${list.name} copy",
                createdAt = now,
                updatedAt = now
            )
        )
        customListDao.getItemsSnapshot()
            .filter { item -> item.listId == list.id }
            .forEachIndexed { index, item ->
                customListDao.upsertItem(
                    item.copy(
                        listId = newListId,
                        addedAt = now + index
                    )
                )
            }
    }

    suspend fun addToCustomList(listId: Long, item: LibraryItem) {
        customListDao.upsertItem(
            CustomListItem(
                listId = listId,
                tmdbId = item.tmdbId,
                mediaType = item.mediaType,
                addedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun removeFromCustomList(listId: Long, item: LibraryItem) {
        customListDao.removeItem(listId, item.tmdbId, item.mediaType)
    }

    suspend fun deleteCustomList(listId: Long) {
        customListDao.deleteItems(listId)
        customListDao.deleteList(listId)
    }

    fun observeAll(): Flow<List<LibraryItem>> {
        return libraryItemDao.observeAll()
    }

    fun observeAllEpisodeWatches(): Flow<List<EpisodeWatch>> {
        return episodeWatchDao.observeAll()
    }

    fun observeAllRewatches(): Flow<List<RewatchRecord>> {
        return rewatchRecordDao.observeAll()
    }

    fun observeRewatches(
        tmdbId: Int,
        mediaType: String
    ): Flow<List<RewatchRecord>> {
        return rewatchRecordDao.observeForTitle(
            tmdbId = tmdbId,
            mediaType = mediaType
        )
    }

    fun observeMovies(): Flow<List<LibraryItem>> {
        return libraryItemDao.observeMovies()
    }

    fun observeTvShows(): Flow<List<LibraryItem>> {
        return libraryItemDao.observeTvShows()
    }

    fun observeByStatus(
        status: LibraryStatus
    ): Flow<List<LibraryItem>> {
        return libraryItemDao.observeByStatus(status)
    }

    fun observeTotalCount(): Flow<Int> {
        return libraryItemDao.observeTotalCount()
    }

    fun observeMovieCount(): Flow<Int> {
        return libraryItemDao.observeMovieCount()
    }

    fun observeTvShowCount(): Flow<Int> {
        return libraryItemDao.observeTvShowCount()
    }

    fun observeStatusCount(
        status: LibraryStatus
    ): Flow<Int> {
        return libraryItemDao.observeStatusCount(status)
    }

    fun observeIsSaved(
        tmdbId: Int,
        mediaType: String
    ): Flow<Boolean> {
        return libraryItemDao.observeIsSaved(
            tmdbId = tmdbId,
            mediaType = mediaType
        )
    }

    fun observeEpisodeWatches(
        tmdbShowId: Int
    ): Flow<List<EpisodeWatch>> {
        return episodeWatchDao.observeForShow(
            tmdbShowId
        )
    }

    fun observeWatchedEpisodeCount(
        tmdbShowId: Int
    ): Flow<Int> {
        return episodeWatchDao.observeWatchedCount(
            tmdbShowId
        )
    }

    suspend fun getItem(
        tmdbId: Int,
        mediaType: String
    ): LibraryItem? {
        return libraryItemDao.getItem(
            tmdbId = tmdbId,
            mediaType = mediaType
        )
    }

    suspend fun saveSearchResult(
        result: TmdbSearchResult,
        status: LibraryStatus,
        watchDateEpochDay: Long?,
        personalRating: Double?,
        notes: String,
        currentSeason: Int? = null,
        currentEpisode: Int? = null,
        totalSeasons: Int? = null,
        totalEpisodes: Int? = null,
        runtimeMinutes: Int? = null
    ) {
        val mediaType = result.mediaType
            ?.takeIf { type ->
                type == "movie" || type == "tv"
            }
            ?: return

        val existingItem = libraryItemDao.getItem(
            tmdbId = result.id,
            mediaType = mediaType
        )

        val currentTime = System.currentTimeMillis()

        val sanitizedRating = personalRating
            ?.coerceIn(
                minimumValue = 0.0,
                maximumValue = 10.0
            )

        val libraryItem = LibraryItem(
            tmdbId = result.id,
            mediaType = mediaType,
            title = result.displayTitle,
            overview = result.overview,
            posterPath = result.posterPath,
            backdropPath = result.backdropPath,
            releaseDate = result.displayDate,
            tmdbRating = result.voteAverage,
            status = status,
            watchDateEpochDay = watchDateEpochDay,
            personalRating = sanitizedRating,
            notes = notes.trim(),
            currentSeason = currentSeason
                ?: existingItem?.currentSeason,
            currentEpisode = currentEpisode
                ?: existingItem?.currentEpisode,
            totalSeasons = totalSeasons
                ?: existingItem?.totalSeasons,
            totalEpisodes = totalEpisodes
                ?: existingItem?.totalEpisodes,
            runtimeMinutes = runtimeMinutes
                ?: existingItem?.runtimeMinutes,
            genreNames = genreNamesFor(
                mediaType = mediaType,
                genreIds = result.genreIds
            ).takeIf { names -> names.isNotEmpty() }
                ?.joinToString("|")
                ?: existingItem?.genreNames,
            addedAt = existingItem?.addedAt
                ?: currentTime,
            updatedAt = currentTime
        )

        libraryItemDao.upsert(libraryItem)
    }

    suspend fun updateMovieRuntime(
        tmdbId: Int,
        runtimeMinutes: Int
    ) {
        val movie = libraryItemDao.getItem(tmdbId, "movie")
            ?: return

        libraryItemDao.upsert(
            movie.copy(
                runtimeMinutes = runtimeMinutes.coerceAtLeast(0),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateGenres(
        tmdbId: Int,
        mediaType: String,
        genreNames: List<String>
    ) {
        if (genreNames.isEmpty()) return

        val item = libraryItemDao.getItem(tmdbId, mediaType) ?: return
        libraryItemDao.upsert(
            item.copy(
                genreNames = genreNames
                    .map { name -> name.trim() }
                    .filter { name -> name.isNotBlank() }
                    .distinct()
                    .joinToString("|"),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateItem(
        item: LibraryItem
    ) {
        libraryItemDao.upsert(
            item.copy(
                personalRating = item.personalRating
                    ?.coerceIn(
                        minimumValue = 0.0,
                        maximumValue = 10.0
                    ),
                notes = item.notes.trim(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateTvMetadata(
        tmdbShowId: Int,
        totalSeasons: Int,
        totalEpisodes: Int
    ) {
        val item = libraryItemDao.getItem(
            tmdbId = tmdbShowId,
            mediaType = "tv"
        ) ?: return

        libraryItemDao.upsert(
            item.copy(
                totalSeasons = totalSeasons,
                totalEpisodes = totalEpisodes,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun synchronizeTvProgress(
        tmdbShowId: Int
    ) {
        val show = libraryItemDao.getItem(
            tmdbId = tmdbShowId,
            mediaType = "tv"
        ) ?: return

        val latestEpisode = episodeWatchDao
            .getLatestForShow(tmdbShowId)

        val watchedCount = episodeWatchDao
            .getWatchedCount(tmdbShowId)

        val lastWatchedDate = episodeWatchDao
            .getLastWatchedDate(tmdbShowId)

        val isComplete = show.totalEpisodes
            ?.takeIf { total -> total > 0 }
            ?.let { total -> watchedCount >= total }
            ?: false

        libraryItemDao.upsert(
            show.copy(
                status = when {
                    show.status == LibraryStatus.DROPPED ->
                        LibraryStatus.DROPPED

                    latestEpisode == null ->
                        LibraryStatus.PLAN_TO_WATCH

                    isComplete ->
                        LibraryStatus.COMPLETED

                    else ->
                        LibraryStatus.WATCHING
                },
                watchDateEpochDay = lastWatchedDate,
                currentSeason = latestEpisode?.seasonNumber,
                currentEpisode = latestEpisode?.episodeNumber,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun markEpisodeWatched(
        show: LibraryItem,
        episode: TmdbEpisode,
        watchedDateEpochDay: Long
    ) {
        episodeWatchDao.upsert(
            EpisodeWatch(
                tmdbShowId = show.tmdbId,
                seasonNumber = episode.seasonNumber,
                episodeNumber = episode.episodeNumber,
                episodeName = episode.name,
                watchedDateEpochDay = watchedDateEpochDay,
                runtimeMinutes = episode.runtime
            )
        )

        val watchedCount = episodeWatchDao
            .getWatchedCount(show.tmdbId)

        val lastWatchedDate = episodeWatchDao
            .getLastWatchedDate(show.tmdbId)

        val isComplete = show.totalEpisodes
            ?.takeIf { total -> total > 0 }
            ?.let { total -> watchedCount >= total }
            ?: false

        libraryItemDao.upsert(
            show.copy(
                status = when (show.status) {
                    LibraryStatus.DROPPED ->
                        LibraryStatus.DROPPED

                    else -> if (isComplete) {
                        LibraryStatus.COMPLETED
                    } else {
                        LibraryStatus.WATCHING
                    }
                },
                watchDateEpochDay = lastWatchedDate,
                currentSeason = episode.seasonNumber,
                currentEpisode = episode.episodeNumber,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun markSeriesCompleted(
        show: LibraryItem,
        remainingEpisodes: List<TmdbEpisode>,
        watchedDateEpochDay: Long
    ) {
        episodeWatchDao.upsertAll(
            remainingEpisodes.map { episode ->
                EpisodeWatch(
                    tmdbShowId = show.tmdbId,
                    seasonNumber = episode.seasonNumber,
                    episodeNumber = episode.episodeNumber,
                    episodeName = episode.name,
                    watchedDateEpochDay = watchedDateEpochDay,
                    runtimeMinutes = episode.runtime
                )
            }
        )

        val finalEpisode = remainingEpisodes.maxWithOrNull(
            compareBy<TmdbEpisode> { it.seasonNumber }
                .thenBy { it.episodeNumber }
        )

        libraryItemDao.upsert(
            show.copy(
                status = LibraryStatus.COMPLETED,
                watchDateEpochDay = watchedDateEpochDay,
                currentSeason = finalEpisode?.seasonNumber
                    ?: show.currentSeason,
                currentEpisode = finalEpisode?.episodeNumber
                    ?: show.currentEpisode,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun markEpisodesWatched(
        show: LibraryItem,
        episodes: List<TmdbEpisode>,
        watchedDateEpochDay: Long
    ) {
        if (episodes.isEmpty()) return

        episodeWatchDao.upsertAll(
            episodes.map { episode ->
                EpisodeWatch(
                    tmdbShowId = show.tmdbId,
                    seasonNumber = episode.seasonNumber,
                    episodeNumber = episode.episodeNumber,
                    episodeName = episode.name,
                    watchedDateEpochDay = watchedDateEpochDay,
                    runtimeMinutes = episode.runtime
                )
            }
        )

        synchronizeTvProgress(show.tmdbId)
    }

    suspend fun unmarkEpisodeWatched(
        show: LibraryItem,
        seasonNumber: Int,
        episodeNumber: Int
    ) {
        episodeWatchDao.deleteEpisode(
            tmdbShowId = show.tmdbId,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber
        )

        val latestEpisode = episodeWatchDao
            .getLatestForShow(show.tmdbId)

        val lastWatchedDate = episodeWatchDao
            .getLastWatchedDate(show.tmdbId)

        libraryItemDao.upsert(
            show.copy(
                status = if (latestEpisode == null) {
                    LibraryStatus.PLAN_TO_WATCH
                } else {
                    LibraryStatus.WATCHING
                },
                currentSeason =
                    latestEpisode?.seasonNumber,
                currentEpisode =
                    latestEpisode?.episodeNumber,
                watchDateEpochDay = lastWatchedDate,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateMovieWatchDate(
        movie: LibraryItem,
        watchedDateEpochDay: Long?
    ) {
        if (movie.mediaType != "movie") {
            return
        }

        libraryItemDao.upsert(
            movie.copy(
                watchDateEpochDay = watchedDateEpochDay,
                status = if (watchedDateEpochDay == null) {
                    LibraryStatus.PLAN_TO_WATCH
                } else {
                    LibraryStatus.COMPLETED
                },
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun addMovieRewatch(
        movie: LibraryItem,
        watchedDateEpochDay: Long
    ) {
        if (movie.mediaType != "movie") return

        rewatchRecordDao.insert(
            RewatchRecord(
                tmdbId = movie.tmdbId,
                mediaType = "movie",
                seasonNumber = null,
                episodeNumber = null,
                episodeName = "",
                watchedDateEpochDay = watchedDateEpochDay,
                runtimeMinutes = movie.runtimeMinutes,
                createdAt = System.currentTimeMillis()
            )
        )

        libraryItemDao.upsert(
            movie.copy(
                status = LibraryStatus.COMPLETED,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun addEpisodeRewatch(
        show: LibraryItem,
        episode: TmdbEpisode,
        watchedDateEpochDay: Long
    ) {
        if (show.mediaType != "tv") return

        rewatchRecordDao.insert(
            RewatchRecord(
                tmdbId = show.tmdbId,
                mediaType = "tv",
                seasonNumber = episode.seasonNumber,
                episodeNumber = episode.episodeNumber,
                episodeName = episode.name,
                watchedDateEpochDay = watchedDateEpochDay,
                runtimeMinutes = episode.runtime,
                createdAt = System.currentTimeMillis()
            )
        )

        libraryItemDao.upsert(
            show.copy(updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun deleteRewatch(recordId: Long) {
        rewatchRecordDao.deleteById(recordId)
    }

    suspend fun deleteItem(
        item: LibraryItem
    ) {
        if (item.mediaType == "tv") {
            episodeWatchDao.deleteForShow(
                item.tmdbId
            )
        }

        customListDao.removeTitleFromAllLists(
            tmdbId = item.tmdbId,
            mediaType = item.mediaType
        )

        rewatchRecordDao.deleteForTitle(
            tmdbId = item.tmdbId,
            mediaType = item.mediaType
        )

        libraryItemDao.delete(item)
    }

    private fun genreNamesFor(
        mediaType: String,
        genreIds: List<Int>
    ): List<String> {
        val movieGenres = mapOf(
            28 to "Action", 12 to "Adventure", 16 to "Animation",
            35 to "Comedy", 80 to "Crime", 99 to "Documentary",
            18 to "Drama", 10751 to "Family", 14 to "Fantasy",
            36 to "History", 27 to "Horror", 10402 to "Music",
            9648 to "Mystery", 10749 to "Romance",
            878 to "Science Fiction", 10770 to "TV Movie",
            53 to "Thriller", 10752 to "War", 37 to "Western"
        )
        val tvGenres = mapOf(
            10759 to "Action & Adventure", 16 to "Animation",
            35 to "Comedy", 80 to "Crime", 99 to "Documentary",
            18 to "Drama", 10751 to "Family", 10762 to "Kids",
            9648 to "Mystery", 10763 to "News", 10764 to "Reality",
            10765 to "Sci-Fi & Fantasy", 10766 to "Soap",
            10767 to "Talk", 10768 to "War & Politics", 37 to "Western"
        )
        val lookup = if (mediaType == "tv") tvGenres else movieGenres
        return genreIds.mapNotNull(lookup::get).distinct()
    }

    suspend fun deleteItem(
        tmdbId: Int,
        mediaType: String
    ) {
        if (mediaType == "tv") {
            episodeWatchDao.deleteForShow(
                tmdbId
            )
        }

        customListDao.removeTitleFromAllLists(
            tmdbId = tmdbId,
            mediaType = mediaType
        )

        rewatchRecordDao.deleteForTitle(
            tmdbId = tmdbId,
            mediaType = mediaType
        )

        libraryItemDao.deleteById(
            tmdbId = tmdbId,
            mediaType = mediaType
        )
    }
}

