package com.sabir.watchtracker.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.sabir.watchtracker.data.remote.TmdbSearchResult

private val SearchBackground = Color(0xFF090B10)
private val SearchSurface = Color(0xFF12151D)
private val SearchSurfaceLight = Color(0xFF1A1E28)
private val SearchPrimary = Color(0xFFE63946)
private val SearchSuccess = Color(0xFF36C98F)
private val SearchTextPrimary = Color(0xFFF5F5F7)
private val SearchTextSecondary = Color(0xFF9A9DA8)

@Composable
fun SearchScreen(
    onBackClick: () -> Unit,
    searchViewModel: SearchViewModel = viewModel()
) {
    val uiState by searchViewModel.uiState
    val keyboardController = LocalSoftwareKeyboardController.current

    var selectedResult by remember {
        mutableStateOf<TmdbSearchResult?>(null)
    }

    LaunchedEffect(uiState.lastSavedItemKey) {
        if (uiState.lastSavedItemKey != null) {
            selectedResult = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SearchBackground)
    ) {
        SearchHeader(
            onBackClick = onBackClick
        )

        SearchInput(
            query = uiState.query,
            onQueryChange = searchViewModel::updateQuery,
            onSearch = {
                keyboardController?.hide()
                searchViewModel.search()
            },
            onClear = searchViewModel::clearSearch
        )

        SearchFilters(
            selectedFilter = uiState.mediaFilter,
            selectedYear = uiState.yearFilter,
            selectedLanguage = uiState.languageFilter,
            availableYears = uiState.availableYears,
            availableLanguages = uiState.availableLanguages,
            onFilterSelected = searchViewModel::updateMediaFilter,
            onYearSelected = searchViewModel::updateYearFilter,
            onLanguageSelected = searchViewModel::updateLanguageFilter,
            onClearFilters = searchViewModel::clearResultFilters
        )

        SaveFeedback(
            successMessage = uiState.saveMessage,
            errorMessage = uiState.saveErrorMessage,
            onDismiss = searchViewModel::clearSaveFeedback
        )

        when {
            uiState.isLoading -> {
                LoadingContent()
            }

            uiState.errorMessage != null -> {
                MessageContent(
                    symbol = "!",
                    title = "Search failed",
                    message = uiState.errorMessage,
                    actionLabel = "Retry",
                    onAction = searchViewModel::search
                )
            }

            uiState.hasSearched && uiState.results.isEmpty() -> {
                MessageContent(
                    symbol = "⌕",
                    title = "No results found",
                    message = "Try another movie or TV-show title."
                )
            }

            uiState.results.isNotEmpty() -> {
                if (uiState.filteredResults.isEmpty()) {
                    MessageContent(
                        symbol = "⌕",
                        title = "No matching results",
                        message = "Change or reset the filters to see more titles.",
                        actionLabel = "Reset filters",
                        onAction = searchViewModel::clearResultFilters
                    )
                } else {
                    SearchResults(
                        results = uiState.filteredResults,
                        savedItemKeys = uiState.savedItemKeys,
                        onResultClick = { result ->
                            searchViewModel.clearSaveFeedback()
                            selectedResult = result
                            searchViewModel.prepareResult(result)
                        }
                    )
                }
            }

            else -> {
                SearchLanding(
                    recentSearches = uiState.recentSearches,
                    onRecentSearch = searchViewModel::searchRecent,
                    onClearRecent = searchViewModel::clearRecentSearches
                )
            }
        }
    }

    selectedResult?.let { result ->
        AddToLibraryDialog(
            result = result,
            isSaving = uiState.isSaving,
            errorMessage = uiState.saveErrorMessage,
            tvDetails = uiState.tvDetails,
            seasonDetails = uiState.seasonDetails,
            isLoadingTvDetails = uiState.isLoadingTvDetails,
            isLoadingSeason = uiState.isLoadingSeason,
            tvDetailsErrorMessage = uiState.tvDetailsErrorMessage,
            onSeasonSelected = { seasonNumber ->
                searchViewModel.loadSeason(
                    seriesId = result.id,
                    seasonNumber = seasonNumber
                )
            },
            onDismiss = {
                if (!uiState.isSaving) {
                    selectedResult = null
                    searchViewModel.clearSaveFeedback()
                }
            },
            onSave = {
                    status,
                    watchDateEpochDay,
                    personalRating,
                    watchMethod,
                    selectedEpisode ->

                searchViewModel.saveToLibrary(
                    result = result,
                    status = status,
                    watchDateEpochDay = watchDateEpochDay,
                    personalRating = personalRating,
                    watchMethod = watchMethod,
                    selectedEpisode = selectedEpisode
                )
            }
        )
    }
}

