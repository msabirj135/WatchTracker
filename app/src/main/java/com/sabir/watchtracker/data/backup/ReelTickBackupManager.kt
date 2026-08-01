package com.sabir.watchtracker.data.backup

import android.content.Context
import androidx.room.withTransaction
import com.google.gson.GsonBuilder
import com.sabir.watchtracker.data.local.CustomList
import com.sabir.watchtracker.data.local.CustomListItem
import com.sabir.watchtracker.data.local.EpisodeWatch
import com.sabir.watchtracker.data.local.LibraryItem
import com.sabir.watchtracker.data.local.RewatchRecord
import com.sabir.watchtracker.data.local.WatchTrackerDatabase
import java.time.LocalDate

private const val BACKUP_IDENTIFIER =
    "com.sabir.watchtracker.reeltick-backup"

private const val CURRENT_BACKUP_VERSION = 2

enum class BackupImportMode {
    MERGE,
    REPLACE
}

data class BackupPreview(
    val titleCount: Int,
    val episodeCount: Int,
    val rewatchCount: Int,
    val customListCount: Int,
    val exportedAtEpochMillis: Long
)

data class BackupRestoreResult(
    val titleCount: Int,
    val episodeCount: Int,
    val rewatchCount: Int,
    val customListCount: Int
)

private data class ReelTickBackupDocument(
    val identifier: String = BACKUP_IDENTIFIER,
    val backupVersion: Int = CURRENT_BACKUP_VERSION,
    val exportedAtEpochMillis: Long = System.currentTimeMillis(),
    val libraryItems: List<LibraryItem> = emptyList(),
    val episodeWatches: List<EpisodeWatch> = emptyList(),
    val rewatchRecords: List<RewatchRecord>? = emptyList(),
    val customLists: List<CustomList> = emptyList(),
    val customListItems: List<CustomListItem> = emptyList()
)

