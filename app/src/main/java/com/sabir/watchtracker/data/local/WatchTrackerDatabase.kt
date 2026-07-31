package com.sabir.watchtracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        LibraryItem::class,
        EpisodeWatch::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(
    LibraryTypeConverters::class
)
abstract class WatchTrackerDatabase : RoomDatabase() {

    abstract fun libraryItemDao(): LibraryItemDao

    abstract fun episodeWatchDao(): EpisodeWatchDao

    companion object {

        private const val DATABASE_NAME =
            "watchtracker_database"

        private val migration1To2 = object : Migration(
            startVersion = 1,
            endVersion = 2
        ) {
            override fun migrate(
                database: SupportSQLiteDatabase
            ) {
                database.execSQL(
                    """
                    ALTER TABLE library_items
                    ADD COLUMN totalSeasons INTEGER
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    ALTER TABLE library_items
                    ADD COLUMN totalEpisodes INTEGER
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS episode_watches (
                        tmdbShowId INTEGER NOT NULL,
                        seasonNumber INTEGER NOT NULL,
                        episodeNumber INTEGER NOT NULL,
                        episodeName TEXT NOT NULL,
                        watchedDateEpochDay INTEGER NOT NULL,
                        runtimeMinutes INTEGER,
                        PRIMARY KEY (
                            tmdbShowId,
                            seasonNumber,
                            episodeNumber
                        )
                    )
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var instance: WatchTrackerDatabase? = null

        fun getInstance(
            context: Context
        ): WatchTrackerDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    WatchTrackerDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(migration1To2)
                    .build()
                    .also { database ->
                        instance = database
                    }
            }
        }
    }
}