@Composable
private fun SearchFilters(
    selectedFilter: SearchMediaFilter,
    selectedYear: Int?,
    selectedLanguage: String?,
    availableYears: List<Int>,
    availableLanguages: List<Pair<String, String>>,
    onFilterSelected: (SearchMediaFilter) -> Unit,
    onYearSelected: (Int?) -> Unit,
    onLanguageSelected: (String?) -> Unit,
    onClearFilters: () -> Unit
) {
    Column(modifier = Modifier.padding(top = 10.dp)) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 20.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(SearchMediaFilter.entries) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onFilterSelected(filter) },
                    label = {
                        Text(
                            text = filter.label,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                )
            }

            if (
                selectedFilter != SearchMediaFilter.ALL ||
                selectedYear != null ||
                selectedLanguage != null
            ) {
                item {
                    TextButton(onClick = onClearFilters) {
                        Text(
                            text = "Reset",
                            color = SearchPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (availableYears.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 20.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedYear == null,
                        onClick = { onYearSelected(null) },
                        label = { Text("Any year") }
                    )
                }

                items(availableYears) { year ->
                    FilterChip(
                        selected = selectedYear == year,
                        onClick = { onYearSelected(year) },
                        label = { Text(year.toString()) }
                    )
                }
            }
        }

        if (availableLanguages.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 20.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedLanguage == null,
                        onClick = { onLanguageSelected(null) },
                        label = { Text("Any language") }
                    )
                }

                items(
                    items = availableLanguages,
                    key = { (code, _) -> code }
                ) { (code, name) ->
                    FilterChip(
                        selected = selectedLanguage == code,
                        onClick = { onLanguageSelected(code) },
                        label = { Text(name) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchLanding(
    recentSearches: List<String>,
    onRecentSearch: (String) -> Unit,
    onClearRecent: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 18.dp)
    ) {
        if (recentSearches.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent searches",
                    modifier = Modifier.weight(1f),
                    color = SearchTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                TextButton(onClick = onClearRecent) {
                    Text(
                        text = "Clear",
                        color = SearchPrimary,
                        fontSize = 12.sp
                    )
                }
            }

            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 20.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recentSearches) { query ->
                    FilterChip(
                        selected = false,
                        onClick = { onRecentSearch(query) },
                        label = { Text(query) }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "⌕",
                    color = SearchPrimary,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Search TMDB",
                    color = SearchTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Find movies and TV shows for your library.",
                    color = SearchTextSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun SearchHeader(
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 8.dp,
                top = 20.dp,
                end = 20.dp,
                bottom = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onBackClick
        ) {
            Text(
                text = "←",
                color = SearchTextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "Search",
            color = SearchTextPrimary,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SearchInput(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "Search movies and TV shows",
                    color = SearchTextSecondary
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = SearchTextPrimary,
                unfocusedTextColor = SearchTextPrimary,
                cursorColor = SearchPrimary,
                focusedBorderColor = SearchPrimary,
                unfocusedBorderColor = SearchSurfaceLight,
                focusedContainerColor = SearchSurface,
                unfocusedContainerColor = SearchSurface
            ),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onSearch()
                }
            ),
            trailingIcon = {
                if (query.isNotEmpty()) {
                    TextButton(
                        onClick = onClear
                    ) {
                        Text(
                            text = "×",
                            color = SearchTextSecondary,
                            fontSize = 25.sp
                        )
                    }
                }
            }
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Button(
            onClick = onSearch,
            modifier = Modifier.fillMaxWidth(),
            enabled = query.trim().length >= 2,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SearchPrimary,
                contentColor = Color.White,
                disabledContainerColor = SearchSurfaceLight,
                disabledContentColor = SearchTextSecondary
            )
        ) {
            Text(
                text = "Search",
                modifier = Modifier.padding(vertical = 5.dp),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SaveFeedback(
    successMessage: String?,
    errorMessage: String?,
    onDismiss: () -> Unit
) {
    val message = successMessage ?: errorMessage ?: return

    val messageColor = if (successMessage != null) {
        SearchSuccess
    } else {
        SearchPrimary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 20.dp,
                top = 14.dp,
                end = 20.dp
            )
            .background(
                color = messageColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(
                start = 14.dp,
                top = 10.dp,
                end = 6.dp,
                bottom = 10.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            color = messageColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        TextButton(
            onClick = onDismiss
        ) {
            Text(
                text = "×",
                color = messageColor,
                fontSize = 20.sp
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = SearchPrimary
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Text(
                text = "Searching TMDB...",
                color = SearchTextSecondary,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun MessageContent(
    symbol: String,
    title: String,
    message: String?,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(
                        color = SearchPrimary.copy(
                            alpha = 0.12f
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = symbol,
                    color = SearchPrimary,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.height(22.dp)
            )

            Text(
                text = title,
                color = SearchTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = message.orEmpty(),
                color = SearchTextSecondary,
                fontSize = 14.sp
            )

            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SearchPrimary
                    )
                ) {
                    Text(
                        text = actionLabel,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResults(
    results: List<TmdbSearchResult>,
    savedItemKeys: Set<String>,
    onResultClick: (TmdbSearchResult) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "${results.size} results",
                modifier = Modifier.padding(
                    start = 20.dp,
                    end = 20.dp,
                    bottom = 2.dp
                ),
                color = SearchTextSecondary,
                fontSize = 13.sp
            )
        }

        items(
            items = results,
            key = { result ->
                "${result.mediaType}-${result.id}"
            }
        ) { result ->
            SearchResultCard(
                result = result,
                isSaved = "${result.mediaType}-${result.id}" in
                    savedItemKeys,
                onClick = {
                    onResultClick(result)
                },
                modifier = Modifier.padding(
                    horizontal = 20.dp
                )
            )
        }

        item {
            Spacer(
                modifier = Modifier.height(24.dp)
            )
        }
    }
}

@Composable
private fun SearchResultCard(
    result: TmdbSearchResult,
    isSaved: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = SearchSurface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            PosterImage(
                result = result
            )

            Spacer(
                modifier = Modifier.width(14.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(164.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = result.displayTitle,
                        color = SearchTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    result.originalTitle?.let { originalTitle ->
                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = originalTitle,
                            color = SearchTextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(7.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MediaTypeBadge(
                            mediaType = result.displayMediaType
                        )

                        Spacer(
                            modifier = Modifier.width(8.dp)
                        )

                        Text(
                            text = "${result.displayYear} • ${result.displayLanguage}",
                            modifier = Modifier.weight(1f),
                            color = SearchTextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (result.voteAverage > 0.0) {
                            Spacer(
                                modifier = Modifier.width(10.dp)
                            )

                            Text(
                                text = "★ %.1f".format(
                                    result.voteAverage
                                ),
                                color = Color(0xFFFFC857),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (isSaved) {
                        Spacer(
                            modifier = Modifier.height(7.dp)
                        )

                        Text(
                            text = "✓ In your library",
                            color = SearchSuccess,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (result.overview.isNotBlank()) {
                        Spacer(
                            modifier = Modifier.height(10.dp)
                        )

                        Text(
                            text = result.overview,
                            color = SearchTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PosterImage(
    result: TmdbSearchResult
) {
    Box(
        modifier = Modifier
            .width(92.dp)
            .height(164.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SearchSurfaceLight),
        contentAlignment = Alignment.Center
    ) {
        if (result.posterUrl != null) {
            AsyncImage(
                model = result.posterUrl,
                contentDescription = "${result.displayTitle} poster",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = result.displayTitle
                    .take(2)
                    .uppercase(),
                color = SearchPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MediaTypeBadge(
    mediaType: String
) {
    Box(
        modifier = Modifier
            .background(
                color = SearchPrimary.copy(alpha = 0.14f),
                shape = RoundedCornerShape(7.dp)
            )
            .padding(
                horizontal = 8.dp,
                vertical = 4.dp
            )
    ) {
        Text(
            text = mediaType,
            color = SearchPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
