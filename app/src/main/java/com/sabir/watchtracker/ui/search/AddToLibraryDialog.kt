package com.sabir.watchtracker.ui.search

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sabir.watchtracker.data.local.LibraryStatus
import com.sabir.watchtracker.data.remote.TmdbEpisode
import com.sabir.watchtracker.data.remote.TmdbSearchResult
import com.sabir.watchtracker.data.remote.TmdbSeasonDetails
import com.sabir.watchtracker.data.remote.TmdbTvDetails
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DialogBackground = Color(0xFF12151D)
private val DialogSurfaceLight = Color(0xFF1A1E28)
private val DialogPrimary = Color(0xFFE63946)
private val DialogWarning = Color(0xFFFFC857)
private val DialogTextPrimary = Color(0xFFF5F5F7)
private val DialogTextSecondary = Color(0xFF9A9DA8)

@Composable
fun AddToLibraryDialog(
    result: TmdbSearchResult,
    isSaving: Boolean,
    errorMessage: String?,
    tvDetails: TmdbTvDetails?,
    seasonDetails: TmdbSeasonDetails?,
    isLoadingTvDetails: Boolean,
    isLoadingSeason: Boolean,
    tvDetailsErrorMessage: String?,
    onSeasonSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    onSave: (
        status: LibraryStatus,
        watchDateEpochDay: Long?,
        personalRating: Double?,
        notes: String,
        selectedEpisode: TmdbEpisode?
    ) -> Unit
) {
    var selectedStatus by remember(result.id) {
        mutableStateOf(LibraryStatus.PLAN_TO_WATCH)
    }

    var watchDateEpochDay by remember(result.id) {
        mutableStateOf<Long?>(null)
    }

    var personalRatingText by remember(result.id) {
        mutableStateOf("")
    }

    var notes by remember(result.id) {
        mutableStateOf("")
    }

    var selectedSeasonNumber by remember(result.id) {
        mutableIntStateOf(0)
    }

    var selectedEpisodeNumber by remember(result.id) {
        mutableIntStateOf(0)
    }

    val isTvShow = result.mediaType == "tv"

    val selectedEpisode = seasonDetails
        ?.episodes
        ?.firstOrNull { episode ->
            episode.episodeNumber ==
                selectedEpisodeNumber
        }

    val parsedPersonalRating = personalRatingText
        .trim()
        .replace(",", ".")
        .takeIf { it.isNotEmpty() }
        ?.toDoubleOrNull()

    val ratingIsValid =
        personalRatingText.isBlank() ||
            (
                parsedPersonalRating != null &&
                    parsedPersonalRating in 0.0..10.0
                )

    val episodeIsRequired =
        isTvShow &&
            selectedStatus != LibraryStatus.PLAN_TO_WATCH

    val episodeSelectionIsValid =
        !episodeIsRequired ||
            selectedEpisode != null

    val dateIsRequired =
        selectedStatus == LibraryStatus.COMPLETED ||
            selectedStatus == LibraryStatus.WATCHING

    val dateSelectionIsValid =
        !dateIsRequired ||
            watchDateEpochDay != null

    LaunchedEffect(tvDetails?.id) {
        val firstSeason = tvDetails
            ?.regularSeasons
            ?.firstOrNull()

        if (
            isTvShow &&
            selectedSeasonNumber == 0 &&
            firstSeason != null
        ) {
            selectedSeasonNumber =
                firstSeason.seasonNumber
        }
    }

    LaunchedEffect(seasonDetails?.seasonNumber) {
        selectedEpisodeNumber = 0

        if (
            selectedStatus == LibraryStatus.COMPLETED
        ) {
            selectedEpisodeNumber =
                seasonDetails
                    ?.episodes
                    ?.lastOrNull()
                    ?.episodeNumber
                    ?: 0
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isSaving) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isSaving,
            dismissOnClickOutside = !isSaving,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 18.dp),
            color = DialogBackground,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp
        ) {
            LazyColumn(
                modifier = Modifier.padding(
                    horizontal = 20.dp,
                    vertical = 22.dp
                )
            ) {
                item {
                    Text(
                        text = "Add to Library",
                        color = DialogTextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = result.displayTitle,
                        color = DialogTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = buildString {
                            append(result.displayMediaType)

                            if (result.displayYear != "—") {
                                append(" • ")
                                append(result.displayYear)
                            }
                        },
                        color = DialogTextSecondary,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    FormLabel("Status")

                    Spacer(modifier = Modifier.height(10.dp))

                    StatusSelector(
                        selectedStatus = selectedStatus,
                        onStatusSelected = { status ->
                            selectedStatus = status

                            when (status) {
                                LibraryStatus.PLAN_TO_WATCH -> {
                                    watchDateEpochDay = null
                                    selectedEpisodeNumber = 0
                                }

                                LibraryStatus.WATCHING -> {
                                    if (watchDateEpochDay == null) {
                                        watchDateEpochDay =
                                            LocalDate.now()
                                                .toEpochDay()
                                    }
                                }

                                LibraryStatus.COMPLETED -> {
                                    if (watchDateEpochDay == null) {
                                        watchDateEpochDay =
                                            LocalDate.now()
                                                .toEpochDay()
                                    }

                                    val lastSeason = tvDetails
                                        ?.regularSeasons
                                        ?.lastOrNull()

                                    if (
                                        isTvShow &&
                                        lastSeason != null
                                    ) {
                                        selectedSeasonNumber =
                                            lastSeason.seasonNumber

                                        selectedEpisodeNumber = 0

                                        onSeasonSelected(
                                            lastSeason.seasonNumber
                                        )
                                    }
                                }

                                LibraryStatus.DROPPED -> Unit
                            }
                        }
                    )

                    if (
                        isTvShow &&
                        selectedStatus !=
                            LibraryStatus.PLAN_TO_WATCH
                    ) {
                        Spacer(modifier = Modifier.height(24.dp))

                        FormLabel("Episode progress")

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = when (selectedStatus) {
                                LibraryStatus.WATCHING ->
                                    "Select the latest episode you watched."

                                LibraryStatus.COMPLETED ->
                                    "The final episode is selected automatically."

                                LibraryStatus.DROPPED ->
                                    "Select where you stopped watching."

                                LibraryStatus.PLAN_TO_WATCH ->
                                    ""
                            },
                            color = DialogTextSecondary,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        TvProgressSelector(
                            tvDetails = tvDetails,
                            seasonDetails = seasonDetails,
                            selectedSeasonNumber =
                                selectedSeasonNumber,
                            selectedEpisodeNumber =
                                selectedEpisodeNumber,
                            isLoadingTvDetails =
                                isLoadingTvDetails,
                            isLoadingSeason =
                                isLoadingSeason,
                            errorMessage =
                                tvDetailsErrorMessage,
                            onSeasonSelected = { seasonNumber ->
                                selectedSeasonNumber =
                                    seasonNumber
                                selectedEpisodeNumber = 0

                                onSeasonSelected(
                                    seasonNumber
                                )
                            },
                            onEpisodeSelected = { episodeNumber ->
                                selectedEpisodeNumber =
                                    episodeNumber
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    FormLabel(
                        if (isTvShow && episodeIsRequired) {
                            "Episode watch date"
                        } else {
                            "Watch date"
                        }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (
                            isTvShow &&
                            selectedStatus ==
                                LibraryStatus.PLAN_TO_WATCH
                        ) {
                            "Not required until you start watching."
                        } else {
                            "Completed and Watching titles default to today."
                        },
                        color = DialogTextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    NativeDateSelector(
                        watchDateEpochDay =
                            watchDateEpochDay,
                        onDateSelected = { selectedEpochDay ->
                            watchDateEpochDay =
                                selectedEpochDay
                        },
                        onClearDate = {
                            watchDateEpochDay = null
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    FormLabel("Personal rating")

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = personalRatingText,
                        onValueChange = { value ->
                            if (
                                value.length <= 4 &&
                                value.all { character ->
                                    character.isDigit() ||
                                        character == '.' ||
                                        character == ','
                                }
                            ) {
                                personalRatingText = value
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("Optional rating from 0 to 10")
                        },
                        suffix = {
                            Text(
                                text = "/ 10",
                                color = DialogTextSecondary
                            )
                        },
                        singleLine = true,
                        isError = !ratingIsValid,
                        supportingText = {
                            if (!ratingIsValid) {
                                Text(
                                    "Enter a number between 0 and 10."
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        shape = RoundedCornerShape(14.dp),
                        colors = dialogTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    FormLabel("Notes")

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { value ->
                            if (value.length <= 500) {
                                notes = value
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Optional thoughts or reminders"
                            )
                        },
                        minLines = 3,
                        maxLines = 5,
                        supportingText = {
                            Text("${notes.length}/500")
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = dialogTextFieldColors()
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = errorMessage,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = DialogPrimary.copy(
                                        alpha = 0.12f
                                    ),
                                    shape =
                                        RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp),
                            color = DialogPrimary,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            enabled = !isSaving,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                onSave(
                                    selectedStatus,
                                    watchDateEpochDay,
                                    parsedPersonalRating,
                                    notes,
                                    selectedEpisode
                                )
                            },
                            modifier = Modifier.weight(1f),
                            enabled =
                                ratingIsValid &&
                                    episodeSelectionIsValid &&
                                    dateSelectionIsValid &&
                                    !isSaving &&
                                    !isLoadingTvDetails &&
                                    !isLoadingSeason,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DialogPrimary,
                                contentColor = Color.White,
                                disabledContainerColor =
                                    DialogSurfaceLight,
                                disabledContentColor =
                                    DialogTextSecondary
                            )
                        ) {
                            Text(
                                text = if (isSaving) {
                                    "Saving..."
                                } else {
                                    "Add"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun TvProgressSelector(
    tvDetails: TmdbTvDetails?,
    seasonDetails: TmdbSeasonDetails?,
    selectedSeasonNumber: Int,
    selectedEpisodeNumber: Int,
    isLoadingTvDetails: Boolean,
    isLoadingSeason: Boolean,
    errorMessage: String?,
    onSeasonSelected: (Int) -> Unit,
    onEpisodeSelected: (Int) -> Unit
) {
    when {
        isLoadingTvDetails -> {
            LoadingRow("Loading seasons...")
        }

        errorMessage != null -> {
            Text(
                text = errorMessage,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = DialogPrimary.copy(
                            alpha = 0.12f
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp),
                color = DialogPrimary,
                fontSize = 12.sp
            )
        }

        tvDetails == null -> {
            Text(
                text = "TV-show details are unavailable.",
                color = DialogTextSecondary,
                fontSize = 12.sp
            )
        }

        else -> {
            Text(
                text = "${tvDetails.numberOfSeasons} seasons • ${tvDetails.numberOfEpisodes} episodes",
                color = DialogTextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = tvDetails.regularSeasons,
                    key = { season ->
                        season.seasonNumber
                    }
                ) { season ->
                    FilterChip(
                        selected =
                            selectedSeasonNumber ==
                                season.seasonNumber,
                        onClick = {
                            onSeasonSelected(
                                season.seasonNumber
                            )
                        },
                        label = {
                            Text(
                                text =
                                    "Season ${season.seasonNumber}"
                            )
                        },
                        colors = progressChipColors()
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                isLoadingSeason -> {
                    LoadingRow("Loading episodes...")
                }

                seasonDetails == null -> {
                    Text(
                        text = "Select a season.",
                        color = DialogTextSecondary,
                        fontSize = 12.sp
                    )
                }

                else -> {
                    LazyRow(
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items =
                                seasonDetails.episodes,
                            key = { episode ->
                                episode.id
                            }
                        ) { episode ->
                            FilterChip(
                                selected =
                                    selectedEpisodeNumber ==
                                        episode.episodeNumber,
                                onClick = {
                                    onEpisodeSelected(
                                        episode.episodeNumber
                                    )
                                },
                                label = {
                                    Text(
                                        text =
                                            "E${episode.episodeNumber}"
                                    )
                                },
                                colors =
                                    progressChipColors()
                            )
                        }
                    }

                    if (selectedEpisodeNumber == 0) {
                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Text(
                            text = "Choose the latest episode watched.",
                            color = DialogWarning,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingRow(
    message: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = DialogPrimary,
            strokeWidth = 2.dp
        )

        Text(
            text = message,
            color = DialogTextSecondary,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun FormLabel(
    text: String
) {
    Text(
        text = text,
        color = DialogTextPrimary,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun StatusSelector(
    selectedStatus: LibraryStatus,
    onStatusSelected: (LibraryStatus) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LibraryStatus.entries.forEach { status ->
            FilterChip(
                selected = selectedStatus == status,
                onClick = {
                    onStatusSelected(status)
                },
                label = {
                    Text(
                        text = status.displayName,
                        modifier = Modifier.padding(
                            vertical = 3.dp
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = DialogSurfaceLight,
                    labelColor = DialogTextSecondary,
                    selectedContainerColor =
                        DialogPrimary.copy(alpha = 0.18f),
                    selectedLabelColor = DialogPrimary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedStatus == status,
                    borderColor = DialogSurfaceLight,
                    selectedBorderColor = DialogPrimary
                )
            )
        }
    }
}

@Composable
private fun NativeDateSelector(
    watchDateEpochDay: Long?,
    onDateSelected: (Long) -> Unit,
    onClearDate: () -> Unit
) {
    val context = LocalContext.current

    val initialDate = watchDateEpochDay?.let {
        LocalDate.ofEpochDay(it)
    } ?: LocalDate.now()

    val datePickerDialog = remember(
        context,
        watchDateEpochDay
    ) {
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
        )
    }

    Column {
        OutlinedButton(
            onClick = {
                datePickerDialog.show()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = watchDateEpochDay?.let {
                    formatEpochDay(it)
                } ?: "Choose watch date"
            )
        }

        if (watchDateEpochDay != null) {
            TextButton(
                onClick = onClearDate,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Clear date",
                    color = DialogTextSecondary
                )
            }
        }
    }
}

@Composable
private fun progressChipColors() =
    FilterChipDefaults.filterChipColors(
        containerColor = DialogSurfaceLight,
        labelColor = DialogTextSecondary,
        selectedContainerColor =
            DialogPrimary.copy(alpha = 0.18f),
        selectedLabelColor = DialogPrimary
    )

@Composable
private fun dialogTextFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = DialogTextPrimary,
        unfocusedTextColor = DialogTextPrimary,
        cursorColor = DialogPrimary,
        focusedBorderColor = DialogPrimary,
        unfocusedBorderColor = DialogSurfaceLight,
        focusedContainerColor = DialogBackground,
        unfocusedContainerColor = DialogBackground,
        focusedPlaceholderColor = DialogTextSecondary,
        unfocusedPlaceholderColor = DialogTextSecondary,
        focusedSupportingTextColor = DialogTextSecondary,
        unfocusedSupportingTextColor = DialogTextSecondary
    )

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
