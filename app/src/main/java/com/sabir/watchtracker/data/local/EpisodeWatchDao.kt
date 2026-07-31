package com.sabir.watchtracker.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodeWatchDao {

    @Upsert
    suspend fun upsert(
        episodeWatch: EpisodeWatch
    )

    @Query(
        """
        DELETE FROM episode_watches
        WHERE tmdbShowId = :tmdbShowId
          AND seasonNumber = :seasonNumber
          AND episodeNumber = :episodeNumber
        """
    )
    suspend fun deleteEpisode(
        tmdbShowId: Int,
        seasonNumber: Int,
        episodeNumber: Int
    )

    @Query(
        """
        SELECT *
        FROM episode_watches
        WHERE tmdbShowId = :tmdbShowId
        ORDER BY seasonNumber ASC, episodeNumber ASC
        """
    )
    fun observeForShow(
        tmdbShowId: Int
    ): Flow<List<EpisodeWatch>>

    @Query(
        """
        SELECT *
        FROM episode_watches
        WHERE tmdbShowId = :tmdbShowId
        ORDER BY seasonNumber DESC, episodeNumber DESC
        LIMIT 1
        """
    )
    suspend fun getLatestForShow(
        tmdbShowId: Int
    ): EpisodeWatch?

    @Query(
        """
        SELECT COUNT(*)
        FROM episode_watches
        WHERE tmdbShowId = :tmdbShowId
        """
    )
    suspend fun getWatchedCount(
        tmdbShowId: Int
    ): Int

    @Query(
        """
        SELECT MAX(watchedDateEpochDay)
        FROM episode_watches
        WHERE tmdbShowId = :tmdbShowId
        """
    )
    suspend fun getLastWatchedDate(
        tmdbShowId: Int
    ): Long?

    @Query(
        """
        SELECT COUNT(*)
        FROM episode_watches
        WHERE tmdbShowId = :tmdbShowId
        """
    )
    fun observeWatchedCount(
        tmdbShowId: Int
    ): Flow<Int>

    @Query(
        """
        SELECT EXISTS(
            SELECT 1
            FROM episode_watches
            WHERE tmdbShowId = :tmdbShowId
              AND seasonNumber = :seasonNumber
              AND episodeNumber = :episodeNumber
        )
        """
    )
    fun observeIsWatched(
        tmdbShowId: Int,
        seasonNumber: Int,
        episodeNumber: Int
    ): Flow<Boolean>

    @Query(
        """
        DELETE FROM episode_watches
        WHERE tmdbShowId = :tmdbShowId
        """
    )
    suspend fun deleteForShow(
        tmdbShowId: Int
    )
}
