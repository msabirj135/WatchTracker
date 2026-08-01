package com.sabir.watchtracker.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sabir.watchtracker.BuildConfig
import com.sabir.watchtracker.data.backup.BackupImportMode
import com.sabir.watchtracker.ui.library.BackupUiState
import com.sabir.watchtracker.ui.library.DataHealthUiState
import com.sabir.watchtracker.ui.library.LibraryUiState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val SettingsBackground = Color(0xFF090B10)
private val SettingsSurface = Color(0xFF12151D)
private val SettingsSurfaceLight = Color(0xFF1A1E28)
private val SettingsPrimary = Color(0xFFE63946)
private val SettingsSuccess = Color(0xFF36C98F)
private val SettingsTextPrimary = Color(0xFFF5F5F7)
private val SettingsTextSecondary = Color(0xFF9A9DA8)

@Composable
fun SettingsScreen(
    libraryUiState: LibraryUiState,
    backupUiState: BackupUiState,
    dataHealthUiState: DataHealthUiState,
    onBackClick: () -> Unit,
    onExportBackup: (Uri) -> Unit,
    onExportCsv: (Uri) -> Unit,
    onInspectBackup: (Uri) -> Unit,
    onRestoreBackup: (BackupImportMode) -> Unit,
    onRestoreSafetyBackup: () -> Unit,
    onDismissBackupPreview: () -> Unit,
    onClearBackupMessage: () -> Unit,
    onClearAllData: () -> Unit,
    onRunHealthCheck: () -> Unit,
    onRepairData: () -> Unit
) {
    var showClearConfirmation by remember {
        mutableStateOf(false)
    }
    var clearConfirmationText by remember {
        mutableStateOf("")
    }
    var showSafetyRestoreConfirmation by remember {
        mutableStateOf(false)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let(onExportBackup)
    }

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let(onExportCsv)
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let(onInspectBackup)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SettingsBackground)
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 18.dp,
            end = 20.dp,
            bottom = 34.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SettingsHeader(onBackClick)
        }

        item {
            BackupCard(
                state = backupUiState,
                onExport = {
                    exportLauncher.launch(
                        "ReelTick-backup-${LocalDate.now()}.json"
                    )
                },
                onImport = {
                    importLauncher.launch(
                        arrayOf(
                            "application/json",
                            "text/plain",
                            "application/octet-stream"
                        )
                    )
                },
                onExportCsv = {
                    csvLauncher.launch(
                        "ReelTick-history-${LocalDate.now()}.csv"
                    )
                },
                onRestoreSafety = {
                    showSafetyRestoreConfirmation = true
                }
            )
        }

        item {
            DatabaseInformationCard(libraryUiState)
        }

        item {
            DataHealthCard(
                state = dataHealthUiState,
                onCheck = onRunHealthCheck,
                onRepair = onRepairData
            )
        }

        item {
            ReleaseManagementCard()
        }

        item {
            DangerZoneCard(
                isWorking = backupUiState.isWorking,
                onClearClick = {
                    clearConfirmationText = ""
                    showClearConfirmation = true
                }
            )
        }
    }

    backupUiState.pendingPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = onDismissBackupPreview,
            containerColor = SettingsSurface,
            title = {
                Text(
                    text = "Restore ReelTick backup?",
                    color = SettingsTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "${preview.titleCount} titles • ${preview.episodeCount} episodes • ${preview.rewatchCount} rewatches • ${preview.customListCount} lists",
                        color = SettingsTextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Merge keeps current data. Replace first creates an internal safety backup, then replaces the library.",
                        color = SettingsTextSecondary,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Row {
                    TextButton(
                        onClick = {
                            onRestoreBackup(BackupImportMode.REPLACE)
                        },
                        enabled = !backupUiState.isWorking
                    ) {
                        Text("Replace", color = SettingsPrimary)
                    }
                    TextButton(
                        onClick = {
                            onRestoreBackup(BackupImportMode.MERGE)
                        },
                        enabled = !backupUiState.isWorking
                    ) {
                        Text(
                            "Merge",
                            color = SettingsSuccess,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissBackupPreview) {
                    Text("Cancel", color = SettingsTextSecondary)
                }
            }
        )
    }

    if (showSafetyRestoreConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showSafetyRestoreConfirmation = false
            },
            containerColor = SettingsSurface,
            title = {
                Text("Restore safety backup?")
            },
            text = {
                Text(
                    "The current library will be replaced with the automatic safety copy."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSafetyRestoreConfirmation = false
                        onRestoreSafetyBackup()
                    }
                ) {
                    Text(
                        "Restore",
                        color = SettingsSuccess,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSafetyRestoreConfirmation = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showClearConfirmation = false
            },
            containerColor = SettingsSurface,
            title = {
                Text("Clear all ReelTick data?")
            },
            text = {
                Column {
                    Text(
                        "An internal safety backup will be created first. Type REELTICK to continue."
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = clearConfirmationText,
                        onValueChange = {
                            clearConfirmationText = it
                        },
                        singleLine = true,
                        label = { Text("Confirmation") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmation = false
                        onClearAllData()
                    },
                    enabled = clearConfirmationText == "REELTICK"
                ) {
                    Text(
                        "Clear everything",
                        color = SettingsPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showClearConfirmation = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    val feedback = backupUiState.successMessage
        ?: backupUiState.errorMessage

    if (feedback != null) {
        AlertDialog(
            onDismissRequest = onClearBackupMessage,
            containerColor = SettingsSurface,
            title = {
                Text(
                    if (backupUiState.errorMessage == null) {
                        "Operation complete"
                    } else {
                        "Operation failed"
                    }
                )
            },
            text = { Text(feedback) },
            confirmButton = {
                TextButton(onClick = onClearBackupMessage) {
                    Text("OK", color = SettingsPrimary)
                }
            }
        )
    }
}

@Composable
private fun SettingsHeader(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onBackClick,
            modifier = Modifier.size(46.dp),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SettingsSurfaceLight
            ),
            shape = RoundedCornerShape(13.dp)
        ) {
            Text("←", fontSize = 21.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                "Settings",
                color = SettingsTextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "ReelTick preferences and data safety",
                color = SettingsTextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun BackupCard(
    state: BackupUiState,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onExportCsv: () -> Unit,
    onRestoreSafety: () -> Unit
) {
    SettingsCard(title = "Backup & restore") {
        if (state.isWorking) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = SettingsPrimary,
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Working…", color = SettingsTextSecondary)
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        InformationRow(
            "Last exported backup",
            formatTimestamp(state.lastBackupEpochMillis)
        )
        InformationRow(
            "Automatic safety backup",
            formatTimestamp(state.safetyBackupEpochMillis)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SettingsActionButton(
                modifier = Modifier.weight(1f),
                text = "Export",
                enabled = !state.isWorking,
                primary = true,
                onClick = onExport
            )
            SettingsActionButton(
                modifier = Modifier.weight(1f),
                text = "Import",
                enabled = !state.isWorking,
                onClick = onImport
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        SettingsActionButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Export watch history as CSV",
            enabled = !state.isWorking,
            onClick = onExportCsv
        )

        if (state.safetyBackupEpochMillis != null) {
            Spacer(modifier = Modifier.height(8.dp))
            SettingsActionButton(
                modifier = Modifier.fillMaxWidth(),
                text = "Restore automatic safety backup",
                enabled = !state.isWorking,
                onClick = onRestoreSafety
            )
        }
    }
}

@Composable
private fun DatabaseInformationCard(state: LibraryUiState) {
    SettingsCard(title = "Library database") {
        InformationRow("Titles", state.totalCount.toString())
        InformationRow("Movies", state.movieCount.toString())
        InformationRow("TV shows", state.tvShowCount.toString())
        InformationRow(
            "Watched episodes",
            state.watchedEpisodeCount.toString()
        )
        InformationRow("Rewatches", state.rewatchCount.toString())
        InformationRow("Custom lists", state.customLists.size.toString())
    }
}

@Composable
private fun DataHealthCard(
    state: DataHealthUiState,
    onCheck: () -> Unit,
    onRepair: () -> Unit
) {
    SettingsCard(title = "Data health") {
        when {
            state.isWorking -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = SettingsPrimary,
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Repairing database…", color = SettingsTextSecondary)
                }
            }
            !state.hasRun -> {
                Text(
                    "Check your library for detached records, future watch dates and incomplete title metadata.",
                    color = SettingsTextSecondary,
                    fontSize = 12.sp
                )
            }
            else -> {
                Text(
                    if (state.isHealthy) "✓ Database structure is healthy" else "Review recommended",
                    color = if (state.isHealthy) SettingsSuccess else SettingsPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                InformationRow("Orphaned records", state.orphanedRecords.toString())
                InformationRow("Future watch dates", state.futureDatedRecords.toString())
                InformationRow("Titles missing metadata", state.missingMetadataTitles.toString())
                if (state.futureDatedRecords > 0) {
                    Text(
                        "Future dates are reported but never changed automatically. Edit them from the title or episode screen.",
                        modifier = Modifier.padding(top = 8.dp),
                        color = SettingsTextSecondary,
                        fontSize = 10.sp
                    )
                }
                if (state.missingMetadataTitles > 0) {
                    Text(
                        "Metadata is refreshed automatically when TMDB is reachable.",
                        modifier = Modifier.padding(top = 6.dp),
                        color = SettingsTextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }

        state.message?.let { message ->
            Spacer(modifier = Modifier.height(10.dp))
            Text(message, color = SettingsSuccess, fontSize = 11.sp)
        }

        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SettingsActionButton(
                modifier = Modifier.weight(1f),
                text = if (state.hasRun) "Check again" else "Run check",
                enabled = !state.isWorking,
                primary = true,
                onClick = onCheck
            )
            SettingsActionButton(
                modifier = Modifier.weight(1f),
                text = "Safe repair",
                enabled = !state.isWorking && state.hasRun && state.orphanedRecords > 0,
                onClick = onRepair
            )
        }
    }
}

@Composable
private fun ReleaseManagementCard() {
    SettingsCard(title = "Version & updates") {
        InformationRow("Version", BuildConfig.VERSION_NAME)
        InformationRow("Build", BuildConfig.VERSION_CODE.toString())
        InformationRow("Release channel", BuildConfig.RELEASE_CHANNEL)
        InformationRow(
            "Database schema",
            BuildConfig.DATABASE_SCHEMA_VERSION.toString()
        )

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            "What’s new in 1.1.0",
            color = SettingsTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        ReleaseNote("Richer title information and viewing timelines")
        ReleaseNote("Custom list colors, icons, duplication and sorting")
        ReleaseNote("Safe database health checks and repair")

        Spacer(modifier = Modifier.height(14.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = SettingsSuccess.copy(alpha = 0.10f)
            )
        ) {
            Column(modifier = Modifier.padding(13.dp)) {
                Text(
                    "Updating ReelTick safely",
                    color = SettingsSuccess,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Create a backup, then install the newer APK over the existing app. Do not uninstall first. Updates must use the same package name and signing key.",
                    modifier = Modifier.padding(top = 5.dp),
                    color = SettingsTextSecondary,
                    fontSize = 10.sp
                )
            }
        }

        Text(
            text = "A private, personal movie and TV tracker.",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            color = SettingsTextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ReleaseNote(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 7.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("•", color = SettingsPrimary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text,
            modifier = Modifier.weight(1f),
            color = SettingsTextSecondary,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun DangerZoneCard(
    isWorking: Boolean,
    onClearClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = SettingsPrimary.copy(alpha = 0.10f)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                "Danger zone",
                color = SettingsPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Remove every title, episode and custom list.",
                color = SettingsTextSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onClearClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isWorking,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SettingsPrimary
                )
            ) {
                Text(
                    "Clear all data",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = SettingsSurface
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                title,
                color = SettingsTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun InformationRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            color = SettingsTextSecondary,
            fontSize = 13.sp
        )
        Text(
            value,
            color = SettingsTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SettingsActionButton(
    modifier: Modifier,
    text: String,
    enabled: Boolean,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) {
                SettingsPrimary
            } else {
                SettingsSurfaceLight
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text,
            color = SettingsTextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

private fun formatTimestamp(epochMillis: Long?): String {
    if (epochMillis == null) return "Never"

    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))
}
