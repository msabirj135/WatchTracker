package com.sabir.watchtracker.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items as columnItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.sabir.watchtracker.data.local.CustomList
import com.sabir.watchtracker.data.local.LibraryItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val ListsBackground = Color(0xFF090B10)
private val ListsSurface = Color(0xFF12151D)
private val ListsSurfaceLight = Color(0xFF1A1E28)
private val ListsPrimary = Color(0xFFE63946)
private val ListsText = Color(0xFFF5F5F7)
private val ListsMuted = Color(0xFF9A9DA8)

private val listColors = listOf(
    "red" to ListsPrimary,
    "blue" to Color(0xFF4D8DFF),
    "green" to Color(0xFF36C98F),
    "amber" to Color(0xFFFFB84D),
    "purple" to Color(0xFFA879FF)
)

private val listIcons = listOf(
    "list" to "☷",
    "heart" to "♥",
    "star" to "★",
    "film" to "▶",
    "bookmark" to "◆"
)

private enum class CustomListSort(val label: String) {
    ADDED("Added"),
    TITLE("A–Z"),
    RECENT("Recently watched")
}

private fun listAccent(key: String?): Color =
    listColors.firstOrNull { it.first == key }?.second ?: ListsPrimary

private fun listIcon(key: String?): String =
    listIcons.firstOrNull { it.first == key }?.second ?: "☷"

