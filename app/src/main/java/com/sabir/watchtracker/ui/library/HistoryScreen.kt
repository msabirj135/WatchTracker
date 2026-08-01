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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.sabir.watchtracker.data.local.LibraryItem
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val HistoryBackground = Color(0xFF090B10)
private val HistorySurface = Color(0xFF12151D)
private val HistorySurfaceLight = Color(0xFF1A1E28)
private val HistoryPrimary = Color(0xFFE63946)
private val HistoryTextPrimary = Color(0xFFF5F5F7)
private val HistoryTextSecondary = Color(0xFF9A9DA8)
private val HistorySuccess = Color(0xFF36C98F)

private enum class HistoryFilter(val label: String) {
    ALL("All"),
    MOVIES("Movies"),
    TV_SHOWS("TV Shows")
}

@Composable
fun HistoryScreen(
    state: LibraryUiState,
    onBackClick: () -> Unit,
    onItemClick: (LibraryItem) -> Unit
) {
    val allEntries = state.watchHistoryEntries
    val initialMonth = remember(allEntries) {
        allEntries.firstOrNull()
            ?.let { YearMonth.from(LocalDate.ofEpochDay(it.watchedDateEpochDay)) }
            ?: YearMonth.now()
    }

    var displayedMonth by remember { mutableStateOf(initialMonth) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedFilter by remember { mutableStateOf(HistoryFilter.ALL) }

    val filteredEntries = remember(
        allEntries,
        displayedMonth,
        selectedDate,
        selectedFilter
    ) {
        allEntries.filter { entry ->
            val date = LocalDate.ofEpochDay(entry.watchedDateEpochDay)
            val matchesMonth = YearMonth.from(date) == displayedMonth
            val matchesDate = selectedDate == null || date == selectedDate
            val matchesType = when (selectedFilter) {
                HistoryFilter.ALL -> true
                HistoryFilter.MOVIES -> entry.item.mediaType == "movie"
                HistoryFilter.TV_SHOWS -> entry.item.mediaType == "tv"
            }
            matchesMonth && matchesDate && matchesType
        }
    }

    val monthEntries = remember(allEntries, displayedMonth, selectedFilter) {
        allEntries.filter { entry ->
            val date = LocalDate.ofEpochDay(entry.watchedDateEpochDay)
            YearMonth.from(date) == displayedMonth && when (selectedFilter) {
                HistoryFilter.ALL -> true
                HistoryFilter.MOVIES -> entry.item.mediaType == "movie"
                HistoryFilter.TV_SHOWS -> entry.item.mediaType == "tv"
            }
        }
    }

    val activityByDate = remember(monthEntries) {
        monthEntries.groupingBy { entry ->
            LocalDate.ofEpochDay(entry.watchedDateEpochDay)
        }.eachCount()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(HistoryBackground),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 38.dp,
            end = 20.dp,
            bottom = 36.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HistoryHeader(onBackClick = onBackClick)
        }

        item {
            HistoryFilters(
                selected = selectedFilter,
                onSelected = {
                    selectedFilter = it
                    selectedDate = null
                }
            )
        }

        item {
            HistoryCalendar(
                month = displayedMonth,
                activityByDate = activityByDate,
                selectedDate = selectedDate,
                onPreviousMonth = {
                    displayedMonth = displayedMonth.minusMonths(1)
                    selectedDate = null
                },
                onNextMonth = {
                    displayedMonth = displayedMonth.plusMonths(1)
                    selectedDate = null
                },
                onDateClick = { date ->
                    selectedDate = if (selectedDate == date) null else date
                }
            )
        }

        item {
            HistoryResultsHeader(
                month = displayedMonth,
                selectedDate = selectedDate,
                count = filteredEntries.size,
                onClearDate = { selectedDate = null }
            )
        }

        if (filteredEntries.isEmpty()) {
            item {
                EmptyMonthCard(
                    selectedDate = selectedDate,
                    filter = selectedFilter
                )
            }
        } else {
            items(
                items = filteredEntries,
                key = { entry -> entry.key }
            ) { entry ->
                HistoryListItem(
                    entry = entry,
                    onClick = { onItemClick(entry.item) }
                )
            }
        }
    }
}

