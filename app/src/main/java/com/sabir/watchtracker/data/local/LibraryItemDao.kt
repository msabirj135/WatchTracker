package com.sabir.watchtracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryItemDao {

    @Upsert
    suspend fun upsert(
        item: LibraryItem
    )

    @Upsert
    suspend fun upsertAll(
        items: List<LibraryItem>
    )

    @Query("SELECT * FROM library_items")
    suspend fun getAllSnapshot(): List<LibraryItem>

    @Query("DELETE FROM library_items")
    suspend fun deleteAll()

    @Delete
    suspend fun delete(
        item: LibraryItem
    )

    @Query(
        """
        SELECT *
        FROM library_items
        ORDER BY updatedAt DESC
        """
    )
    fun observeAll(): Flow<List<LibraryItem>>

    @Query(
        """
        SELECT *
        FROM library_items
        WHERE mediaType = 'movie'
        ORDER BY updatedAt DESC
        """
    )
    fun observeMovies(): Flow<List<LibraryItem>>

    @Query(
        """
        SELECT *
        FROM library_items
        WHERE mediaType = 'tv'
        ORDER BY updatedAt DESC
        """
    )
    fun observeTvShows(): Flow<List<LibraryItem>>

    @Query(
        """
        SELECT *
        FROM library_items
        WHERE status = :status
        ORDER BY updatedAt DESC
        """
    )
    fun observeByStatus(
        status: LibraryStatus
    ): Flow<List<LibraryItem>>

    @Query(
        """
        SELECT *
        FROM library_items
        WHERE tmdbId = :tmdbId
          AND mediaType = :mediaType
        LIMIT 1
        """
    )
    suspend fun getItem(
        tmdbId: Int,
        mediaType: String
    ): LibraryItem?

    @Query(
        """
        SELECT EXISTS(
            SELECT 1
            FROM library_items
            WHERE tmdbId = :tmdbId
              AND mediaType = :mediaType
        )
        """
    )
    fun observeIsSaved(
        tmdbId: Int,
        mediaType: String
    ): Flow<Boolean>

    @Query(
        """
        SELECT COUNT(*)
        FROM library_items
        """
    )
    fun observeTotalCount(): Flow<Int>

    @Query(
        """
        SELECT COUNT(*)
        FROM library_items
        WHERE mediaType = 'movie'
        """
    )
    fun observeMovieCount(): Flow<Int>

    @Query(
        """
        SELECT COUNT(*)
        FROM library_items
        WHERE mediaType = 'tv'
        """
    )
    fun observeTvShowCount(): Flow<Int>

    @Query(
        """
        SELECT COUNT(*)
        FROM library_items
        WHERE status = :status
        """
    )
    fun observeStatusCount(
        status: LibraryStatus
    ): Flow<Int>

    @Query(
        """
        DELETE FROM library_items
        WHERE tmdbId = :tmdbId
          AND mediaType = :mediaType
        """
    )
    suspend fun deleteById(
        tmdbId: Int,
        mediaType: String
    )
}
