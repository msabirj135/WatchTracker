package com.sabir.watchtracker.ui.library

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.Color
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
private val ListsSuccess = Color(0xFF36C98F)
private val ListsText = Color(0xFFF5F5F7)
private val ListsMuted = Color(0xFF9A9DA8)

@Composable
fun ListsScreen(
    paddingValues: PaddingValues,
    state: LibraryUiState,
    onCreateList: (String, String) -> Unit,
    onUpdateList: (CustomList, String, String) -> Unit,
    onDeleteList: (Long) -> Unit,
    onAddItem: (Long, LibraryItem) -> Unit,
    onRemoveItem: (Long, LibraryItem) -> Unit,
    onItemClick: (LibraryItem) -> Unit
) {
    var selectedMonth by remember { mutableStateOf<MonthlyWatchList?>(null) }
    var selectedList by remember { mutableStateOf<CustomList?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showAddTitles by remember { mutableStateOf(false) }
    var editingList by remember { mutableStateOf<CustomList?>(null) }

    BackHandler(enabled = selectedMonth != null || selectedList != null) {
        selectedMonth = null
        selectedList = null
    }

    when {
        selectedMonth != null -> MonthlyListDetail(
            paddingValues = paddingValues,
            month = selectedMonth!!,
            onBack = { selectedMonth = null },
            onItemClick = onItemClick
        )

        selectedList != null -> CustomListDetail(
            paddingValues = paddingValues,
            list = selectedList!!,
            items = state.itemsForList(selectedList!!.id),
            onBack = { selectedList = null },
            onAdd = { showAddTitles = true },
            onEdit = { editingList = selectedList },
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
            onNewList = { showCreateDialog = true },
            onMonthClick = { selectedMonth = it },
            onListClick = { selectedList = it }
        )
    }

    if (showCreateDialog) {
        ListEditorDialog(
            title = "Create list",
            initialName = "",
            initialDescription = "",
            onDismiss = { showCreateDialog = false },
            onSave = { name, description ->
                onCreateList(name, description)
                showCreateDialog = false
            }
        )
    }

    editingList?.let { list ->
        ListEditorDialog(
            title = "Edit list",
            initialName = list.name,
            initialDescription = list.description,
            onDismiss = { editingList = null },
            onSave = { name, description ->
                onUpdateList(list, name, description)
                selectedList = list.copy(name = name.trim(), description = description.trim())
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
    onNewList: () -> Unit,
    onMonthClick: (MonthlyWatchList) -> Unit,
    onListClick: (CustomList) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(ListsBackground).padding(paddingValues),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
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

        item { SectionTitle("Smart lists", "Grouped automatically by watch date") }

        if (state.monthlyLists.isEmpty()) {
            item { EmptyListCard("Watch a movie or episode to create your first monthly list.") }
        } else {
            val years = state.monthlyLists.groupBy { it.year }
            years.forEach { (year, months) ->
                item { Text(year.toString(), color = ListsPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
                items(months, key = { "month-${it.year}-${it.month}" }) { month ->
                    MonthCard(month, onMonthClick)
                }
            }
        }

        item { SectionTitle("My lists", "${state.customLists.size} custom lists") }

        if (state.customLists.isEmpty()) {
            item { EmptyListCard("Create a list for favourites, recommendations or anything else.") }
        } else {
            items(state.customLists, key = { it.id }) { list ->
                val count = state.customListItems.count { it.listId == list.id }
                Card(
                    onClick = { onListClick(list) },
                    colors = CardDefaults.cardColors(containerColor = ListsSurface),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(48.dp).background(ListsPrimary.copy(alpha = .14f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                            Text("☷", color = ListsPrimary, fontSize = 23.sp)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(list.name, color = ListsText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(list.description.ifBlank { "$count titles" }, color = ListsMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(count.toString(), color = ListsText, fontWeight = FontWeight.Bold)
                    }
                }
            }
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
private fun MonthCard(month: MonthlyWatchList, onClick: (MonthlyWatchList) -> Unit) {
    Card(onClick = { onClick(month) }, colors = CardDefaults.cardColors(containerColor = ListsSurface), shape = RoundedCornerShape(18.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(52.dp).background(ListsPrimary.copy(alpha = .14f), RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
                Text(month.month.toString().padStart(2, '0'), color = ListsPrimary, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(month.label, color = ListsText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("${month.entries.size} watched • ${formatMinutes(month.totalMinutes)}", color = ListsMuted, fontSize = 12.sp)
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
    onItemClick: (LibraryItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(ListsBackground).padding(paddingValues),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { DetailHeader(month.label, onBack) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniSummary(Modifier.weight(1f), month.entries.size.toString(), "Entries")
                MiniSummary(Modifier.weight(1f), formatMinutes(month.totalMinutes), "Watch time")
            }
        }
        items(month.entries, key = { it.key }) { entry ->
            HistoryListRow(entry.item, entry.detailText, entry.watchedDateEpochDay, { onItemClick(entry.item) })
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
    onDelete: () -> Unit,
    onRemove: (LibraryItem) -> Unit,
    onItemClick: (LibraryItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(ListsBackground).padding(paddingValues),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { DetailHeader(list.name, onBack) }
        if (list.description.isNotBlank()) item { Text(list.description, color = ListsMuted, fontSize = 13.sp) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = ListsPrimary)) { Text("+ Add titles") }
                OutlinedButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) { Text("Delete", color = ListsPrimary) }
            }
        }
        if (items.isEmpty()) item { EmptyListCard("No titles yet. Tap Add titles to build this list.") }
        items(items, key = { "${it.mediaType}-${it.tmdbId}" }) { item ->
            Card(colors = CardDefaults.cardColors(containerColor = ListsSurface), shape = RoundedCornerShape(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(model = item.posterUrl, contentDescription = item.title, modifier = Modifier.width(48.dp).height(70.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        TextButton(onClick = { onItemClick(item) }, contentPadding = PaddingValues(0.dp)) {
                            Text(item.title, color = ListsText, fontWeight = FontWeight.Bold, maxLines = 2)
                        }
                        Text("${item.displayMediaType} • ${item.displayYear}", color = ListsMuted, fontSize = 11.sp)
                    }
                    TextButton(onClick = { onRemove(item) }) { Text("Remove", color = ListsPrimary) }
                }
            }
        }
    }
}

@Composable
private fun HistoryListRow(item: LibraryItem, detail: String, epochDay: Long, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = ListsSurface), shape = RoundedCornerShape(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = item.posterUrl, contentDescription = item.title, modifier = Modifier.width(48.dp).height(70.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, color = ListsText, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(detail, color = ListsPrimary, fontSize = 11.sp, maxLines = 1)
                Text(LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("dd MMM yyyy")), color = ListsMuted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun DetailHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = onBack, modifier = Modifier.size(44.dp), contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.buttonColors(containerColor = ListsSurfaceLight)) { Text("←", fontSize = 20.sp) }
        Spacer(Modifier.width(12.dp))
        Text(title, color = ListsText, fontSize = 23.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MiniSummary(modifier: Modifier, value: String, label: String) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = ListsSurface), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(value, color = ListsText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(label, color = ListsMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun EmptyListCard(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = ListsSurface), shape = RoundedCornerShape(18.dp)) {
        Text(message, modifier = Modifier.fillMaxWidth().padding(22.dp), color = ListsMuted, fontSize = 13.sp)
    }
}

@Composable
private fun ListEditorDialog(
    title: String,
    initialName: String,
    initialDescription: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var description by remember(initialDescription) { mutableStateOf(initialDescription) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("List name") }, singleLine = true)
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description (optional)") }, maxLines = 3)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name, description) }, enabled = name.isNotBlank()) { Text("Save") } },
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
    val selectedKeys = selectedItems.map { it.tmdbId to it.mediaType }.toSet()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add titles") },
        text = {
            LazyColumn(modifier = Modifier.height(420.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(allItems, key = { "pick-${it.mediaType}-${it.tmdbId}" }) { item ->
                    val selected = (item.tmdbId to item.mediaType) in selectedKeys
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(item.title, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        TextButton(onClick = { if (selected) onRemove(item) else onAdd(item) }) {
                            Text(if (selected) "Remove" else "Add")
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

private fun formatMinutes(minutes: Int): String {
    if (minutes <= 0) return "0m"
    val hours = minutes / 60
    val remainder = minutes % 60
    return if (hours > 0) "${hours}h ${remainder}m" else "${remainder}m"
}