@Composable
fun ListsScreen(
    paddingValues: PaddingValues,
    state: LibraryUiState,
    onBackClick: () -> Unit,
    onCreateList: (String, String, String, String) -> Unit,
    onUpdateList: (CustomList, String, String, String, String) -> Unit,
    onDuplicateList: (CustomList) -> Unit,
    onDeleteList: (Long) -> Unit,
    onAddItem: (Long, LibraryItem) -> Unit,
    onRemoveItem: (Long, LibraryItem) -> Unit,
    onItemClick: (LibraryItem) -> Unit
) {
    var selectedMonth by remember { mutableStateOf<MonthlyWatchList?>(null) }
    var theatreListIsOpen by remember { mutableStateOf(false) }
    var selectedMonthlyEntry by remember { mutableStateOf<MonthlyGridEntry?>(null) }
    var selectedList by remember { mutableStateOf<CustomList?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showAddTitles by remember { mutableStateOf(false) }
    var editingList by remember { mutableStateOf<CustomList?>(null) }

    BackHandler(
        enabled = selectedMonthlyEntry != null || selectedMonth != null || selectedList != null || theatreListIsOpen
    ) {
        when {
            selectedMonthlyEntry != null -> selectedMonthlyEntry = null
            selectedMonth != null -> selectedMonth = null
            selectedList != null -> selectedList = null
            theatreListIsOpen -> theatreListIsOpen = false
        }
    }

    when {
        theatreListIsOpen -> TheatreWatchesDetail(
            paddingValues = paddingValues,
            entries = state.theatreWatchEntries,
            moviesThisYear = state.theatreMoviesThisYear,
            totalMinutes = state.theatreWatchMinutes,
            onBack = { theatreListIsOpen = false },
            onItemClick = onItemClick
        )

        selectedMonthlyEntry != null -> MonthlyTvDetail(
            paddingValues = paddingValues,
            entry = selectedMonthlyEntry!!,
            onBack = { selectedMonthlyEntry = null },
            onOpenSeries = { onItemClick(selectedMonthlyEntry!!.item) }
        )

        selectedMonth != null -> MonthlyListDetail(
            paddingValues = paddingValues,
            month = selectedMonth!!,
            onBack = { selectedMonth = null },
            onEntryClick = { entry ->
                if (entry.item.mediaType == "tv") {
                    selectedMonthlyEntry = entry
                } else {
                    onItemClick(entry.item)
                }
            }
        )

        selectedList != null -> CustomListDetail(
            paddingValues = paddingValues,
            list = selectedList!!,
            items = state.itemsForList(selectedList!!.id),
            onBack = { selectedList = null },
            onAdd = { showAddTitles = true },
            onEdit = { editingList = selectedList },
            onDuplicate = {
                onDuplicateList(selectedList!!)
                selectedList = null
            },
            onDelete = {
                onDeleteList(selectedList!!.id)
                selectedList = null
            },
            onRemove = { onRemoveItem(selectedList!!.id, it) },
            onItemClick = onItemClick
        )

        else -> ListsOverview(
            paddingValues = paddingValues,
            state = state,
            onBackClick = onBackClick,
            onNewList = { showCreateDialog = true },
            onTheatreClick = { theatreListIsOpen = true },
            onMonthClick = { selectedMonth = it },
            onListClick = { selectedList = it }
        )
    }

    if (showCreateDialog) {
        ListEditorDialog(
            title = "Create list",
            initialName = "",
            initialDescription = "",
            initialColorKey = "red",
            initialIconKey = "list",
            onDismiss = { showCreateDialog = false },
            onSave = { name, description, colorKey, iconKey ->
                onCreateList(name, description, colorKey, iconKey)
                showCreateDialog = false
            }
        )
    }

    editingList?.let { list ->
        ListEditorDialog(
            title = "Edit list",
            initialName = list.name,
            initialDescription = list.description,
            initialColorKey = list.colorKey ?: "red",
            initialIconKey = list.iconKey ?: "list",
            onDismiss = { editingList = null },
            onSave = { name, description, colorKey, iconKey ->
                onUpdateList(list, name, description, colorKey, iconKey)
                selectedList = list.copy(
                    name = name.trim(),
                    description = description.trim(),
                    colorKey = colorKey,
                    iconKey = iconKey
                )
                editingList = null
            }
        )
    }

    if (showAddTitles && selectedList != null) {
        TitlePickerDialog(
            allItems = state.items,
            selectedItems = state.itemsForList(selectedList!!.id),
            onDismiss = { showAddTitles = false },
            onAdd = { onAddItem(selectedList!!.id, it) },
            onRemove = { onRemoveItem(selectedList!!.id, it) }
        )
    }
}

@Composable
private fun ListsOverview(
    paddingValues: PaddingValues,
    state: LibraryUiState,
    onBackClick: () -> Unit,
    onNewList: () -> Unit,
    onTheatreClick: () -> Unit,
    onMonthClick: (MonthlyWatchList) -> Unit,
    onListClick: (CustomList) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ListsBackground)
            .padding(paddingValues),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onBackClick,
                    modifier = Modifier.size(44.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ListsSurfaceLight),
                    shape = RoundedCornerShape(13.dp)
                ) { Text("←", fontSize = 20.sp) }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Lists", color = ListsText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("Monthly history and your collections", color = ListsMuted, fontSize = 13.sp)
                }
                Button(
                    onClick = onNewList,
                    colors = ButtonDefaults.buttonColors(containerColor = ListsPrimary)
                ) { Text("+ New") }
            }
        }

        item { SectionTitle("Smart lists", "Automatic collections and monthly history") }

        item {
            TheatreSmartListCard(
                movieCount = state.theatreWatchEntries.size,
                moviesThisYear = state.theatreMoviesThisYear,
                totalMinutes = state.theatreWatchMinutes,
                onClick = onTheatreClick
            )
        }

        if (state.monthlyLists.isEmpty()) {
            item { EmptyListCard("Watch a movie or episode to create your first monthly list.") }
        } else {
            state.monthlyLists.groupBy { it.year }.forEach { (year, months) ->
                item {
                    Text(year.toString(), color = ListsPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                columnItems(months, key = { "month-${it.year}-${it.month}" }) { month ->
                    MonthCard(month, onMonthClick)
                }
            }
        }

        item { SectionTitle("My lists", "${state.customLists.size} custom lists") }

        if (state.customLists.isEmpty()) {
            item { EmptyListCard("Create a list for favourites, recommendations or anything else.") }
        } else {
            columnItems(state.customLists, key = { it.id }) { list ->
                val count = state.customListItems.count { it.listId == list.id }
                Card(
                    onClick = { onListClick(list) },
                    colors = CardDefaults.cardColors(containerColor = ListsSurface),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val accent = listAccent(list.colorKey)
                        Box(
                            modifier = Modifier.size(48.dp).background(
                                accent.copy(alpha = .14f), RoundedCornerShape(14.dp)
                            ),
                            contentAlignment = Alignment.Center
                        ) { Text(listIcon(list.iconKey), color = accent, fontSize = 23.sp) }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(list.name, color = ListsText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(
                                list.description.ifBlank { "$count titles" },
                                color = ListsMuted,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(count.toString(), color = ListsText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TheatreSmartListCard(
    movieCount: Int,
    moviesThisYear: Int,
    totalMinutes: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = ListsSurface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(52.dp).background(
                    ListsPrimary.copy(alpha = .14f), RoundedCornerShape(15.dp)
                ),
                contentAlignment = Alignment.Center
            ) {
                Text("🎟", color = ListsPrimary, fontSize = 23.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Theatre watches", color = ListsText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    "$movieCount movies • $moviesThisYear this year • ${formatMinutes(totalMinutes)}",
                    color = ListsMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text("›", color = ListsMuted, fontSize = 24.sp)
        }
    }
}

@Composable
private fun TheatreWatchesDetail(
    paddingValues: PaddingValues,
    entries: List<TheatreWatchEntry>,
    moviesThisYear: Int,
    totalMinutes: Int,
    onBack: () -> Unit,
    onItemClick: (LibraryItem) -> Unit
) {
    val entriesByYear = entries
        .groupBy { entry ->
            LocalDate.ofEpochDay(entry.latestVisitEpochDay).year
        }
        .toList()
        .sortedByDescending { (year, _) -> year }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize().background(ListsBackground).padding(paddingValues),
        contentPadding = PaddingValues(14.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            DetailHeader("Theatre watches", onBack)
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniSummary(Modifier.weight(1f), entries.size.toString(), "Movies")
                MiniSummary(Modifier.weight(1f), moviesThisYear.toString(), "This year")
                MiniSummary(Modifier.weight(1f), formatMinutes(totalMinutes), "Watch time")
            }
        }
        if (entries.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyListCard("Movies marked as watched in a theatre will appear here automatically.")
            }
        } else {
            entriesByYear.forEach { (year, yearEntries) ->
                item(
                    key = "theatre-year-$year",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    Text(
                        text = "$year (${yearEntries.size} ${if (yearEntries.size == 1) "movie" else "movies"})",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 2.dp),
                        color = ListsText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(
                    items = yearEntries.sortedByDescending { entry ->
                        entry.latestVisitEpochDay
                    },
                    key = { entry ->
                        "theatre-$year-${entry.item.tmdbId}"
                    }
                ) { entry ->
                    TheatrePosterCard(
                        entry = entry,
                        onClick = { onItemClick(entry.item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TheatrePosterCard(
    entry: TheatreWatchEntry,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = ListsSurface),
        shape = RoundedCornerShape(13.dp)
    ) {
        Column {
            AsyncImage(
                model = entry.item.posterUrl,
                contentDescription = entry.item.title,
                modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.fillMaxWidth().height(100.dp).padding(8.dp)) {
                Text(
                    entry.item.title,
                    modifier = Modifier.height(28.dp),
                    color = ListsText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 12.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (entry.visitCount == 1) {
                        "Theatre"
                    } else {
                        "Watched ${entry.visitCount} times"
                    },
                    color = ListsPrimary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Last • ${formatEpochDayForLists(entry.latestVisitEpochDay)}",
                    color = ListsMuted,
                    fontSize = 8.sp,
                    maxLines = 1
                )
                Text(
                    formatMinutes(entry.totalMinutes),
                    color = ListsMuted,
                    fontSize = 8.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun MonthCard(month: MonthlyWatchList, onClick: (MonthlyWatchList) -> Unit) {
    Card(
        onClick = { onClick(month) },
        colors = CardDefaults.cardColors(containerColor = ListsSurface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(52.dp).background(
                    ListsPrimary.copy(alpha = .14f), RoundedCornerShape(15.dp)
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(month.month.toString().padStart(2, '0'), color = ListsPrimary, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(month.label, color = ListsText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${month.movieCount} movies • ${month.tvShowCount} TV shows • ${formatMinutes(month.totalMinutes)}",
                    color = ListsMuted,
                    fontSize = 12.sp
                )
            }
            Text("›", color = ListsMuted, fontSize = 24.sp)
        }
    }
}

@Composable
private fun MonthlyListDetail(
    paddingValues: PaddingValues,
    month: MonthlyWatchList,
    onBack: () -> Unit,
    onEntryClick: (MonthlyGridEntry) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxSize()
            .background(ListsBackground)
            .padding(paddingValues),
        contentPadding = PaddingValues(14.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            DetailHeader(month.label, onBack)
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniSummary(Modifier.weight(1f), month.movieCount.toString(), "Movies")
                MiniSummary(Modifier.weight(1f), month.tvShowCount.toString(), "TV shows")
                MiniSummary(Modifier.weight(1f), formatMinutes(month.totalMinutes), "Watch time")
            }
        }
        items(month.entries, key = { it.key }) { entry ->
            MonthlyPosterCard(entry = entry, onClick = { onEntryClick(entry) })
        }
    }
}

@Composable
private fun MonthlyPosterCard(entry: MonthlyGridEntry, onClick: () -> Unit) {
    val item = entry.item
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = ListsSurface),
        shape = RoundedCornerShape(13.dp)
    ) {
        Column {
            AsyncImage(
                model = item.posterUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
                    .padding(8.dp)
            ) {
                Text(
                    item.title,
                    modifier = Modifier.height(28.dp),
                    color = ListsText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 13.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (item.mediaType == "tv") {
                        "${entry.episodes.size} episodes"
                    } else {
                        "Movie"
                    },
                    color = ListsPrimary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (item.mediaType == "tv") {
                        item.totalEpisodes?.let {
                            "${entry.overallWatchedEpisodes}/$it overall • ${formatMinutes(entry.totalMinutes)}"
                        } ?: "${entry.overallWatchedEpisodes} overall • ${formatMinutes(entry.totalMinutes)}"
                    } else {
                        "${formatShortEpochDay(entry.watchedDateEpochDay)} • ${formatMinutes(entry.totalMinutes)}"
                    },
                    color = ListsMuted,
                    fontSize = 8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MonthlyTvDetail(
    paddingValues: PaddingValues,
    entry: MonthlyGridEntry,
    onBack: () -> Unit,
    onOpenSeries: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(ListsBackground).padding(paddingValues),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { DetailHeader(entry.item.title, onBack) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniSummary(Modifier.weight(1f), entry.episodes.size.toString(), "Episodes")
                MiniSummary(Modifier.weight(1f), formatMinutes(entry.totalMinutes), "Watch time")
            }
        }
        item {
            Button(
                onClick = onOpenSeries,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ListsPrimary)
            ) { Text("Open series and overall progress") }
        }
        columnItems(
            entry.episodes,
            key = { "${it.tmdbShowId}-${it.seasonNumber}-${it.episodeNumber}" }
        ) { episode ->
            Card(
                colors = CardDefaults.cardColors(containerColor = ListsSurface),
                shape = RoundedCornerShape(15.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).background(
                            ListsPrimary.copy(alpha = .14f), RoundedCornerShape(12.dp)
                        ),
                        contentAlignment = Alignment.Center
                    ) { Text(episode.episodeNumber.toString(), color = ListsPrimary, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${episode.episodeCode} • ${episode.episodeName}",
                            color = ListsText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2
                        )
                        Text(
                            "Watched ${formatEpochDayForLists(episode.watchedDateEpochDay)}" +
                                (episode.runtimeMinutes?.let { " • ${formatMinutes(it)}" } ?: ""),
                            color = ListsMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomListDetail(
    paddingValues: PaddingValues,
    list: CustomList,
    items: List<LibraryItem>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onRemove: (LibraryItem) -> Unit,
    onItemClick: (LibraryItem) -> Unit
) {
    var sortMode by remember(list.id) { mutableStateOf(CustomListSort.ADDED) }
    val sortedItems = remember(items, sortMode) {
        when (sortMode) {
            CustomListSort.ADDED -> items
            CustomListSort.TITLE -> items.sortedBy { it.title.lowercase() }
            CustomListSort.RECENT -> items.sortedByDescending { it.watchDateEpochDay ?: Long.MIN_VALUE }
        }
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize().background(ListsBackground).padding(paddingValues),
        contentPadding = PaddingValues(14.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { DetailHeader(list.name, onBack) }
        if (list.description.isNotBlank()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(list.description, color = ListsMuted, fontSize = 13.sp)
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onAdd,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ListsPrimary)
                ) { Text("+ Add titles") }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(onClick = onEdit) { Text("Edit") }
                    OutlinedButton(onClick = onDuplicate) { Text("Duplicate") }
                    TextButton(onClick = onDelete) { Text("Delete", color = ListsPrimary) }
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CustomListSort.entries.forEach { option ->
                    TextButton(onClick = { sortMode = option }) {
                        Text(
                            option.label,
                            color = if (sortMode == option) listAccent(list.colorKey) else ListsMuted,
                            fontSize = 11.sp,
                            fontWeight = if (sortMode == option) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
        if (items.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyListCard("No titles yet. Tap Add titles to build this list.")
            }
        }
        items(sortedItems, key = { "${it.mediaType}-${it.tmdbId}" }) { item ->
            CustomPosterCard(item, { onItemClick(item) }, { onRemove(item) })
        }
    }
}

@Composable
private fun CustomPosterCard(item: LibraryItem, onClick: () -> Unit, onRemove: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ListsSurface),
        shape = RoundedCornerShape(13.dp)
    ) {
        Column {
            AsyncImage(
                model = item.posterUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(topStart = 13.dp, topEnd = 13.dp)),
                contentScale = ContentScale.Crop
            )
            TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                Text(
                    item.title,
                    modifier = Modifier.fillMaxWidth(),
                    color = ListsText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 12.sp
                )
            }
            TextButton(
                onClick = onRemove,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) { Text("Remove", color = ListsPrimary, fontSize = 9.sp) }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(title, color = ListsText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = ListsMuted, fontSize = 12.sp)
    }
}

@Composable
private fun DetailHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(
            onClick = onBack,
            modifier = Modifier.size(44.dp),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ListsSurfaceLight)
        ) { Text("←", fontSize = 20.sp) }
        Spacer(Modifier.width(12.dp))
        Text(
            title,
            color = ListsText,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MiniSummary(modifier: Modifier, value: String, label: String) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = ListsSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(value, color = ListsText, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(label, color = ListsMuted, fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
private fun EmptyListCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ListsSurface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(
            message,
            modifier = Modifier.fillMaxWidth().padding(22.dp),
            color = ListsMuted,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun ListEditorDialog(
    title: String,
    initialName: String,
    initialDescription: String,
    initialColorKey: String,
    initialIconKey: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var description by remember(initialDescription) { mutableStateOf(initialDescription) }
    var colorKey by remember(initialColorKey) { mutableStateOf(initialColorKey) }
    var iconKey by remember(initialIconKey) { mutableStateOf(initialIconKey) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("List name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    maxLines = 3
                )
                Text("Color", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listColors.forEach { option ->
                        Button(
                            onClick = { colorKey = option.first },
                            modifier = Modifier.size(if (colorKey == option.first) 42.dp else 36.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = option.second),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text(if (colorKey == option.first) "✓" else "") }
                    }
                }
                Text("Icon", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listIcons.forEach { option ->
                        OutlinedButton(
                            onClick = { iconKey = option.first },
                            modifier = Modifier.size(42.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                option.second,
                                color = if (iconKey == option.first) listAccent(colorKey) else ListsMuted
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, description, colorKey, iconKey) }, enabled = name.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun TitlePickerDialog(
    allItems: List<LibraryItem>,
    selectedItems: List<LibraryItem>,
    onDismiss: () -> Unit,
    onAdd: (LibraryItem) -> Unit,
    onRemove: (LibraryItem) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var mediaFilter by remember { mutableStateOf("all") }
    val selectedKeys = selectedItems.map { it.tmdbId to it.mediaType }.toSet()
    val visibleItems = allItems.filter { item ->
        (mediaFilter == "all" || item.mediaType == mediaFilter) &&
            (query.isBlank() || item.title.contains(query.trim(), ignoreCase = true))
    }.sortedBy { it.title.lowercase() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add titles") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search your library") },
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("all" to "All", "movie" to "Movies", "tv" to "TV").forEach { filter ->
                        TextButton(onClick = { mediaFilter = filter.first }) {
                            Text(
                                filter.second,
                                color = if (mediaFilter == filter.first) ListsPrimary else ListsMuted,
                                fontWeight = if (mediaFilter == filter.first) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                LazyColumn(
                    modifier = Modifier.height(350.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    columnItems(visibleItems, key = { "pick-${it.mediaType}-${it.tmdbId}" }) { item ->
                    val selected = (item.tmdbId to item.mediaType) in selectedKeys
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            item.title,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        TextButton(onClick = { if (selected) onRemove(item) else onAdd(item) }) {
                            Text(if (selected) "Remove" else "Add")
                        }
                    }
                }
                    if (visibleItems.isEmpty()) {
                        item {
                            Text(
                                "No matching titles",
                                modifier = Modifier.fillMaxWidth().padding(20.dp),
                                color = ListsMuted
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

private fun formatEpochDayForLists(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("dd MMM yyyy"))

private fun formatShortEpochDay(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("dd MMM"))

private fun formatMinutes(minutes: Int): String {
    if (minutes <= 0) return "0m"
    val hours = minutes / 60
    val remainder = minutes % 60
    return if (hours > 0) "${hours}h ${remainder}m" else "${remainder}m"
}