class ReelTickBackupManager(
    context: Context
) {
    private val database = WatchTrackerDatabase.getInstance(
        context.applicationContext
    )

    private val libraryItemDao = database.libraryItemDao()
    private val episodeWatchDao = database.episodeWatchDao()
    private val customListDao = database.customListDao()
    private val rewatchRecordDao = database.rewatchRecordDao()

    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    suspend fun createBackupJson(): String {
        val document = database.withTransaction {
            ReelTickBackupDocument(
                exportedAtEpochMillis = System.currentTimeMillis(),
                libraryItems = libraryItemDao.getAllSnapshot(),
                episodeWatches = episodeWatchDao.getAllSnapshot(),
                rewatchRecords = rewatchRecordDao.getAllSnapshot(),
                customLists = customListDao.getListsSnapshot(),
                customListItems = customListDao.getItemsSnapshot()
            )
        }

        return gson.toJson(document)
    }

    suspend fun createHistoryCsv(): String {
        val snapshot = database.withTransaction {
            ReelTickBackupDocument(
                libraryItems = libraryItemDao.getAllSnapshot(),
                episodeWatches = episodeWatchDao.getAllSnapshot(),
                rewatchRecords = rewatchRecordDao.getAllSnapshot()
            )
        }

        val itemsByKey = snapshot.libraryItems.associateBy { item ->
            item.mediaType to item.tmdbId
        }

        val rows = mutableListOf<List<String>>()
        rows += listOf(
            "Title",
            "Media Type",
            "Episode",
            "Episode Name",
            "Watched Date",
            "Status",
            "Personal Rating",
            "Runtime Minutes",
            "Notes"
        )

        snapshot.libraryItems
            .filter { item -> item.mediaType == "movie" }
            .sortedByDescending { item ->
                item.watchDateEpochDay ?: Long.MIN_VALUE
            }
            .forEach { movie ->
                rows += listOf(
                    movie.title,
                    "Movie",
                    "",
                    "",
                    movie.watchDateEpochDay
                        ?.let { LocalDate.ofEpochDay(it).toString() }
                        .orEmpty(),
                    movie.status.displayName,
                    movie.personalRating?.toString().orEmpty(),
                    movie.runtimeMinutes?.toString().orEmpty(),
                    movie.notes
                )
            }

        snapshot.episodeWatches
            .sortedByDescending { watch ->
                watch.watchedDateEpochDay
            }
            .forEach { watch ->
                val show = itemsByKey["tv" to watch.tmdbShowId]
                    ?: return@forEach

                rows += listOf(
                    show.title,
                    "TV Show",
                    watch.episodeCode,
                    watch.episodeName,
                    LocalDate.ofEpochDay(
                        watch.watchedDateEpochDay
                    ).toString(),
                    show.status.displayName,
                    show.personalRating?.toString().orEmpty(),
                    watch.runtimeMinutes?.toString().orEmpty(),
                    show.notes
                )
            }

        snapshot.rewatchRecords.orEmpty()
            .sortedByDescending { record -> record.watchedDateEpochDay }
            .forEach { record ->
                val item = itemsByKey[record.mediaType to record.tmdbId]
                    ?: return@forEach

                rows += listOf(
                    item.title,
                    if (record.mediaType == "movie") {
                        "Movie rewatch"
                    } else {
                        "TV Show rewatch"
                    },
                    record.episodeCode.orEmpty(),
                    record.episodeName,
                    LocalDate.ofEpochDay(
                        record.watchedDateEpochDay
                    ).toString(),
                    item.status.displayName,
                    item.personalRating?.toString().orEmpty(),
                    record.runtimeMinutes?.toString().orEmpty(),
                    item.notes
                )
            }

        return rows.joinToString("\n") { row ->
            row.joinToString(",") { value ->
                "\"${value.replace("\"", "\"\"")}\""
            }
        }
    }

    suspend fun clearAllData() {
        database.withTransaction {
            customListDao.deleteAllItems()
            customListDao.deleteAllLists()
            episodeWatchDao.deleteAll()
            rewatchRecordDao.deleteAll()
            libraryItemDao.deleteAll()
        }
    }

    fun inspectBackupJson(json: String): BackupPreview {
        val document = parseAndValidate(json)

        return BackupPreview(
            titleCount = document.libraryItems.size,
            episodeCount = document.episodeWatches.size,
            rewatchCount = document.rewatchRecords.orEmpty().size,
            customListCount = document.customLists.size,
            exportedAtEpochMillis = document.exportedAtEpochMillis
        )
    }

    suspend fun restoreBackupJson(
        json: String,
        mode: BackupImportMode
    ): BackupRestoreResult {
        val document = parseAndValidate(json)

        database.withTransaction {
            when (mode) {
                BackupImportMode.REPLACE ->
                    replaceEverything(document)

                BackupImportMode.MERGE ->
                    mergeWithExisting(document)
            }
        }

        return BackupRestoreResult(
            titleCount = document.libraryItems.size,
            episodeCount = document.episodeWatches.size,
            rewatchCount = document.rewatchRecords.orEmpty().size,
            customListCount = document.customLists.size
        )
    }

    private suspend fun replaceEverything(
        document: ReelTickBackupDocument
    ) {
        customListDao.deleteAllItems()
        customListDao.deleteAllLists()
        episodeWatchDao.deleteAll()
        rewatchRecordDao.deleteAll()
        libraryItemDao.deleteAll()

        libraryItemDao.upsertAll(document.libraryItems)
        episodeWatchDao.upsertAll(document.episodeWatches)
        rewatchRecordDao.upsertAll(document.rewatchRecords.orEmpty())
        customListDao.upsertLists(document.customLists)
        customListDao.upsertItems(document.customListItems)
    }

    private suspend fun mergeWithExisting(
        document: ReelTickBackupDocument
    ) {
        val existingItems = libraryItemDao
            .getAllSnapshot()
            .associateBy { item ->
                item.tmdbId to item.mediaType
            }

        val mergedItems = document.libraryItems.map { backupItem ->
            val existingItem = existingItems[
                backupItem.tmdbId to backupItem.mediaType
            ]

            if (
                existingItem != null &&
                existingItem.updatedAt > backupItem.updatedAt
            ) {
                existingItem
            } else {
                backupItem
            }
        }

        libraryItemDao.upsertAll(mergedItems)
        episodeWatchDao.upsertAll(document.episodeWatches)

        val existingRewatchKeys = rewatchRecordDao.getAllSnapshot()
            .map { record ->
                listOf(
                    record.tmdbId,
                    record.mediaType,
                    record.seasonNumber,
                    record.episodeNumber,
                    record.watchedDateEpochDay
                )
            }
            .toSet()

        val newRewatches = document.rewatchRecords.orEmpty()
            .filter { record ->
                listOf(
                    record.tmdbId,
                    record.mediaType,
                    record.seasonNumber,
                    record.episodeNumber,
                    record.watchedDateEpochDay
                ) !in existingRewatchKeys
            }
            .map { record -> record.copy(id = 0) }

        rewatchRecordDao.insertAll(newRewatches)

        val existingLists = customListDao.getListsSnapshot()
        val listIdMapping = mutableMapOf<Long, Long>()

        document.customLists.forEach { backupList ->
            val matchingList = existingLists.firstOrNull { existing ->
                existing.name.equals(
                    backupList.name,
                    ignoreCase = true
                )
            }

            val destinationId = if (matchingList != null) {
                if (backupList.updatedAt >= matchingList.updatedAt) {
                    customListDao.upsertList(
                        backupList.copy(id = matchingList.id)
                    )
                }
                matchingList.id
            } else {
                customListDao.insertList(
                    backupList.copy(id = 0)
                )
            }

            listIdMapping[backupList.id] = destinationId
        }

        val mappedListItems = document.customListItems.mapNotNull { item ->
            listIdMapping[item.listId]?.let { destinationListId ->
                item.copy(listId = destinationListId)
            }
        }

        customListDao.upsertItems(mappedListItems)
    }

    private fun parseAndValidate(
        json: String
    ): ReelTickBackupDocument {
        require(json.isNotBlank()) {
            "The selected backup file is empty."
        }

        val document = runCatching {
            gson.fromJson(
                json,
                ReelTickBackupDocument::class.java
            )
        }.getOrElse {
            throw IllegalArgumentException(
                "This is not a valid ReelTick backup file."
            )
        } ?: throw IllegalArgumentException(
            "This is not a valid ReelTick backup file."
        )

        require(document.identifier == BACKUP_IDENTIFIER) {
            "This file was not created by ReelTick."
        }

        require(document.backupVersion in 1..CURRENT_BACKUP_VERSION) {
            "This backup was created by a newer ReelTick version."
        }

        require(document.exportedAtEpochMillis > 0) {
            "The backup date is invalid."
        }

        val libraryKeys = document.libraryItems.map { item ->
            require(item.tmdbId > 0) {
                "The backup contains an invalid title."
            }
            require(item.mediaType == "movie" || item.mediaType == "tv") {
                "The backup contains an unsupported media type."
            }
            require(item.title.isNotBlank()) {
                "The backup contains a title without a name."
            }
            item.tmdbId to item.mediaType
        }

        require(libraryKeys.size == libraryKeys.distinct().size) {
            "The backup contains duplicate titles."
        }

        val tvShowIds = document.libraryItems
            .filter { item -> item.mediaType == "tv" }
            .map { item -> item.tmdbId }
            .toSet()

        document.episodeWatches.forEach { episode ->
            require(episode.tmdbShowId in tvShowIds) {
                "The backup contains an episode without its TV show."
            }
            require(
                episode.seasonNumber >= 0 &&
                    episode.episodeNumber > 0 &&
                    episode.watchedDateEpochDay > 0
            ) {
                "The backup contains invalid episode information."
            }
        }

        document.rewatchRecords.orEmpty().forEach { record ->
            require(
                (record.tmdbId to record.mediaType) in libraryKeys.toSet()
            ) {
                "The backup contains a rewatch without its title."
            }
            require(record.watchedDateEpochDay > 0) {
                "The backup contains an invalid rewatch date."
            }
        }

        val listIds = document.customLists.map { list -> list.id }
        require(listIds.size == listIds.distinct().size) {
            "The backup contains duplicate custom lists."
        }

        val libraryKeySet = libraryKeys.toSet()
        val listIdSet = listIds.toSet()

        document.customListItems.forEach { listItem ->
            require(listItem.listId in listIdSet) {
                "The backup contains an invalid list membership."
            }
            require(
                (listItem.tmdbId to listItem.mediaType) in libraryKeySet
            ) {
                "The backup contains a missing list title."
            }
        }

        return document
    }
}
