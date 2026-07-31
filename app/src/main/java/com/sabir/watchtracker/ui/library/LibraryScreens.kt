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
    onSearchClick: () -> Unit
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
                planToWatchCount =
                    libraryUiState.planToWatchCount,
                movieCount = libraryUiState.movieCount,
                completedCount =
                    libraryUiState.completedCount
            )
        }

        item {
            SectionHeader(
                title = "Watch history",
                action = if (
                    libraryUiState.watchHistory.isNotEmpty()
                ) {
                    "${libraryUiState.watchHistory.size} completed"
                } else {
                    ""
                }
            )
        }

        if (libraryUiState.isLoading) {
            item {
                LoadingBlock()
            }
        } else if (libraryUiState.watchHistory.isEmpty()) {
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
                            .watchHistory
                            .take(10),
                        key = { item ->
                            "${item.mediaType}-${item.tmdbId}"
                        }
                    ) { item ->
                        HistoryCard(item)
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
                planToWatchCount =
                    libraryUiState.planToWatchCount,
                completedCount =
                    libraryUiState.completedCount,
                droppedCount =
                    libraryUiState.droppedCount,
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
    planToWatchCount: Int,
    movieCount: Int,
    completedCount: Int
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
            value = planToWatchCount.toString(),
            label = "Planned"
        )

        SummaryCard(
            modifier = Modifier.weight(1f),
            value = movieCount.toString(),
            label = "Movies"
        )

        SummaryCard(
            modifier = Modifier.weight(1f),
            value = completedCount.toString(),
            label = "Completed"
        )
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier,
    value: String,
    label: String
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
                color = ScreenTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
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
private fun HistoryCard(
    item: LibraryItem
) {
    Card(
        modifier = Modifier.width(164.dp),
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
                modifier = Modifier.padding(14.dp)
            ) {
                Text(
                    text = item.title,
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
                    text = item.watchDateEpochDay
                        ?.let(::formatEpochDay)
                        ?: "Date not recorded",
                    color = ScreenTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1
                )

                if (item.personalRating != null) {
                    Spacer(
                        modifier = Modifier.height(5.dp)
                    )

                    Text(
                        text = "★ ${formatRating(item.personalRating)}",
                        color = ScreenWarning,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
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
                text = "No completed titles yet",
                color = ScreenTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = "Completed movies and shows will appear here.",
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
    planToWatchCount: Int,
    completedCount: Int,
    droppedCount: Int,
    onSearchClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(
            horizontal = 20.dp
        ),
        verticalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {
        LibraryCountRow(
            symbol = "＋",
            title = "Plan to Watch",
            subtitle = "Titles saved for later",
            count = planToWatchCount,
            onClick = onSearchClick
        )

        LibraryCountRow(
            symbol = "✓",
            title = "Completed",
            subtitle = "Your watch history",
            count = completedCount,
            onClick = {}
        )

        LibraryCountRow(
            symbol = "×",
            title = "Dropped",
            subtitle = "Titles you stopped watching",
            count = droppedCount,
            onClick = {}
        )
    }
}

@Composable
private fun LibraryCountRow(
    symbol: String,
    title: String,
    subtitle: String,
    count: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = ScreenSurface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = ScreenPrimary.copy(
                            alpha = 0.14f
                        ),
                        shape = RoundedCornerShape(13.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = symbol,
                    color = ScreenPrimary,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.width(14.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = ScreenTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = subtitle,
                    color = ScreenTextSecondary,
                    fontSize = 12.sp
                )
            }

            Text(
                text = count.toString(),
                color = ScreenTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun LibraryScreen(
    paddingValues: PaddingValues,
    title: String,
    items: List<LibraryItem>,
    isLoading: Boolean,
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
    libraryUiState: LibraryUiState
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

            Spacer(
                modifier = Modifier.height(14.dp)
            )
        }

        item {
            StatisticCard(
                value = libraryUiState
                    .totalCount
                    .toString(),
                label = "Total titles",
                symbol = "▦"
            )
        }

        item {
            StatisticCard(
                value = libraryUiState
                    .completedCount
                    .toString(),
                label = "Completed",
                symbol = "✓"
            )
        }

        item {
            StatisticCard(
                value = libraryUiState
                    .planToWatchCount
                    .toString(),
                label = "Plan to Watch",
                symbol = "＋"
            )
        }

        item {
            StatisticCard(
                value = libraryUiState
                    .movieCount
                    .toString(),
                label = "Movies",
                symbol = "▶"
            )
        }

        item {
            StatisticCard(
                value = libraryUiState
                    .tvShowCount
                    .toString(),
                label = "TV Shows",
                symbol = "▣"
            )
        }

        item {
            StatisticCard(
                value = libraryUiState
                    .droppedCount
                    .toString(),
                label = "Dropped",
                symbol = "×"
            )
        }
    }
}

@Composable
private fun StatisticCard(
    value: String,
    label: String,
    symbol: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = ScreenSurface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
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

            Spacer(
                modifier = Modifier.width(16.dp)
            )

            Column {
                Text(
                    text = value,
                    color = ScreenTextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = label,
                    color = ScreenTextSecondary,
                    fontSize = 13.sp
                )
            }
        }
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

