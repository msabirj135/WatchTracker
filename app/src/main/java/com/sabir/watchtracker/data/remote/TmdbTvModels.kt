package com.sabir.watchtracker.data.remote

import com.google.gson.annotations.SerializedName
import com.sabir.watchtracker.BuildConfig

data class TmdbTvDetails(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("overview")
    val overview: String = "",

    @SerializedName("poster_path")
    val posterPath: String? = null,

    @SerializedName("backdrop_path")
    val backdropPath: String? = null,

    @SerializedName("first_air_date")
    val firstAirDate: String = "",

    @SerializedName("last_air_date")
    val lastAirDate: String? = null,

    @SerializedName("status")
    val productionStatus: String = "",

    @SerializedName("in_production")
    val inProduction: Boolean = false,

    @SerializedName("next_episode_to_air")
    val nextEpisodeToAir: TmdbEpisode? = null,

    @SerializedName("last_episode_to_air")
    val lastEpisodeToAir: TmdbEpisode? = null,

    @SerializedName("number_of_seasons")
    val numberOfSeasons: Int = 0,

    @SerializedName("number_of_episodes")
    val numberOfEpisodes: Int = 0,

    @SerializedName("seasons")
    val seasons: List<TmdbSeasonSummary> = emptyList()
) {
    val regularSeasons: List<TmdbSeasonSummary>
        get() = seasons
            .filter { season ->
                season.seasonNumber > 0
            }
            .sortedBy { season ->
                season.seasonNumber
            }
}

data class TmdbSeasonSummary(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("season_number")
    val seasonNumber: Int,

    @SerializedName("episode_count")
    val episodeCount: Int = 0,

    @SerializedName("air_date")
    val airDate: String? = null,

    @SerializedName("overview")
    val overview: String = "",

    @SerializedName("poster_path")
    val posterPath: String? = null
)

data class TmdbSeasonDetails(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("overview")
    val overview: String = "",

    @SerializedName("season_number")
    val seasonNumber: Int,

    @SerializedName("air_date")
    val airDate: String? = null,

    @SerializedName("poster_path")
    val posterPath: String? = null,

    @SerializedName("episodes")
    val episodes: List<TmdbEpisode> = emptyList()
)

data class TmdbEpisode(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("overview")
    val overview: String = "",

    @SerializedName("season_number")
    val seasonNumber: Int,

    @SerializedName("episode_number")
    val episodeNumber: Int,

    @SerializedName("air_date")
    val airDate: String? = null,

    @SerializedName("runtime")
    val runtime: Int? = null,

    @SerializedName("still_path")
    val stillPath: String? = null,

    @SerializedName("vote_average")
    val voteAverage: Double = 0.0
) {
    val episodeCode: String
        get() = "S%02dE%02d".format(
            seasonNumber,
            episodeNumber
        )

    val stillUrl: String?
        get() = stillPath?.let { path ->
            "${BuildConfig.TMDB_PROXY_BASE_URL.trimEnd('/')}/image/t/p/w500$path"
        }

    val parsedAirDate: java.time.LocalDate?
        get() = airDate
            ?.takeIf { value -> value.isNotBlank() }
            ?.let { value ->
                runCatching {
                    java.time.LocalDate.parse(value)
                }.getOrNull()
            }

    val hasAired: Boolean
        get() = parsedAirDate
            ?.isAfter(java.time.LocalDate.now())
            ?.not()
            ?: true
}
