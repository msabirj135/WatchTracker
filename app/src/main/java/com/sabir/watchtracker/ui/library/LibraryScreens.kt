package com.sabir.watchtracker.ui.library

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.sabir.watchtracker.data.local.LibraryItem
import com.sabir.watchtracker.data.local.LibraryStatus
import java.time.LocalDate
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
    onSearchClick: () -> Unit,
    onItemClick: (LibraryItem) -> Unit
) {
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
                onSearchClick = onSearchClick
            )
        }

        item {
            HomeSummary(
                movieCount = libraryUiState.movieCount,
                tvShowCount = libraryUiState.tvShowCount,
                totalWatchMinutes = libraryUiState.totalWatchMinutes
            )
        }

        if (libraryUiState.continueWatching.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Continue watching",
                    action = "${libraryUiState.continueWatching.size} shows"
                )
            }

            item {
                LazyRow(
                    horizontalArrangement =
                        Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(
                        horizontal = 20.dp
                    )
                ) {
                    items(
                        items = libraryUiState.continueWatching,
                        key = { item ->
                            "continue-${item.tmdbId}"
                        }
                    ) { item ->
                        ContinueWatchingCard(
                            item = item,
                            watchedEpisodeCount =
                                libraryUiState
                                    .watchedEpisodeCount(
                                        item.tmdbId
                                    ),
                            onClick = {
                                onItemClick(item)
                            }
                        )
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "Watch history",
                action = ""
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
    onSearchClick: () -> Unit
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
                text = "WATCHTRACKER",
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
    totalWatchMinutes: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {
        SummaryCard(
            modifier = Modifier.weight(1f),
            value = movieCount.toString(),
            label = "Movies"
        )

        SummaryCard(
            modifier = Modifier.weight(1f),
            value = tvShowCount.toString(),
            label = "TV Shows"
        )

        SummaryCard(
            modifier = Modifier.weight(1f),
            value = formatHomeWatchTime(totalWatchMinutes),
            label = "Watch time",
            valueFontSize = 18
        )
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier,
    value: String,
    label: String,
    valueFontSize: Int = 24
) {
    Card(
        modifier = modifier,
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
                fontSize = valueFontSize.sp,
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
private fun SectionHeader(
    title: String,
    action: String
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
                color = ScreenPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ContinueWatchingCard(
    item: LibraryItem,
    watchedEpisodeCount: Int,
    onClick: () -> Unit
) {
    val totalEpisodes = item.totalEpisodes
        ?.coerceAtLeast(0)
        ?: 0

    val progress = if (totalEpisodes > 0) {
        watchedEpisodeCount
            .toFloat()
            .div(totalEpisodes.toFloat())
            .coerceIn(0f, 1f)
    } else {
        0f
    }

    Card(
        modifier = Modifier.width(270.dp),
        onClick = onClick,
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
                    .width(82.dp)
                    .height(124.dp)
            )

            Spacer(
                modifier = Modifier.width(13.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(124.dp),
                verticalArrangement =
                    Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = item.title,
                        color = ScreenTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    Text(
                        text = item.episodeProgressText
                            ?: "Not started",
                        color = ScreenPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (totalEpisodes > 0) {
                                "$watchedEpisodeCount / $totalEpisodes"
                            } else {
                                "$watchedEpisodeCount watched"
                            },
                            modifier = Modifier.weight(1f),
                            color = ScreenTextSecondary,
                            fontSize = 11.sp
                        )

                        if (totalEpisodes > 0) {
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                color = ScreenPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = ScreenPrimary,
                        trackColor = ScreenSurfaceLight
                    )

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    Text(
                        text = item.watchDateEpochDay
                            ?.let { epochDay ->
                                "Last watched ${formatEpochDay(epochDay)}"
                            }
                            ?: "Open to continue",
                        color = ScreenTextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
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
                    text = entry.detailText,
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
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    onItemClick: (LibraryItem) -> Unit
) {
    LazyColumn(
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
        verticalArrangement =
            Arrangement.spacedBy(14.dp)
    ) {
        item {
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
                item {
                    LoadingBlock()
                }
            }

            items.isEmpty() -> {
                item {
                    EmptyLibraryCard(
                        title = title,
                        onAddClick = onAddClick
                    )
                }
            }

            else -> {
                items(
                    items = items,
                    key = { item ->
                        "${item.mediaType}-${item.tmdbId}"
                    }
                ) { item ->
                    LibraryItemCard(
                        item = item,
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
private fun LibraryItemCard(
    item: LibraryItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
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
                    .width(92.dp)
                    .height(156.dp)
            )

            Spacer(
                modifier = Modifier.width(14.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(156.dp)
            ) {
                Text(
                    text = item.title,
                    color = ScreenTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(
                    modifier = Modifier.height(7.dp)
                )

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    StatusBadge(item.status)

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    Text(
                        text = item.displayYear,
                        color = ScreenTextSecondary,
                        fontSize = 12.sp
                    )
                }

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                if (item.watchDateEpochDay != null) {
                    Text(
                        text = if (item.mediaType == "tv") {
                            "Last watched: ${
                                formatEpochDay(
                                    item.watchDateEpochDay
                                )
                            }"
                        } else {
                            "Watched: ${
                            formatEpochDay(
                                item.watchDateEpochDay
                            )
                        }"
                        },
                        color = ScreenTextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(
                        modifier = Modifier.height(5.dp)
                    )
                }

                if (item.personalRating != null) {
                    Text(
                        text = "Your rating: ★ ${
                            formatRating(
                                item.personalRating
                            )
                        }",
                        color = ScreenWarning,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (item.mediaType == "tv") {
                    val progressText = buildString {
                        append(
                            item.episodeProgressText
                                ?: "Not started"
                        )

                        if (
                            item.totalEpisodes != null &&
                            item.totalEpisodes > 0
                        ) {
                            append(" • ")
                            append(item.totalEpisodes)
                            append(" episodes")
                        }
                    }

                    Text(
                        text = progressText,
                        color = ScreenPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
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

        item { LongestStreakCard(libraryUiState.longestWatchStreak) }
        item { MonthlyWatchTimeCard(libraryUiState.monthlyWatchTimeTrend) }

        item { WatchTimeSplitCard(libraryUiState) }
        item { StatusDistributionCard(libraryUiState) }
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

private fun formatHomeWatchTime(totalMinutes: Int): String {
    if (totalMinutes <= 0) return "0m"
    val days = totalMinutes / (24 * 60)
    val hours = (totalMinutes % (24 * 60)) / 60
    val minutes = totalMinutes % 60

    return when {
        days > 0 && minutes > 0 -> "${days}d ${hours}h\n${minutes}m"
        days > 0 -> "${days}d ${hours}h"
        hours > 0 && minutes > 0 -> "${hours}h\n${minutes}m"
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

private fun formatRating(
    rating: Double
): String {
    return if (rating % 1.0 == 0.0) {
        rating.toInt().toString()
    } else {
        "%.1f".format(rating)
    }
}

