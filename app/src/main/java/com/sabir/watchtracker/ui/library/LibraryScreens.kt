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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.sabir.watchtracker.data.local.LibraryItem
import com.sabir.watchtracker.data.local.LibraryStatus
import com.sabir.watchtracker.data.local.EpisodeWatch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val ScreenBackground = Color(0xFF090B10)
private val ScreenSurface = Color(0xFF12151D)
private val ScreenSurfaceLight = Color(0xFF1A1E28)
private val ScreenPrimary = Color(0xFFE63946)
private val ScreenSuccess = Color(0xFF36C98F)
private val ScreenWarning = Color(0xFFFFC857)
private val ScreenTextPrimary = Color(0xFFF5F5F7)
private val ScreenTextSecondary = Color(0xFF9A9DA8)

@Composable
fun HomeScreen(
    paddingValues: PaddingValues,
    libraryUiState: LibraryUiState,
    upNextUiState: UpNextUiState,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onMoviesClick: () -> Unit,
    onTvShowsClick: () -> Unit,
    onMarkUpNextWatched: (UpNextEntry, Long) -> Unit,
    onRetryUpNext: () -> Unit,
    onItemClick: (LibraryItem) -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .padding(paddingValues),
        contentPadding = PaddingValues(
            bottom = 28.dp
        )
    ) {
        item {
            HomeHeader(
                onSearchClick = onSearchClick,
                onSettingsClick = onSettingsClick
            )
        }

        item {
            HomeSummary(
                movieCount = libraryUiState.movieCount,
                tvShowCount = libraryUiState.tvShowCount,
                totalWatchMinutes = libraryUiState.totalWatchMinutes,
                currentMonthMinutes = libraryUiState.monthlyLists
                    .firstOrNull { month ->
                        val current = YearMonth.now()
                        month.year == current.year && month.month == current.monthValue
                    }
                    ?.totalMinutes
                    ?: 0,
                previousMonthMinutes = libraryUiState.monthlyLists
                    .firstOrNull { month ->
                        val previous = YearMonth.now().minusMonths(1)
                        month.year == previous.year && month.month == previous.monthValue
                    }
                    ?.totalMinutes
                    ?: 0,
                onMoviesClick = onMoviesClick,
                onTvShowsClick = onTvShowsClick
            )
        }

        if (
            libraryUiState.continueWatching.isNotEmpty() ||
            upNextUiState.isLoading ||
            upNextUiState.entries.isNotEmpty() ||
            upNextUiState.upcomingEntries.isNotEmpty() ||
            upNextUiState.errorMessage != null
        ) {
            item {
                SectionHeader(
                    title = "Up next",
                    action = "${libraryUiState.tvQueueCandidates.size} shows"
                )
            }

            when {
                upNextUiState.isLoading &&
                    upNextUiState.entries.isEmpty() -> {
                    item {
                        LoadingBlock()
                    }
                }

                upNextUiState.errorMessage != null &&
                    upNextUiState.entries.isEmpty() -> {
                    item {
                        UpNextErrorCard(
                            message = upNextUiState.errorMessage,
                            onRetry = onRetryUpNext
                        )
                    }
                }

                upNextUiState.entries.isEmpty() &&
                    upNextUiState.upcomingEntries.isEmpty() -> {
                    item {
                        UpNextCaughtUpCard()
                    }
                }

                upNextUiState.entries.isEmpty() -> {
                    item {
                        UpNextWaitingCard()
                    }
                }

                else -> {
                    item {
                        LazyRow(
                            horizontalArrangement =
                                Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(
                                horizontal = 20.dp
                            )
                        ) {
                            items(
                                items = upNextUiState.entries,
                                key = { entry -> entry.key }
                            ) { entry ->
                                UpNextCard(
                                    entry = entry,
                                    isSaving = entry.item.tmdbId in
                                        upNextUiState.savingShowIds,
                                    onOpenShow = {
                                        onItemClick(entry.item)
                                    },
                                    onWatchedToday = {
                                        onMarkUpNextWatched(
                                            entry,
                                            LocalDate.now().toEpochDay()
                                        )
                                    },
                                    onChooseDate = {
                                        showHomeDatePicker(
                                            context = context,
                                            onDateSelected = { epochDay ->
                                                onMarkUpNextWatched(
                                                    entry,
                                                    epochDay
                                                )
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (upNextUiState.upcomingEntries.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Coming soon",
                        action = "${upNextUiState.upcomingEntries.size} shows"
                    )
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp)
                    ) {
                        items(
                            items = upNextUiState.upcomingEntries,
                            key = { entry -> entry.key }
                        ) { entry ->
                            UpcomingEpisodeCard(
                                entry = entry,
                                onOpenShow = { onItemClick(entry.item) }
                            )
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "Watch history",
                action = "See all",
                onActionClick = onHistoryClick
            )
        }

        if (libraryUiState.isLoading) {
            item {
                LoadingBlock()
            }
        } else if (
            libraryUiState.watchHistoryEntries.isEmpty()
        ) {
            item {
                EmptyHistoryCard(
                    onSearchClick = onSearchClick
                )
            }
        } else {
            item {
                LazyRow(
                    horizontalArrangement =
                        Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(
                        horizontal = 20.dp
                    )
                ) {
                    items(
                        items = libraryUiState
                            .watchHistoryEntries
                            .take(10),
                        key = { entry ->
                            entry.key
                        }
                    ) { entry ->
                        HistoryCard(
                            entry = entry,
                            onClick = {
                                onItemClick(entry.item)
                            }
                        )
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "Your library",
                action = "${libraryUiState.totalCount} titles"
            )
        }

        item {
            LibraryOverview(
                totalCount = libraryUiState.totalCount,
                planToWatchCount =
                    libraryUiState.planToWatchCount,
                watchingCount =
                    libraryUiState.watching.size,
                completedCount =
                    libraryUiState.completedCount,
                onSearchClick = onSearchClick
            )
        }
    }
}

@Composable
private fun HomeHeader(
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 20.dp,
                top = 24.dp,
                end = 20.dp,
                bottom = 18.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "REELTICK",
                color = ScreenPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "What are you watching?",
                color = ScreenTextPrimary,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Surface(
            modifier = Modifier.size(46.dp),
            shape = CircleShape,
            color = ScreenSurfaceLight,
            onClick = onSettingsClick
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⚙",
                    color = ScreenTextPrimary,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            modifier = Modifier.size(46.dp),
            shape = CircleShape,
            color = ScreenSurfaceLight,
            onClick = onSearchClick
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⌕",
                    color = ScreenTextPrimary,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun HomeSummary(
    movieCount: Int,
    tvShowCount: Int,
    totalWatchMinutes: Int,
    currentMonthMinutes: Int,
    previousMonthMinutes: Int,
    onMoviesClick: () -> Unit,
    onTvShowsClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard(
                modifier = Modifier.weight(1f),
                value = movieCount.toString(),
                label = "Movies",
                onClick = onMoviesClick
            )

            SummaryCard(
                modifier = Modifier.weight(1f),
                value = tvShowCount.toString(),
                label = "TV Shows",
                onClick = onTvShowsClick
            )
        }

        WatchTimeSummaryCard(
            totalMinutes = totalWatchMinutes,
            currentMonthMinutes = currentMonthMinutes,
            previousMonthMinutes = previousMonthMinutes
        )
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier,
    value: String,
    label: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = ScreenSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                modifier = Modifier.height(44.dp),
                color = ScreenTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                maxLines = 2
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = label,
                color = ScreenTextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun WatchTimeSummaryCard(
    totalMinutes: Int,
    currentMonthMinutes: Int,
    previousMonthMinutes: Int
) {
    val trendText = when {
        currentMonthMinutes == 0 && previousMonthMinutes == 0 -> "No monthly activity yet"
        previousMonthMinutes == 0 -> "New activity this month"
        currentMonthMinutes == previousMonthMinutes -> "Same as last month"
        else -> {
            val difference = ((currentMonthMinutes - previousMonthMinutes) * 100) /
                previousMonthMinutes
            if (difference > 0) "↑ $difference% vs last month" else "↓ ${-difference}% vs last month"
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ScreenSurface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "TOTAL WATCH TIME",
                        color = ScreenPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        formatWatchHours(totalMinutes),
                        color = ScreenTextPrimary,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(ScreenPrimary.copy(alpha = 0.13f), RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("◷", color = ScreenPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${formatWatchHours(currentMonthMinutes)} this month",
                    modifier = Modifier.weight(1f),
                    color = ScreenTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    trendText,
                    color = ScreenTextSecondary,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    action: String,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 20.dp,
                top = 28.dp,
                end = 20.dp,
                bottom = 14.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = ScreenTextPrimary,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )

        if (action.isNotEmpty()) {
            Text(
                text = action,
                modifier = Modifier.clickable(
                    enabled = onActionClick != null,
                    onClick = { onActionClick?.invoke() }
                ),
                color = ScreenPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun UpNextCard(
    entry: UpNextEntry,
    isSaving: Boolean,
    onOpenShow: () -> Unit,
    onWatchedToday: () -> Unit,
    onChooseDate: () -> Unit
) {
    val item = entry.item
    val episode = entry.episode
    val totalEpisodes = item.totalEpisodes
        ?.coerceAtLeast(0)
        ?: 0

    Card(
        modifier = Modifier.width(310.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = ScreenSurface
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp)
        ) {
            LibraryPoster(
                item = item,
                modifier = Modifier
                    .width(88.dp)
                    .height(146.dp)
            )

            Spacer(
                modifier = Modifier.width(13.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(146.dp),
                verticalArrangement =
                    Arrangement.SpaceBetween
            ) {
                Column {
                    TextButton(
                        onClick = onOpenShow,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = item.title,
                            color = ScreenTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(2.dp)
                    )

                    Text(
                        text = buildString {
                            append(episode.episodeCode)
                            episode.runtime?.takeIf { it > 0 }?.let { runtime ->
                                append(" • ")
                                append(runtime)
                                append("m")
                            }
                        },
                        color = ScreenPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = episode.name,
                        color = ScreenTextSecondary,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Button(
                            onClick = onWatchedToday,
                            modifier = Modifier.weight(1f),
                            enabled = !isSaving,
                            contentPadding = PaddingValues(
                                horizontal = 8.dp,
                                vertical = 0.dp
                            ),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ScreenPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (isSaving) "Saving…" else "Watched today",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = onChooseDate,
                            enabled = !isSaving,
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ScreenSurfaceLight
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Date",
                                color = ScreenTextPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (totalEpisodes > 0) {
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = "$totalEpisodes episodes overall",
                            color = ScreenTextSecondary,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UpNextErrorCard(
    message: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = ScreenSurface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                color = ScreenTextSecondary,
                fontSize = 12.sp
            )

            TextButton(onClick = onRetry) {
                Text(
                    text = "Retry",
                    color = ScreenPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun UpNextCaughtUpCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = ScreenSuccess.copy(alpha = 0.10f)
        )
    ) {
        Text(
            text = "✓ You’re caught up with all currently aired episodes.",
            modifier = Modifier.padding(17.dp),
            color = ScreenSuccess,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun UpNextWaitingCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = ScreenSurface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "✓ You’re caught up",
                color = ScreenSuccess,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "The next announced episodes are listed below.",
                color = ScreenTextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun UpcomingEpisodeCard(
    entry: UpcomingEpisodeEntry,
    onOpenShow: () -> Unit
) {
    val airDate = entry.episode.parsedAirDate
    val daysUntil = airDate?.let { date ->
        java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), date)
    }
    val dateText = when (daysUntil) {
        0L -> "Airs today"
        1L -> "Airs tomorrow"
        null -> "Air date TBA"
        else -> "Airs ${airDate?.format(DateTimeFormatter.ofPattern("dd MMM")) ?: "TBA"} • in $daysUntil days"
    }

    Card(
        modifier = Modifier.width(280.dp),
        onClick = onOpenShow,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ScreenSurface)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            LibraryPoster(
                item = entry.item,
                modifier = Modifier.width(72.dp).height(108.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.height(108.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = entry.item.title,
                    color = ScreenTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = "${entry.episode.episodeCode} • ${entry.episode.name}",
                    color = ScreenPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(7.dp))
                Text(
                    text = dateText,
                    color = ScreenSuccess,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                if (entry.productionStatus.isNotBlank()) {
                    Text(
                        text = entry.productionStatus,
                        color = ScreenTextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

private fun showHomeDatePicker(
    context: android.content.Context,
    onDateSelected: (Long) -> Unit
) {
    val initialDate = LocalDate.now()

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
    ).apply {
        datePicker.maxDate = System.currentTimeMillis()
    }.show()
}

@Composable
private fun HistoryCard(
    entry: WatchHistoryEntry,
    onClick: () -> Unit
) {
    val item = entry.item

    Card(
        modifier = Modifier.width(164.dp),
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = ScreenSurface
        )
    ) {
        Column {
            LibraryPoster(
                item = item,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )

            Column(
                modifier = Modifier
                    .padding(14.dp)
                    .height(110.dp)
            ) {
                Text(
                    text = item.title,
                    modifier = Modifier.height(18.dp),
                    color = ScreenTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(
                    modifier = Modifier.height(5.dp)
                )

                Text(
                    text = if (entry.isRewatch) {
                        "↻ ${entry.detailText}"
                    } else {
                        entry.detailText
                    },
                    modifier = Modifier.height(28.dp),
                    color = ScreenPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(
                    modifier = Modifier.height(5.dp)
                )

                Text(
                    text = formatEpochDay(
                        entry.watchedDateEpochDay
                    ),
                    color = ScreenTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1
                )

                Spacer(
                    modifier = Modifier.height(5.dp)
                )

                Text(
                    text = item.personalRating
                        ?.let { "★ ${formatRating(it)}" }
                        ?: "Not rated",
                    color = if (item.personalRating != null) {
                        ScreenWarning
                    } else {
                        ScreenTextSecondary
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun EmptyHistoryCard(
    onSearchClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = ScreenSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Text(
                text = "✓",
                color = ScreenPrimary,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(
                text = "No watch history yet",
                color = ScreenTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = "Watched movies and TV episodes will appear here.",
                color = ScreenTextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Button(
                onClick = onSearchClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ScreenPrimary
                ),
                shape = RoundedCornerShape(13.dp)
            ) {
                Text(
                    text = "Find a title",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LibraryOverview(
    totalCount: Int,
    planToWatchCount: Int,
    watchingCount: Int,
    completedCount: Int,
    onSearchClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = ScreenSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Library breakdown",
                        color = ScreenTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$totalCount titles across your collection",
                        color = ScreenTextSecondary,
                        fontSize = 11.sp
                    )
                }
                Text(
                    text = totalCount.toString(),
                    color = ScreenPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(8.dp)),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (planToWatchCount > 0) {
                    Box(Modifier.weight(planToWatchCount.toFloat()).fillMaxSize().background(ScreenWarning))
                }
                if (watchingCount > 0) {
                    Box(Modifier.weight(watchingCount.toFloat()).fillMaxSize().background(ScreenPrimary))
                }
                if (completedCount > 0) {
                    Box(Modifier.weight(completedCount.toFloat()).fillMaxSize().background(ScreenSuccess))
                }
                if (totalCount == 0) {
                    Box(Modifier.fillMaxSize().background(ScreenSurfaceLight))
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LibraryStatusStat(Modifier.weight(1f), "＋", planToWatchCount, "Planned", ScreenWarning)
                LibraryStatusStat(Modifier.weight(1f), "▶", watchingCount, "Watching", ScreenPrimary)
                LibraryStatusStat(Modifier.weight(1f), "✓", completedCount, "Completed", ScreenSuccess)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onSearchClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ScreenPrimary),
                shape = RoundedCornerShape(13.dp)
            ) {
                Text("+ Add a title", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LibraryStatusStat(
    modifier: Modifier,
    symbol: String,
    count: Int,
    label: String,
    color: Color
) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(symbol, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(count.toString(), color = ScreenTextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Text(label, color = ScreenTextSecondary, fontSize = 9.sp, maxLines = 1)
    }
}

@Composable
fun LibraryScreen(
    paddingValues: PaddingValues,
    title: String,
    items: List<LibraryItem>,
    episodeWatches: List<EpisodeWatch>,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    onItemClick: (LibraryItem) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .padding(paddingValues),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 28.dp,
            end = 20.dp,
            bottom = 28.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Button(
                    onClick = onBackClick,
                    modifier = Modifier.size(44.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ScreenSurfaceLight
                    )
                ) {
                    Text("←", fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        color = ScreenTextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "${items.size} saved titles",
                        color = ScreenTextSecondary,
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = onAddClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ScreenPrimary
                    ),
                    shape = RoundedCornerShape(13.dp)
                ) {
                    Text(
                        text = "+ Add",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        when {
            isLoading -> {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LoadingBlock()
                }
            }

            items.isEmpty() -> {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyLibraryCard(
                        title = title,
                        onAddClick = onAddClick
                    )
                }
            }

            else -> {
                gridItems(
                    items = items,
                    key = { item ->
                        "${item.mediaType}-${item.tmdbId}"
                    }
                ) { item ->
                    LibraryGridCard(
                        item = item,
                        episodeWatches = episodeWatches.filter { watch ->
                            watch.tmdbShowId == item.tmdbId
                        },
                        onClick = {
                            onItemClick(item)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryGridCard(
    item: LibraryItem,
    episodeWatches: List<EpisodeWatch>,
    onClick: () -> Unit
) {
    val watchedEpisodeCount = episodeWatches.size
    val watchedEpisodeMinutes = episodeWatches.sumOf { watch ->
        watch.runtimeMinutes ?: 0
    }
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = ScreenSurface
        )
    ) {
        Column {
            LibraryPoster(
                item = item,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .padding(9.dp)
            ) {
                Text(
                    text = item.title,
                    modifier = Modifier.height(29.dp),
                    color = ScreenTextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (item.mediaType == "tv") {
                        "$watchedEpisodeCount ${if (watchedEpisodeCount == 1) "episode" else "episodes"}"
                    } else {
                        item.status.displayName
                    },
                    color = if (item.mediaType == "tv") {
                        ScreenPrimary
                    } else {
                        when (item.status) {
                            LibraryStatus.COMPLETED -> ScreenSuccess
                            LibraryStatus.WATCHING -> ScreenWarning
                            else -> ScreenPrimary
                        }
                    },
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = if (item.mediaType == "tv") {
                        val total = item.totalEpisodes?.takeIf { it > 0 }
                            ?: watchedEpisodeCount
                        "$watchedEpisodeCount/$total overall • ${formatWatchTime(watchedEpisodeMinutes)}"
                    } else {
                        item.watchDateEpochDay?.let { date ->
                            "${formatCompactEpochDay(date)} • ${item.displayYear}"
                        } ?: item.displayYear
                    },
                    color = ScreenTextSecondary,
                    fontSize = 8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(5.dp))
                if (item.mediaType != "tv") {
                    Text(
                        text = item.personalRating?.let { rating ->
                            "★ ${formatRating(rating)}"
                        } ?: "Not rated",
                        color = if (item.personalRating != null) ScreenWarning else ScreenTextSecondary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(
    status: LibraryStatus
) {
    val color = when (status) {
        LibraryStatus.PLAN_TO_WATCH ->
            ScreenPrimary

        LibraryStatus.WATCHING ->
            ScreenWarning

        LibraryStatus.COMPLETED ->
            ScreenSuccess

        LibraryStatus.DROPPED ->
            ScreenTextSecondary
    }

    Box(
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.14f),
                shape = RoundedCornerShape(7.dp)
            )
            .padding(
                horizontal = 8.dp,
                vertical = 4.dp
            )
    ) {
        Text(
            text = status.displayName,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LibraryPoster(
    item: LibraryItem,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ScreenSurfaceLight),
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
                color = ScreenPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EmptyLibraryCard(
    title: String,
    onAddClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 70.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = ScreenSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(30.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Text(
                text = "＋",
                color = ScreenPrimary,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Text(
                text = "No $title saved",
                color = ScreenTextPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Search TMDB and add your first title.",
                color = ScreenTextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ScreenPrimary
                ),
                shape = RoundedCornerShape(13.dp)
            ) {
                Text(
                    text = "Add title",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StatisticsScreen(
    paddingValues: PaddingValues,
    libraryUiState: LibraryUiState,
    onBackClick: () -> Unit
) {
    val availableYears = remember(libraryUiState.watchHistoryEntries) {
        libraryUiState.watchHistoryEntries
            .map { entry ->
                LocalDate.ofEpochDay(entry.watchedDateEpochDay).year
            }
            .distinct()
            .sorted()
            .ifEmpty { listOf(LocalDate.now().year) }
    }
    var selectedYear by remember(availableYears) {
        mutableIntStateOf(availableYears.last())
    }
    val yearSummary = remember(
        libraryUiState.watchHistoryEntries,
        selectedYear
    ) {
        calculateAdvancedYearSummary(
            entries = libraryUiState.watchHistoryEntries,
            year = selectedYear
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .padding(paddingValues),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 30.dp,
            end = 20.dp,
            bottom = 28.dp
        ),
        verticalArrangement =
            Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onBackClick,
                    modifier = Modifier.size(44.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ScreenSurfaceLight)
                ) { Text("←", fontSize = 20.sp) }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Statistics",
                        color = ScreenTextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Your viewing activity at a glance",
                        color = ScreenTextSecondary,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(14.dp)
            )
        }

        item { WatchTimeHeroCard(libraryUiState) }

        item {
            StatisticsYearSelector(
                years = availableYears,
                selectedYear = selectedYear,
                onYearSelected = { year -> selectedYear = year }
            )
        }

        item { AnnualSummaryCard(yearSummary) }
        item { ViewingPatternsCard(yearSummary) }
        item { TopGenresCard(yearSummary.topGenres) }
        item { RewatchInsightsCard(yearSummary) }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CompactStatisticCard(
                    modifier = Modifier.weight(1f),
                    value = libraryUiState.watchedMovieCount.toString(),
                    label = "Movies watched",
                    symbol = "▶"
                )
                CompactStatisticCard(
                    modifier = Modifier.weight(1f),
                    value = libraryUiState.watchedEpisodeCount.toString(),
                    label = "Episodes watched",
                    symbol = "▣"
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CompactStatisticCard(
                    modifier = Modifier.weight(1f),
                    value = libraryUiState.thisMonthCount.toString(),
                    label = "${LocalDate.now().format(DateTimeFormatter.ofPattern("MMM"))} watches",
                    symbol = "◷"
                )
                CompactStatisticCard(
                    modifier = Modifier.weight(1f),
                    value = libraryUiState.averagePersonalRating
                        ?.let { formatRating(it) }
                        ?: "—",
                    label = "Average rating",
                    symbol = "★"
                )
            }
        }

        if (libraryUiState.rewatchCount > 0) {
            item {
                CompactStatisticCard(
                    modifier = Modifier.fillMaxWidth(),
                    value = libraryUiState.rewatchCount.toString(),
                    label = "Rewatches logged",
                    symbol = "↻"
                )
            }
        }

        item { LongestStreakCard(libraryUiState.longestWatchStreak) }
        item { MonthlyWatchTimeCard(libraryUiState.monthlyWatchTimeTrend) }

        item { WatchTimeSplitCard(libraryUiState) }
        item { StatusDistributionCard(libraryUiState) }
    }
}

private data class AdvancedYearSummary(
    val year: Int,
    val totalEntries: Int,
    val movieWatches: Int,
    val episodeWatches: Int,
    val totalMinutes: Int,
    val activeDays: Int,
    val averageMinutesPerActiveDay: Int,
    val favoriteDay: String,
    val busiestMonth: String,
    val rewatchCount: Int,
    val rewatchRate: Int,
    val mostRewatchedTitle: String?,
    val topGenres: List<Pair<String, Int>>
)

private fun calculateAdvancedYearSummary(
    entries: List<WatchHistoryEntry>,
    year: Int
): AdvancedYearSummary {
    val yearEntries = entries.filter { entry ->
        LocalDate.ofEpochDay(entry.watchedDateEpochDay).year == year
    }
    val activeDays = yearEntries
        .map { entry -> entry.watchedDateEpochDay }
        .distinct()
        .size
    val totalMinutes = yearEntries.sumOf { entry ->
        entry.runtimeMinutes ?: 0
    }
    val favoriteDay = yearEntries
        .groupingBy { entry ->
            LocalDate.ofEpochDay(entry.watchedDateEpochDay).dayOfWeek
        }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key
        ?.name
        ?.lowercase()
        ?.replaceFirstChar { it.uppercase() }
        ?: "—"
    val busiestMonth = yearEntries
        .groupingBy { entry ->
            YearMonth.from(LocalDate.ofEpochDay(entry.watchedDateEpochDay))
        }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key
        ?.format(DateTimeFormatter.ofPattern("MMMM"))
        ?: "—"
    val rewatches = yearEntries.filter { entry -> entry.isRewatch }
    val mostRewatchedTitle = rewatches
        .groupingBy { entry -> entry.item.title }
        .eachCount()
        .maxByOrNull { it.value }
        ?.let { (title, count) -> "$title • $count" }
    val topGenres = yearEntries
        .distinctBy { entry ->
            entry.item.mediaType to entry.item.tmdbId
        }
        .flatMap { entry -> entry.item.genres.distinct() }
        .groupingBy { genre -> genre }
        .eachCount()
        .toList()
        .sortedWith(
            compareByDescending<Pair<String, Int>> { pair -> pair.second }
                .thenBy { pair -> pair.first }
        )
        .take(5)

    return AdvancedYearSummary(
        year = year,
        totalEntries = yearEntries.size,
        movieWatches = yearEntries.count { it.item.mediaType == "movie" },
        episodeWatches = yearEntries.count { it.item.mediaType == "tv" },
        totalMinutes = totalMinutes,
        activeDays = activeDays,
        averageMinutesPerActiveDay = if (activeDays > 0) {
            totalMinutes / activeDays
        } else {
            0
        },
        favoriteDay = favoriteDay,
        busiestMonth = busiestMonth,
        rewatchCount = rewatches.size,
        rewatchRate = if (yearEntries.isNotEmpty()) {
            (rewatches.size * 100f / yearEntries.size).toInt()
        } else {
            0
        },
        mostRewatchedTitle = mostRewatchedTitle,
        topGenres = topGenres
    )
}

@Composable
private fun StatisticsYearSelector(
    years: List<Int>,
    selectedYear: Int,
    onYearSelected: (Int) -> Unit
) {
    val index = years.indexOf(selectedYear).coerceAtLeast(0)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ScreenSurface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { onYearSelected(years[index - 1]) },
                enabled = index > 0,
                modifier = Modifier.size(42.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ScreenSurfaceLight)
            ) { Text("‹", fontSize = 22.sp) }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = selectedYear.toString(),
                    color = ScreenTextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Year in review",
                    color = ScreenTextSecondary,
                    fontSize = 10.sp
                )
            }
            Button(
                onClick = { onYearSelected(years[index + 1]) },
                enabled = index < years.lastIndex,
                modifier = Modifier.size(42.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ScreenSurfaceLight)
            ) { Text("›", fontSize = 22.sp) }
        }
    }
}

@Composable
private fun AnnualSummaryCard(summary: AdvancedYearSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ScreenSurface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "${summary.year} summary",
                color = ScreenTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnnualMetric(
                    modifier = Modifier.weight(1f),
                    value = summary.movieWatches.toString(),
                    label = "Movies"
                )
                AnnualMetric(
                    modifier = Modifier.weight(1f),
                    value = summary.episodeWatches.toString(),
                    label = "Episodes"
                )
                AnnualMetric(
                    modifier = Modifier.weight(1f),
                    value = formatWatchTime(summary.totalMinutes),
                    label = "Watch time"
                )
            }
        }
    }
}

@Composable
private fun AnnualMetric(
    modifier: Modifier,
    value: String,
    label: String
) {
    Column(
        modifier = modifier
            .background(ScreenSurfaceLight, RoundedCornerShape(13.dp))
            .padding(horizontal = 6.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = ScreenTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(text = label, color = ScreenTextSecondary, fontSize = 9.sp)
    }
}

@Composable
private fun ViewingPatternsCard(summary: AdvancedYearSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ScreenSurface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Viewing patterns",
                color = ScreenTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            StatisticsInformationRow("Favorite viewing day", summary.favoriteDay)
            StatisticsInformationRow("Busiest month", summary.busiestMonth)
            StatisticsInformationRow("Active watch days", summary.activeDays.toString())
            StatisticsInformationRow(
                "Average per active day",
                formatWatchTime(summary.averageMinutesPerActiveDay)
            )
        }
    }
}

@Composable
private fun StatisticsInformationRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = ScreenTextSecondary,
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = ScreenTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TopGenresCard(genres: List<Pair<String, Int>>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ScreenSurface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Top genres",
                color = ScreenTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Ranked by unique watched titles in the selected year",
                color = ScreenTextSecondary,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            if (genres.isEmpty()) {
                Text(
                    text = "Genre information is being updated from TMDB.",
                    color = ScreenTextSecondary,
                    fontSize = 12.sp
                )
            } else {
                val maximum = genres.maxOf { pair -> pair.second }.coerceAtLeast(1)
                genres.forEachIndexed { index, (genre, count) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${index + 1}",
                            modifier = Modifier.width(24.dp),
                            color = ScreenPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row {
                                Text(
                                    text = genre,
                                    modifier = Modifier.weight(1f),
                                    color = ScreenTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = count.toString(),
                                    color = ScreenTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            LinearProgressIndicator(
                                progress = {
                                    count.toFloat() / maximum.toFloat()
                                },
                                modifier = Modifier.fillMaxWidth().height(5.dp),
                                color = ScreenPrimary,
                                trackColor = ScreenSurfaceLight
                            )
                        }
                    }
                    if (index < genres.lastIndex) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RewatchInsightsCard(summary: AdvancedYearSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ScreenSurface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Rewatch insights",
                color = ScreenTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            StatisticsInformationRow("Rewatches", summary.rewatchCount.toString())
            StatisticsInformationRow("Share of watch history", "${summary.rewatchRate}%")
            StatisticsInformationRow(
                "Most rewatched",
                summary.mostRewatchedTitle ?: "—"
            )
        }
    }
}

@Composable
private fun LongestStreakCard(days: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ScreenSurface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(54.dp).background(
                    ScreenPrimary.copy(alpha = 0.14f),
                    RoundedCornerShape(16.dp)
                ),
                contentAlignment = Alignment.Center
            ) {
                Text("🔥", fontSize = 25.sp)
            }
            Spacer(modifier = Modifier.width(15.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$days ${if (days == 1) "day" else "days"}",
                    color = ScreenTextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Longest watching streak",
                    color = ScreenTextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun MonthlyWatchTimeCard(months: List<MonthlyWatchTime>) {
    val maximumMinutes = months.maxOfOrNull { it.minutes }
        ?.coerceAtLeast(1)
        ?: 1

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ScreenSurface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Watching hours by month",
                color = ScreenTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Last 6 months",
                color = ScreenTextSecondary,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                months.forEach { month ->
                    val barHeight = if (month.minutes == 0) {
                        4.dp
                    } else {
                        (month.minutes.toFloat() / maximumMinutes * 92f)
                            .coerceAtLeast(10f)
                            .dp
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = formatHoursForChart(month.minutes),
                            color = ScreenTextSecondary,
                            fontSize = 9.sp,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Box(
                            modifier = Modifier.height(96.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(25.dp)
                                    .height(barHeight)
                                    .background(
                                        if (month.minutes > 0) ScreenPrimary else ScreenSurfaceLight,
                                        RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp)
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = month.label,
                            color = ScreenTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

private fun formatHoursForChart(minutes: Int): String {
    if (minutes <= 0) return "0h"
    val hours = minutes / 60f
    return if (hours >= 10f || hours % 1f == 0f) {
        "${hours.toInt()}h"
    } else {
        String.format("%.1fh", hours)
    }
}

@Composable
private fun WatchTimeHeroCard(state: LibraryUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ScreenPrimary)
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Text(
                text = "TOTAL WATCH TIME",
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatWatchTime(state.totalWatchMinutes),
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = "From watched movies and individual TV episodes",
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun CompactStatisticCard(
    modifier: Modifier,
    value: String,
    label: String,
    symbol: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = ScreenSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        color = ScreenPrimary.copy(
                            alpha = 0.14f
                        ),
                        shape = RoundedCornerShape(15.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = symbol,
                    color = ScreenPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = value,
                color = ScreenTextPrimary,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                color = ScreenTextSecondary,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun WatchTimeSplitCard(state: LibraryUiState) {
    val total = state.totalWatchMinutes.coerceAtLeast(1)
    val movieShare = state.movieWatchMinutes.toFloat() / total

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ScreenSurface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Watch-time breakdown", color = ScreenTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { movieShare },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)),
                color = ScreenPrimary,
                trackColor = ScreenSuccess
            )
            Spacer(modifier = Modifier.height(14.dp))
            TimeLegendRow("Movies", state.movieWatchMinutes, ScreenPrimary)
            Spacer(modifier = Modifier.height(10.dp))
            TimeLegendRow("TV episodes", state.tvWatchMinutes, ScreenSuccess)
        }
    }
}

@Composable
private fun TimeLegendRow(label: String, minutes: Int, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(9.dp))
        Text(label, modifier = Modifier.weight(1f), color = ScreenTextSecondary, fontSize = 13.sp)
        Text(formatWatchTime(minutes), color = ScreenTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatusDistributionCard(state: LibraryUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ScreenSurface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Library status", color = ScreenTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(14.dp))
            StatusCountRow("Plan to watch", state.planToWatchCount, ScreenWarning)
            StatusCountRow("Watching", state.watching.size, ScreenPrimary)
            StatusCountRow("Completed", state.completedCount, ScreenSuccess)
        }
    }
}

@Composable
private fun StatusCountRow(label: String, count: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(9.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(10.dp))
        Text(label, modifier = Modifier.weight(1f), color = ScreenTextSecondary, fontSize = 13.sp)
        Text(count.toString(), color = ScreenTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

private fun formatWatchTime(totalMinutes: Int): String {
    if (totalMinutes <= 0) return "0m"
    val days = totalMinutes / (24 * 60)
    val hours = (totalMinutes % (24 * 60)) / 60
    val minutes = totalMinutes % 60
    return buildList {
        if (days > 0) add("${days}d")
        if (hours > 0) add("${hours}h")
        if (minutes > 0 || isEmpty()) add("${minutes}m")
    }.joinToString(" ")
}

private fun formatWatchHours(totalMinutes: Int): String {
    if (totalMinutes <= 0) return "0m"
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}

@Composable
private fun LoadingBlock() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = ScreenPrimary
        )
    }
}

private fun formatEpochDay(
    epochDay: Long
): String {
    return LocalDate
        .ofEpochDay(epochDay)
        .format(
            DateTimeFormatter.ofPattern(
                "dd MMM yyyy"
            )
        )
}

private fun formatCompactEpochDay(epochDay: Long): String {
    return LocalDate
        .ofEpochDay(epochDay)
        .format(DateTimeFormatter.ofPattern("dd MMM yy"))
}

private fun formatRating(
    rating: Double
): String {
    return if (rating % 1.0 == 0.0) {
        rating.toInt().toString()
    } else {
        "%.1f".format(rating)
    }
}

