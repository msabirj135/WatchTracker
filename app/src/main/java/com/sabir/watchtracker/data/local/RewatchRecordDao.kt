package com.sabir.watchtracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RewatchRecordDao {

    @Query("SELECT * FROM rewatch_records ORDER BY watchedDateEpochDay DESC, createdAt DESC")
    fun observeAll(): Flow<List<RewatchRecord>>

    @Query("SELECT * FROM rewatch_records WHERE tmdbId = :tmdbId AND mediaType = :mediaType ORDER BY watchedDateEpochDay ASC, createdAt ASC")
    fun observeForTitle(
        tmdbId: Int,
        mediaType: String
    ): Flow<List<RewatchRecord>>

    @Query("SELECT * FROM rewatch_records")
    suspend fun getAllSnapshot(): List<RewatchRecord>

    @Insert
    suspend fun insert(record: RewatchRecord): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(records: List<RewatchRecord>)

    @Upsert
    suspend fun upsertAll(records: List<RewatchRecord>)

    @Query("DELETE FROM rewatch_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE rewatch_records SET watchMethod = :watchMethod WHERE id = :id AND mediaType = 'movie'")
    suspend fun updateMovieWatchMethod(id: Long, watchMethod: String)

    @Query("DELETE FROM rewatch_records WHERE tmdbId = :tmdbId AND mediaType = :mediaType")
    suspend fun deleteForTitle(
        tmdbId: Int,
        mediaType: String
    )

    @Query("DELETE FROM rewatch_records")
    suspend fun deleteAll()

    @Query("DELETE FROM rewatch_records WHERE NOT EXISTS (SELECT 1 FROM library_items WHERE library_items.tmdbId = rewatch_records.tmdbId AND library_items.mediaType = rewatch_records.mediaType)")
    suspend fun deleteOrphanRecords(): Int
}
