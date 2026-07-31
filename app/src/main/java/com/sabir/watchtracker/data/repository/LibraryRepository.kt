package com.sabir.watchtracker.data.repository

import android.content.Context
import com.sabir.watchtracker.data.local.EpisodeWatch
import com.sabir.watchtracker.data.local.CustomList
import com.sabir.watchtracker.data.local.CustomListItem
import com.sabir.watchtracker.data.local.LibraryItem
import com.sabir.watchtracker.data.local.LibraryStatus
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

    fun observeCustomLists(): Flow<List<CustomList>> =
        customListDao.observeLists()

    fun observeCustomListItems(): Flow<List<CustomListItem>> =
        customListDao.observeItems()

    suspend fun createCustomList(name: String, description: String) {
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) return
        val now = System.currentTimeMillis()
        customListDao.upsertList(
            CustomList(
                name = normalizedName,
                description = description.trim(),
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun updateCustomList(list: CustomList, name: String, description: String) {
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) return
        customListDao.upsertList(
            list.copy(
                name = normalizedName,
                description = description.trim(),
                updatedAt = System.currentTimeMillis()
            )
        )
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

    suspend fun deleteItem(
        item: LibraryItem
    ) {
        if (item.mediaType == "tv") {
            episodeWatchDao.deleteForShow(
                item.tmdbId
            )
        }

        libraryItemDao.delete(item)
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

        libraryItemDao.deleteById(
            tmdbId = tmdbId,
            mediaType = mediaType
        )
    }
}