@Composable
private fun HistoryHeader(onBackClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(
            onClick = onBackClick,
            modifier = Modifier.size(46.dp),
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(13.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = HistorySurfaceLight
            )
        ) {
            Text("←", fontSize = 21.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                "Watch history",
                color = HistoryTextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Browse everything you watched by date",
                color = HistoryTextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun HistoryFilters(
    selected: HistoryFilter,
    onSelected: (HistoryFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HistoryFilter.entries.forEach { filter ->
            Button(
                onClick = { onSelected(filter) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected == filter) {
                        HistoryPrimary
                    } else {
                        HistorySurface
                    }
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 5.dp)
            ) {
                Text(
                    filter.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun HistoryCalendar(
    month: YearMonth,
    activityByDate: Map<LocalDate, Int>,
    selectedDate: LocalDate?,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateClick: (LocalDate) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = HistorySurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CalendarArrow("‹", onPreviousMonth)
                Text(
                    month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    modifier = Modifier.weight(1f),
                    color = HistoryTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                CalendarArrow("›", onNextMonth)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                    Text(
                        day,
                        modifier = Modifier.weight(1f),
                        color = HistoryTextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val firstOffset = month.atDay(1).dayOfWeek.value - DayOfWeek.MONDAY.value
            val cells = List(firstOffset) { null } +
                (1..month.lengthOfMonth()).map(month::atDay)

            cells.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    (week + List(7 - week.size) { null }).forEach { date ->
                        CalendarDay(
                            modifier = Modifier.weight(1f),
                            date = date,
                            activityCount = date?.let(activityByDate::get) ?: 0,
                            selected = date == selectedDate,
                            onClick = onDateClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarArrow(symbol: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(36.dp),
        onClick = onClick,
        color = HistorySurfaceLight,
        shape = CircleShape
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(symbol, color = HistoryTextPrimary, fontSize = 22.sp)
        }
    }
}

@Composable
private fun CalendarDay(
    modifier: Modifier,
    date: LocalDate?,
    activityCount: Int,
    selected: Boolean,
    onClick: (LocalDate) -> Unit
) {
    Box(
        modifier = modifier.height(43.dp),
        contentAlignment = Alignment.Center
    ) {
        if (date != null) {
            Surface(
                modifier = Modifier.size(38.dp),
                onClick = { onClick(date) },
                color = when {
                    selected -> HistoryPrimary
                    activityCount > 0 -> HistoryPrimary.copy(alpha = 0.16f)
                    else -> Color.Transparent
                },
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        date.dayOfMonth.toString(),
                        color = if (activityCount > 0 || selected) {
                            HistoryTextPrimary
                        } else {
                            HistoryTextSecondary
                        },
                        fontSize = 12.sp,
                        fontWeight = if (activityCount > 0) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        }
                    )
                    if (activityCount > 0 && !selected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 3.dp)
                                .size(4.dp)
                                .background(HistoryPrimary, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryResultsHeader(
    month: YearMonth,
    selectedDate: LocalDate?,
    count: Int,
    onClearDate: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                selectedDate?.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
                    ?: month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                color = HistoryTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "$count ${if (count == 1) "entry" else "entries"}",
                color = HistoryTextSecondary,
                fontSize = 11.sp
            )
        }
        if (selectedDate != null) {
            Button(
                onClick = onClearDate,
                colors = ButtonDefaults.buttonColors(
                    containerColor = HistorySurfaceLight
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Show month", fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun HistoryListItem(
    entry: WatchHistoryEntry,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = HistorySurface)
    ) {
        Row(modifier = Modifier.padding(11.dp)) {
            AsyncImage(
                model = entry.item.posterUrl,
                contentDescription = entry.item.title,
                modifier = Modifier
                    .width(64.dp)
                    .height(92.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(92.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    entry.item.title,
                    color = HistoryTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    entry.detailText,
                    color = HistoryPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    LocalDate.ofEpochDay(entry.watchedDateEpochDay)
                        .format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                    color = HistoryTextSecondary,
                    fontSize = 11.sp
                )
            }
            Text(
                "›",
                modifier = Modifier.align(Alignment.CenterVertically),
                color = HistoryTextSecondary,
                fontSize = 22.sp
            )
        }
    }
}

@Composable
private fun EmptyMonthCard(
    selectedDate: LocalDate?,
    filter: HistoryFilter
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = HistorySurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("◷", color = HistoryPrimary, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                if (selectedDate != null) {
                    "Nothing watched on this day"
                } else {
                    "No ${filter.label.lowercase()} watched this month"
                },
                color = HistoryTextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Choose another date, month or filter.",
                color = HistoryTextSecondary,
                fontSize = 11.sp
            )
        }
    }
}
