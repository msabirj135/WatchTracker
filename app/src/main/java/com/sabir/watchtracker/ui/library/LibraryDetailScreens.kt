package com.sabir.watchtracker.ui.library

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.sabir.watchtracker.data.local.EpisodeWatch
import com.sabir.watchtracker.data.local.LibraryItem
import com.sabir.watchtracker.data.local.LibraryStatus
import com.sabir.watchtracker.data.local.RewatchRecord
import com.sabir.watchtracker.data.remote.TmdbEpisode
import com.sabir.watchtracker.data.remote.TmdbSeasonDetails
import com.sabir.watchtracker.data.remote.TmdbTvDetails
import com.sabir.watchtracker.ui.components.StarRatingSelector
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DetailBackground = Color(0xFF090B10)
private val DetailSurface = Color(0xFF12151D)
private val DetailSurfaceLight = Color(0xFF1A1E28)
private val DetailPrimary = Color(0xFFE63946)
private val DetailSuccess = Color(0xFF36C98F)
private val DetailWarning = Color(0xFFFFC857)
private val DetailTextPrimary = Color(0xFFF5F5F7)
private val DetailTextSecondary = Color(0xFF9A9DA8)

private val detailDateFormatter = DateTimeFormatter.ofPattern(
    "dd MMM yyyy"
)

