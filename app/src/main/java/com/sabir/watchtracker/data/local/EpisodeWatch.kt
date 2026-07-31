package com.sabir.watchtracker.data.local

import androidx.room.Entity

@Entity(
    tableName = "episode_watches",
    primaryKeys = [
        "tmdbShowId",
        "seasonNumber",
        "episodeNumber"
    ]
)
data class EpisodeWatch(
    val tmdbShowId: Int,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val episodeName: String,
    val watchedDateEpochDay: Long,
    val runtimeMinutes: Int?
) {
    val episodeCode: String
        get() = "S%02dE%02d".format(
            seasonNumber,
            episodeNumber
        )
}
