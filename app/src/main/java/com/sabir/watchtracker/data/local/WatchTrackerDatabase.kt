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
        EpisodeWatch::class,
        CustomList::class,
        CustomListItem::class,
        RewatchRecord::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(
    LibraryTypeConverters::class
)
abstract class WatchTrackerDatabase : RoomDatabase() {

    abstract fun libraryItemDao(): LibraryItemDao

    abstract fun episodeWatchDao(): EpisodeWatchDao

    abstract fun customListDao(): CustomListDao

    abstract fun rewatchRecordDao(): RewatchRecordDao

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

        private val migration2To3 = object : Migration(
            startVersion = 2,
            endVersion = 3
        ) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE library_items ADD COLUMN runtimeMinutes INTEGER"
                )
            }
        }

        private val migration3To4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS custom_lists (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, description TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)"
                )
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS custom_list_items (listId INTEGER NOT NULL, tmdbId INTEGER NOT NULL, mediaType TEXT NOT NULL, addedAt INTEGER NOT NULL, PRIMARY KEY(listId, tmdbId, mediaType))"
                )
            }
        }

        private val migration4To5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS rewatch_records (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, tmdbId INTEGER NOT NULL, mediaType TEXT NOT NULL, seasonNumber INTEGER, episodeNumber INTEGER, episodeName TEXT NOT NULL, watchedDateEpochDay INTEGER NOT NULL, runtimeMinutes INTEGER, createdAt INTEGER NOT NULL)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_rewatch_records_tmdbId_mediaType ON rewatch_records (tmdbId, mediaType)"
                )
            }
        }

        private val migration5To6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE library_items ADD COLUMN genreNames TEXT"
                )
            }
        }

        private val migration6To7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE custom_lists ADD COLUMN colorKey TEXT"
                )
                database.execSQL(
                    "ALTER TABLE custom_lists ADD COLUMN iconKey TEXT"
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
                    .addMigrations(
                        migration1To2,
                        migration2To3,
                        migration3To4,
                        migration4To5,
                        migration5To6,
                        migration6To7
                    )
                    .build()
                    .also { database ->
                        instance = database
                    }
            }
        }
    }
}
