package com.sabir.watchtracker.data.local

import androidx.room.Entity
import com.sabir.watchtracker.BuildConfig

@Entity(
    tableName = "library_items",
    primaryKeys = [
        "tmdbId",
        "mediaType"
    ]
)
data class LibraryItem(
    val tmdbId: Int,
    val mediaType: String,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: String,
    val tmdbRating: Double,
    val status: LibraryStatus,
    val watchDateEpochDay: Long?,
    val personalRating: Double?,
    val notes: String,
    val currentSeason: Int?,
    val currentEpisode: Int?,
    val totalSeasons: Int?,
    val totalEpisodes: Int?,
    val runtimeMinutes: Int?,
    val genreNames: String? = null,
    val watchMethod: String? = null,
    val originalTitle: String? = null,
    val originalLanguage: String? = null,
    val addedAt: Long,
    val updatedAt: Long
) {
    val displayYear: String
        get() = releaseDate
            .takeIf { it.length >= 4 }
            ?.take(4)
            ?: "—"

    val displayMediaType: String
        get() = when (mediaType) {
            "movie" -> "Movie"
            "tv" -> "TV Show"
            else -> "Unknown"
        }

    val posterUrl: String?
        get() = posterPath?.let { path ->
            "${BuildConfig.TMDB_PROXY_BASE_URL.trimEnd('/')}/image/t/p/w500$path"
        }

    val backdropUrl: String?
        get() = backdropPath?.let { path ->
            "${BuildConfig.TMDB_PROXY_BASE_URL.trimEnd('/')}/image/t/p/w780$path"
        }

    val genres: List<String>
        get() = genreNames
            ?.split("|")
            ?.map { value -> value.trim() }
            ?.filter { value -> value.isNotBlank() }
            .orEmpty()

    val episodeProgressText: String?
        get() {
            if (mediaType != "tv") {
                return null
            }

            val season = currentSeason
                ?: return "Not started"

            val episode = currentEpisode
                ?: return "Not started"

            return "S%02dE%02d".format(
                season,
                episode
            )
        }
}
