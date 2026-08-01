package com.sabir.watchtracker.data.remote

import com.google.gson.annotations.SerializedName
import com.sabir.watchtracker.BuildConfig
import java.util.Locale

data class TmdbMovieDetails(
    @SerializedName("id")
    val id: Int,

    @SerializedName("runtime")
    val runtime: Int? = null,

    @SerializedName("genres")
    val genres: List<TmdbGenre> = emptyList()
)

data class TmdbGenre(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String
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

data class TmdbFindResponse(
    @SerializedName("movie_results")
    val movieResults: List<TmdbSearchResult> = emptyList(),

    @SerializedName("tv_results")
    val tvResults: List<TmdbSearchResult> = emptyList()
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

    @SerializedName("original_language")
    val originalLanguage: String? = null,

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
    val voteAverage: Double = 0.0,

    @SerializedName("genre_ids")
    val genreIds: List<Int> = emptyList()
) {
    val displayTitle: String
        get() = movieTitle
            ?: showTitle
            ?: originalMovieTitle
            ?: originalShowTitle
            ?: "Untitled"

    val originalTitle: String?
        get() = (originalMovieTitle ?: originalShowTitle)
            ?.trim()
            ?.takeIf { title ->
                title.isNotBlank() &&
                    !title.equals(displayTitle, ignoreCase = true)
            }

    val displayLanguage: String
        get() {
            val code = originalLanguage
                ?.trim()
                ?.lowercase()
                ?.takeIf { it.isNotBlank() }
                ?: return "Unknown language"

            return Locale.forLanguageTag(code)
                .getDisplayLanguage(Locale.ENGLISH)
                .takeIf { name ->
                    name.isNotBlank() &&
                        !name.equals(code, ignoreCase = true)
                }
                ?.replaceFirstChar { character ->
                    character.titlecase(Locale.ENGLISH)
                }
                ?: code.uppercase(Locale.ENGLISH)
        }

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
            "${BuildConfig.TMDB_PROXY_BASE_URL.trimEnd('/')}/image/t/p/w500$path"
        }

    val backdropUrl: String?
        get() = backdropPath?.let { path ->
            "${BuildConfig.TMDB_PROXY_BASE_URL.trimEnd('/')}/image/t/p/w780$path"
        }
}
