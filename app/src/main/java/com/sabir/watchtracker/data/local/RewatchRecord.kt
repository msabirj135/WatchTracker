package com.sabir.watchtracker.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rewatch_records",
    indices = [
        Index(value = ["tmdbId", "mediaType"])
    ]
)
data class RewatchRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tmdbId: Int,
    val mediaType: String,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val episodeName: String,
    val watchedDateEpochDay: Long,
    val runtimeMinutes: Int?,
    val createdAt: Long,
    val watchMethod: String? = null
) {
    val episodeCode: String?
        get() = if (
            seasonNumber != null &&
            episodeNumber != null
        ) {
            "S%02dE%02d".format(
                seasonNumber,
                episodeNumber
            )
        } else {
            null
        }
}
