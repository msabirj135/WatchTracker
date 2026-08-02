package com.sabir.watchtracker.data.local

import androidx.room.Entity

@Entity(
    tableName = "continue_watching_cache",
    primaryKeys = ["tmdbShowId"]
)
data class ContinueWatchingCache(
    val tmdbShowId: Int,
    val nextEpisodeId: Int?,
    val nextEpisodeName: String?,
    val nextEpisodeOverview: String?,
    val nextSeasonNumber: Int?,
    val nextEpisodeNumber: Int?,
    val nextAirDate: String?,
    val nextRuntimeMinutes: Int?,
    val nextStillPath: String?,
    val nextVoteAverage: Double?,
    val upcomingEpisodeId: Int?,
    val upcomingEpisodeName: String?,
    val upcomingEpisodeOverview: String?,
    val upcomingSeasonNumber: Int?,
    val upcomingEpisodeNumber: Int?,
    val upcomingAirDate: String?,
    val upcomingRuntimeMinutes: Int?,
    val upcomingStillPath: String?,
    val upcomingVoteAverage: Double?,
    val productionStatus: String,
    val sourceUpdatedAt: Long,
    val fetchedAt: Long
)