@Composable
fun LibraryItemDetailScreen(
    item: LibraryItem,
    onBackClick: () -> Unit,
    onDelete: () -> Unit,
    detailViewModel: LibraryDetailViewModel = viewModel()
) {
    val uiState by detailViewModel.uiState
    var showDeleteConfirmation by remember(item.tmdbId, item.mediaType) {
        mutableStateOf(false)
    }

    LaunchedEffect(item.tmdbId, item.mediaType) {
        detailViewModel.loadItem(item)
    }

    val currentItem = uiState.item ?: item

    if (currentItem.mediaType == "tv") {
        TvShowDetailScreen(
            item = currentItem,
            uiState = uiState,
            onBackClick = onBackClick,
            onDeleteClick = { showDeleteConfirmation = true },
            onMarkNextEpisode = detailViewModel::markNextEpisodeWatched,
            onMarkSeriesCompleted = detailViewModel::markSeriesCompleted,
            onMarkSeasonWatched = detailViewModel::markSeasonWatched,
            onMarkEpisode = detailViewModel::markEpisodeWatched,
            onAddEpisodeRewatch = detailViewModel::addEpisodeRewatch,
            onUnmarkEpisode = detailViewModel::unmarkEpisodeWatched,
            onDeleteRewatch = detailViewModel::deleteRewatch,
            onRatingChange = detailViewModel::updatePersonalRating,
            onClearError = detailViewModel::clearError,
            onRetryLoad = detailViewModel::retryLoadTvDetails
        )
    } else {
        MovieDetailScreen(
            item = currentItem,
            isSaving = uiState.isSaving,
            errorMessage = uiState.errorMessage,
            onBackClick = onBackClick,
            onDeleteClick = { showDeleteConfirmation = true },
            onUpdateWatchDate = detailViewModel::updateMovieWatchDate,
            onAddRewatch = detailViewModel::addMovieRewatch,
            onDeleteRewatch = detailViewModel::deleteRewatch,
            rewatchRecords = uiState.rewatchRecords,
            onRatingChange = detailViewModel::updatePersonalRating,
            onClearError = detailViewModel::clearError
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Remove from library?") },
            text = {
                Text(
                    if (item.mediaType == "tv") {
                        "${item.title} and all its saved episode history will be removed."
                    } else {
                        "${item.title} will be removed from your library and watch history."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    }
                ) { Text("Remove", color = DetailPrimary, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TvShowDetailScreen(
    item: LibraryItem,
    uiState: LibraryDetailUiState,
    onBackClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onMarkNextEpisode: () -> Unit,
    onMarkSeriesCompleted: () -> Unit,
    onMarkSeasonWatched: (TmdbSeasonDetails, Long) -> Unit,
    onMarkEpisode: (TmdbEpisode, Long) -> Unit,
    onAddEpisodeRewatch: (TmdbEpisode, Long) -> Unit,
    onUnmarkEpisode: (TmdbEpisode) -> Unit,
    onDeleteRewatch: (Long) -> Unit,
    onRatingChange: (Double?) -> Unit,
    onClearError: () -> Unit,
    onRetryLoad: () -> Unit
) {
    val context = LocalContext.current

    var expandedSeasons by remember {
        mutableStateOf(setOf<Int>())
    }

    var editingEpisode by remember {
        mutableStateOf<TmdbEpisode?>(null)
    }

    var showCompleteConfirmation by remember {
        mutableStateOf(false)
    }

    val watchedByKey = uiState.episodeWatches.associateBy { watch ->
        watch.seasonNumber to watch.episodeNumber
    }

    LaunchedEffect(uiState.seasons) {
        if (
            expandedSeasons.isEmpty() &&
            uiState.seasons.isNotEmpty()
        ) {
            val initialSeason = item.currentSeason
                ?: uiState.seasons.first().seasonNumber

            expandedSeasons = setOf(initialSeason)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DetailBackground)
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 18.dp,
            end = 20.dp,
            bottom = 36.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            DetailHeader(
                title = "TV Show",
                onBackClick = onBackClick
            )
        }

        item {
            TitleOverviewCard(item)
        }

        item {
            SeriesIntelligenceCard(
                details = uiState.tvDetails,
                availableEpisodeCount = uiState.availableEpisodeCount,
                totalEpisodeCount = uiState.totalEpisodeCount,
                isCaughtUp = uiState.isCaughtUp,
                nextUpcomingEpisode = uiState.nextUpcomingEpisode
            )
        }

        item {
            RemoveFromLibraryButton(onClick = onDeleteClick)
        }

        item {
            RatingEditorCard(
                rating = item.personalRating,
                isSaving = uiState.isSaving,
                onRatingChange = onRatingChange
            )
        }

        item {
            ProgressCard(
                watchedCount = uiState.watchedCount,
                totalCount = uiState.totalEpisodeCount,
                progress = uiState.progress,
                lastWatchedDate = item.watchDateEpochDay,
                nextEpisode = uiState.nextEpisode,
                nextUpcomingEpisode = uiState.nextUpcomingEpisode,
                isCaughtUp = uiState.isCaughtUp,
                isSaving = uiState.isSaving,
                onMarkNextEpisode = onMarkNextEpisode
            )
        }

        if (
            uiState.nextEpisode != null
        ) {
            item {
                OutlinedButton(
                    onClick = { showCompleteConfirmation = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSaving,
                    shape = RoundedCornerShape(13.dp)
                ) {
                    Text(
                        text = "Mark all aired episodes watched",
                        color = DetailSuccess,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (uiState.errorMessage != null) {
            item {
                ErrorCard(
                    message = uiState.errorMessage,
                    onDismiss = onClearError,
                    onRetry = onRetryLoad
                )
            }
        }

        if (uiState.isLoading) {
            item {
                LoadingDetails()
            }
        } else {
            item {
                Text(
                    text = "Episodes",
                    color = DetailTextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            items(
                items = uiState.seasons,
                key = { season -> season.seasonNumber }
            ) { season ->
                SeasonCard(
                    season = season,
                    expanded = season.seasonNumber in
                        expandedSeasons,
                    watchedByKey = watchedByKey,
                    isSaving = uiState.isSaving,
                    onToggle = {
                        expandedSeasons =
                            if (
                                season.seasonNumber in
                                expandedSeasons
                            ) {
                                expandedSeasons -
                                    season.seasonNumber
                            } else {
                                expandedSeasons +
                                    season.seasonNumber
                            }
                    },
                    onMarkRemaining = {
                        showDatePicker(
                            context = context,
                            initialEpochDay = LocalDate
                                .now()
                                .toEpochDay(),
                            onDateSelected = { epochDay ->
                                onMarkSeasonWatched(
                                    season,
                                    epochDay
                                )
                            }
                        )
                    },
                    onEpisodeClick = { episode ->
                        val existingWatch = watchedByKey[
                            episode.seasonNumber to
                                episode.episodeNumber
                        ]

                        if (existingWatch != null) {
                            editingEpisode = episode
                        } else {
                            showDatePicker(
                                context = context,
                                initialEpochDay = LocalDate
                                    .now()
                                    .toEpochDay(),
                                onDateSelected = { epochDay ->
                                    onMarkEpisode(
                                        episode,
                                        epochDay
                                    )
                                }
                            )
                        }
                    }
                )
            }
        }
    }

    if (showCompleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showCompleteConfirmation = false },
            title = { Text("Mark series completed?") },
            text = {
                Text(
                    "All remaining aired episodes will be marked as watched today. Future episodes will stay untouched."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCompleteConfirmation = false
                        onMarkSeriesCompleted()
                    }
                ) {
                    Text("Mark aired episodes", color = DetailSuccess, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCompleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    editingEpisode?.let { episode ->
        val episodeWatch = watchedByKey[
            episode.seasonNumber to episode.episodeNumber
        ]

        if (episodeWatch != null) {
            EditEpisodeDialog(
                episode = episode,
                episodeWatch = episodeWatch,
                rewatchRecords = uiState.rewatchRecords.filter { record ->
                    record.seasonNumber == episode.seasonNumber &&
                        record.episodeNumber == episode.episodeNumber
                },
                isSaving = uiState.isSaving,
                onDismiss = {
                    if (!uiState.isSaving) {
                        editingEpisode = null
                    }
                },
                onChangeDate = {
                    showDatePicker(
                        context = context,
                        initialEpochDay =
                            episodeWatch.watchedDateEpochDay,
                        onDateSelected = { epochDay ->
                            onMarkEpisode(
                                episode,
                                epochDay
                            )
                            editingEpisode = null
                        }
                    )
                },
                onAddRewatch = {
                    showDatePicker(
                        context = context,
                        initialEpochDay = LocalDate.now().toEpochDay(),
                        onDateSelected = { epochDay ->
                            onAddEpisodeRewatch(episode, epochDay)
                            editingEpisode = null
                        }
                    )
                },
                onDeleteRewatch = onDeleteRewatch,
                onMarkUnwatched = {
                    onUnmarkEpisode(episode)
                    editingEpisode = null
                }
            )
        }
    }
}

@Composable
private fun MovieDetailScreen(
    item: LibraryItem,
    isSaving: Boolean,
    errorMessage: String?,
    onBackClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onUpdateWatchDate: (Long?) -> Unit,
    onAddRewatch: (Long) -> Unit,
    onDeleteRewatch: (Long) -> Unit,
    rewatchRecords: List<RewatchRecord>,
    onRatingChange: (Double?) -> Unit,
    onClearError: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DetailBackground)
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 18.dp,
            end = 20.dp,
            bottom = 36.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            DetailHeader(
                title = "Movie",
                onBackClick = onBackClick
            )
        }

        item {
            TitleOverviewCard(item)
        }

        item {
            RemoveFromLibraryButton(onClick = onDeleteClick)
        }

        item {
            RatingEditorCard(
                rating = item.personalRating,
                isSaving = isSaving,
                onRatingChange = onRatingChange
            )
        }

        if (item.status == LibraryStatus.WATCHING) {
            item {
                Button(
                    onClick = {
                        showDatePicker(
                            context = context,
                            initialEpochDay = LocalDate.now().toEpochDay(),
                            onDateSelected = onUpdateWatchDate
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DetailSuccess
                    ),
                    shape = RoundedCornerShape(13.dp)
                ) {
                    Text(
                        text = "Mark movie completed",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = DetailSurface
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text(
                        text = "Watched date",
                        color = DetailTextSecondary,
                        fontSize = 13.sp
                    )

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    Text(
                        text = item.watchDateEpochDay
                            ?.let(::formatDetailDate)
                            ?: "Not watched yet",
                        color = DetailTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    Button(
                        onClick = {
                            showDatePicker(
                                context = context,
                                initialEpochDay =
                                    item.watchDateEpochDay
                                        ?: LocalDate.now()
                                            .toEpochDay(),
                                onDateSelected =
                                    onUpdateWatchDate
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DetailPrimary
                        ),
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        Text(
                            text = if (
                                item.watchDateEpochDay == null
                            ) {
                                "Mark as watched"
                            } else {
                                "Change watched date"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (item.watchDateEpochDay != null) {
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                showDatePicker(
                                    context = context,
                                    initialEpochDay = LocalDate.now().toEpochDay(),
                                    onDateSelected = onAddRewatch
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isSaving,
                            shape = RoundedCornerShape(13.dp)
                        ) {
                            Text(
                                text = "Add another watch",
                                color = DetailSuccess,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (rewatchRecords.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Rewatches",
                            color = DetailTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        rewatchRecords.forEach { record ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatDetailDate(record.watchedDateEpochDay),
                                    modifier = Modifier.weight(1f),
                                    color = DetailTextPrimary,
                                    fontSize = 13.sp
                                )
                                TextButton(
                                    onClick = { onDeleteRewatch(record.id) },
                                    enabled = !isSaving
                                ) {
                                    Text("Remove", color = DetailPrimary, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    if (
                        item.watchDateEpochDay != null &&
                        rewatchRecords.isEmpty()
                    ) {
                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        TextButton(
                            onClick = {
                                onUpdateWatchDate(null)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isSaving
                        ) {
                            Text(
                                text = "Mark as not watched",
                                color = DetailTextSecondary
                            )
                        }
                    }
                }
            }
        }

        if (item.notes.isNotBlank()) {
            item {
                DetailInformationCard(
                    label = "Notes",
                    value = item.notes
                )
            }
        }

        if (errorMessage != null) {
            item {
                ErrorCard(
                    message = errorMessage,
                    onDismiss = onClearError
                )
            }
        }
    }
}

@Composable
private fun DetailHeader(
    title: String,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onBackClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = DetailSurfaceLight,
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "←",
                color = DetailTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(
            modifier = Modifier.width(10.dp)
        )

        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = DetailTextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RemoveFromLibraryButton(
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(13.dp)
    ) {
        Text(
            text = "Remove from library",
            color = DetailPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RatingEditorCard(
    rating: Double?,
    isSaving: Boolean,
    onRatingChange: (Double?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DetailSurface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Your rating",
                color = DetailTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            StarRatingSelector(
                rating = rating,
                onRatingChange = onRatingChange,
                enabled = !isSaving
            )
        }
    }
}

@Composable
private fun TitleOverviewCard(
    item: LibraryItem
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = DetailSurface
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(108.dp)
                    .height(162.dp)
                    .background(
                        color = DetailSurfaceLight,
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (item.posterUrl != null) {
                    AsyncImage(
                        model = item.posterUrl,
                        contentDescription =
                            "${item.title} poster",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = item.title
                            .take(2)
                            .uppercase(),
                        color = DetailPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(
                modifier = Modifier.width(16.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.title,
                    color = DetailTextPrimary,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = "${item.displayYear} • ${item.status.displayName}",
                    color = DetailPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                if (item.overview.isNotBlank()) {
                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Text(
                        text = item.overview,
                        color = DetailTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SeriesIntelligenceCard(
    details: TmdbTvDetails?,
    availableEpisodeCount: Int,
    totalEpisodeCount: Int,
    isCaughtUp: Boolean,
    nextUpcomingEpisode: TmdbEpisode?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DetailSurface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Series information",
                        color = DetailTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = details?.productionStatus
                            ?.takeIf { it.isNotBlank() }
                            ?: "Status unavailable",
                        color = if (details?.inProduction == true) {
                            DetailSuccess
                        } else {
                            DetailTextSecondary
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (isCaughtUp) {
                    Text(
                        text = "✓ Caught up",
                        color = DetailSuccess,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "$availableEpisodeCount of $totalEpisodeCount episodes currently aired",
                color = DetailTextSecondary,
                fontSize = 12.sp
            )

            nextUpcomingEpisode?.let { episode ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = buildString {
                        append("Next: ")
                        append(episode.episodeCode)
                        append(" • ")
                        append(episode.name)
                    },
                    color = DetailPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = episode.parsedAirDate?.format(
                        DateTimeFormatter.ofPattern("dd MMM yyyy")
                    )?.let { "Airs $it" } ?: "Air date TBA",
                    color = DetailTextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun ProgressCard(
    watchedCount: Int,
    totalCount: Int,
    progress: Float,
    lastWatchedDate: Long?,
    nextEpisode: TmdbEpisode?,
    nextUpcomingEpisode: TmdbEpisode?,
    isCaughtUp: Boolean,
    isSaving: Boolean,
    onMarkNextEpisode: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = DetailSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Overall progress",
                        color = DetailTextSecondary,
                        fontSize = 13.sp
                    )

                    Text(
                        text = "$watchedCount / $totalCount episodes",
                        color = DetailTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "${(progress * 100).toInt()}%",
                    color = DetailPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = DetailPrimary,
                trackColor = DetailSurfaceLight
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = lastWatchedDate?.let { epochDay ->
                    "Last watched: ${formatDetailDate(epochDay)}"
                } ?: "No episodes watched yet",
                color = DetailTextSecondary,
                fontSize = 12.sp
            )

            if (nextEpisode != null) {
                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                Button(
                    onClick = onMarkNextEpisode,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DetailPrimary
                    ),
                    shape = RoundedCornerShape(13.dp)
                ) {
                    Text(
                        text = if (isSaving) {
                            "Saving..."
                        } else {
                            "Mark ${nextEpisode.episodeCode} watched today"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (isCaughtUp) {
                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Text(
                    text = if (nextUpcomingEpisode != null) {
                        "✓ Caught up • waiting for ${nextUpcomingEpisode.episodeCode}"
                    } else {
                        "✓ Caught up with all aired episodes"
                    },
                    color = DetailSuccess,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SeasonCard(
    season: TmdbSeasonDetails,
    expanded: Boolean,
    watchedByKey: Map<Pair<Int, Int>, EpisodeWatch>,
    isSaving: Boolean,
    onToggle: () -> Unit,
    onMarkRemaining: () -> Unit,
    onEpisodeClick: (TmdbEpisode) -> Unit
) {
    val watchedCount = season.episodes.count { episode ->
        watchedByKey.containsKey(
            episode.seasonNumber to episode.episodeNumber
        )
    }

    val remainingCount = season.episodes.count { episode ->
        episode.hasAired && !watchedByKey.containsKey(
            episode.seasonNumber to episode.episodeNumber
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onToggle,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = DetailSurface
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(17.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = season.name,
                        color = DetailTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "$watchedCount / ${season.episodes.size} watched",
                        color = DetailTextSecondary,
                        fontSize = 12.sp
                    )
                }

                Text(
                    text = if (expanded) "⌃" else "⌄",
                    color = DetailPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (expanded) {
                if (remainingCount > 0) {
                    TextButton(
                        onClick = onMarkRemaining,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        enabled = !isSaving
                    ) {
                        Text(
                            text = "Mark $remainingCount remaining watched",
                            color = DetailSuccess,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                season.episodes
                    .sortedBy { episode ->
                        episode.episodeNumber
                    }
                    .forEach { episode ->
                        val watch = watchedByKey[
                            episode.seasonNumber to
                                episode.episodeNumber
                        ]

                        EpisodeRow(
                            episode = episode,
                            watch = watch,
                            enabled = episode.hasAired || watch != null,
                            onClick = {
                                onEpisodeClick(episode)
                            }
                        )
                    }
            }
        }
    }
}

@Composable
private fun EpisodeRow(
    episode: TmdbEpisode,
    watch: EpisodeWatch?,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 12.dp,
                end = 12.dp,
                bottom = 10.dp
            ),
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (watch != null) {
                DetailSuccess.copy(alpha = 0.10f)
            } else {
                DetailSurfaceLight
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        color = if (watch != null) {
                            DetailSuccess.copy(alpha = 0.18f)
                        } else {
                            DetailPrimary.copy(alpha = 0.13f)
                        },
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (watch != null) "✓" else
                        episode.episodeNumber.toString(),
                    color = if (watch != null) {
                        DetailSuccess
                    } else {
                        DetailPrimary
                    },
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${episode.episodeCode} • ${episode.name}",
                    color = DetailTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = watch?.let {
                        "Watched ${formatDetailDate(it.watchedDateEpochDay)}"
                    } ?: episode.parsedAirDate?.let { airDate ->
                        if (episode.hasAired) {
                            "Aired ${airDate.format(detailDateFormatter)}"
                        } else {
                            "Airs ${airDate.format(detailDateFormatter)}"
                        }
                    } ?: "Not watched",
                    color = if (watch != null) {
                        DetailSuccess
                    } else {
                        DetailTextSecondary
                    },
                    fontSize = 11.sp
                )
            }

            Text(
                text = when {
                    watch != null -> "Edit"
                    episode.hasAired -> "Add"
                    else -> "Upcoming"
                },
                color = if (enabled) DetailPrimary else DetailTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EditEpisodeDialog(
    episode: TmdbEpisode,
    episodeWatch: EpisodeWatch,
    rewatchRecords: List<RewatchRecord>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onChangeDate: () -> Unit,
    onAddRewatch: () -> Unit,
    onDeleteRewatch: (Long) -> Unit,
    onMarkUnwatched: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = episode.episodeCode,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = episode.name,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onAddRewatch,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                ) {
                    Text(
                        text = "Add another watch",
                        color = DetailSuccess,
                        fontWeight = FontWeight.Bold
                    )
                }

                rewatchRecords.forEach { record ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Rewatched ${formatDetailDate(record.watchedDateEpochDay)}",
                            modifier = Modifier.weight(1f),
                            fontSize = 12.sp
                        )
                        TextButton(
                            onClick = { onDeleteRewatch(record.id) },
                            enabled = !isSaving
                        ) {
                            Text("Remove", color = DetailPrimary, fontSize = 10.sp)
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "Watched ${
                        formatDetailDate(
                            episodeWatch.watchedDateEpochDay
                        )
                    }"
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onChangeDate,
                enabled = !isSaving
            ) {
                Text("Change date")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = onMarkUnwatched,
                    enabled = !isSaving && rewatchRecords.isEmpty()
                ) {
                    Text(
                        text = if (rewatchRecords.isEmpty()) {
                            "Mark unwatched"
                        } else {
                            "Remove rewatches first"
                        },
                        color = DetailPrimary
                    )
                }

                TextButton(
                    onClick = onDismiss,
                    enabled = !isSaving
                ) {
                    Text("Cancel")
                }
            }
        },
        containerColor = DetailSurface,
        titleContentColor = DetailTextPrimary,
        textContentColor = DetailTextSecondary
    )
}

@Composable
private fun DetailInformationCard(
    label: String,
    value: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = DetailSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = label,
                color = DetailTextSecondary,
                fontSize = 12.sp
            )

            Spacer(
                modifier = Modifier.height(5.dp)
            )

            Text(
                text = value,
                color = DetailTextPrimary,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun LoadingDetails() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = DetailPrimary
        )

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        Text(
            text = "Loading seasons and episodes...",
            color = DetailTextSecondary,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun ErrorCard(
    message: String,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DetailPrimary.copy(alpha = 0.12f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                color = DetailPrimary,
                fontSize = 12.sp
            )

            if (onRetry != null) {
                TextButton(onClick = onRetry) {
                    Text(
                        text = "Retry",
                        color = DetailPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                TextButton(
                    onClick = onDismiss
                ) {
                    Text(
                        text = "×",
                        color = DetailPrimary
                    )
                }
            }
        }
    }
}

private fun showDatePicker(
    context: android.content.Context,
    initialEpochDay: Long,
    onDateSelected: (Long) -> Unit
) {
    val initialDate = LocalDate.ofEpochDay(
        initialEpochDay
    )

    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            onDateSelected(
                LocalDate.of(
                    year,
                    month + 1,
                    dayOfMonth
                ).toEpochDay()
            )
        },
        initialDate.year,
        initialDate.monthValue - 1,
        initialDate.dayOfMonth
    ).show()
}

private fun formatDetailDate(
    epochDay: Long
): String {
    return LocalDate
        .ofEpochDay(epochDay)
        .format(detailDateFormatter)
}
