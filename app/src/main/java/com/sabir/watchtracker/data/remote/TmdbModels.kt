package com.sabir.watchtracker.data.remote

import com.google.gson.annotations.SerializedName

data class TmdbMovieDetails(
    @SerializedName("id")
    val id: Int,

    @SerializedName("runtime")
    val runtime: Int? = null
)

data class TmdbSearchResponse(
    @SerializedName("page")
    val page: Int = 1,

    @SerializedName("results")
    val results: List<TmdbSearchResult> = emptyList(),

    @SerializedName("total_pages")
    val totalPages: Int = 0,

    @SerializedName("total_results")
    val totalResults: Int = 0
)

data class TmdbSearchResult(
    @SerializedName("id")
    val id: Int,

    @SerializedName("media_type")
    val mediaType: String? = null,

    @SerializedName("title")
    val movieTitle: String? = null,

    @SerializedName("name")
    val showTitle: String? = null,

    @SerializedName("original_title")
    val originalMovieTitle: String? = null,

    @SerializedName("original_name")
    val originalShowTitle: String? = null,

    @SerializedName("overview")
    val overview: String = "",

    @SerializedName("poster_path")
    val posterPath: String? = null,

    @SerializedName("backdrop_path")
    val backdropPath: String? = null,

    @SerializedName("release_date")
    val movieReleaseDate: String? = null,

    @SerializedName("first_air_date")
    val firstAirDate: String? = null,

    @SerializedName("vote_average")
    val voteAverage: Double = 0.0
) {
    val displayTitle: String
        get() = movieTitle
            ?: showTitle
            ?: originalMovieTitle
            ?: originalShowTitle
            ?: "Untitled"

    val displayDate: String
        get() = movieReleaseDate
            ?.takeIf { it.isNotBlank() }
            ?: firstAirDate
                ?.takeIf { it.isNotBlank() }
            ?: ""

    val displayYear: String
        get() = displayDate
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
            "https://image.tmdb.org/t/p/w500$path"
        }

    val backdropUrl: String?
        get() = backdropPath?.let { path ->
            "https://image.tmdb.org/t/p/w780$path"
        }
}
