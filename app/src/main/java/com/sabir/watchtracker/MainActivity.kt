package com.sabir.watchtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sabir.watchtracker.data.local.LibraryItem
import com.sabir.watchtracker.ui.library.HomeScreen
import com.sabir.watchtracker.ui.library.LibraryItemDetailScreen
import com.sabir.watchtracker.ui.library.LibraryScreen
import com.sabir.watchtracker.ui.library.LibraryViewModel
import com.sabir.watchtracker.ui.library.ListsScreen
import com.sabir.watchtracker.ui.library.StatisticsScreen
import com.sabir.watchtracker.ui.search.SearchScreen

private val AppBackground = Color(0xFF090B10)
private val AppSurface = Color(0xFF12151D)
private val AppPrimary = Color(0xFFE63946)
private val AppTextPrimary = Color(0xFFF5F5F7)
private val AppTextSecondary = Color(0xFF9A9DA8)

private val WatchTrackerColors = darkColorScheme(
    primary = AppPrimary,
    onPrimary = Color.White,
    background = AppBackground,
    onBackground = AppTextPrimary,
    surface = AppSurface,
    onSurface = AppTextPrimary,
    onSurfaceVariant = AppTextSecondary
)

private data class NavigationItem(
    val label: String,
    val symbol: String
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WatchTrackerApp()
        }
    }
}

@Composable
private fun WatchTrackerApp(
    libraryViewModel: LibraryViewModel = viewModel()
) {
    val libraryUiState by libraryViewModel.uiState
    val backupUiState by libraryViewModel.backupUiState
    val upNextUiState by libraryViewModel.upNextUiState

    var selectedTab by remember {
        mutableIntStateOf(0)
    }

    var searchIsOpen by remember {
        mutableStateOf(false)
    }

    var selectedLibraryItem by remember {
        mutableStateOf<LibraryItem?>(null)
    }

    BackHandler(
        enabled = selectedLibraryItem != null ||
            searchIsOpen ||
            selectedTab != 0
    ) {
        when {
            selectedLibraryItem != null -> {
                selectedLibraryItem = null
            }

            searchIsOpen -> {
                searchIsOpen = false
            }

            selectedTab != 0 -> {
                selectedTab = 0
            }
        }
    }

    val navigationItems = listOf(
        NavigationItem("Home", "⌂"),
        NavigationItem("Movies", "▶"),
        NavigationItem("TV Shows", "▣"),
        NavigationItem("Lists", "☷"),
        NavigationItem("Stats", "◉")
    )

    MaterialTheme(
        colorScheme = WatchTrackerColors
    ) {
        if (selectedLibraryItem != null) {
            LibraryItemDetailScreen(
                item = selectedLibraryItem!!,
                onBackClick = {
                    selectedLibraryItem = null
                },
                onDelete = {
                    selectedLibraryItem?.let(libraryViewModel::deleteItem)
                    selectedLibraryItem = null
                }
            )
        } else if (searchIsOpen) {
            SearchScreen(
                onBackClick = {
                    searchIsOpen = false
                }
            )
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = AppBackground,
                bottomBar = {
                    NavigationBar(
                        containerColor = AppSurface,
                        tonalElevation = 0.dp
                    ) {
                        navigationItems.forEachIndexed { index, item ->
                            NavigationBarItem(
                                selected = selectedTab == index,
                                onClick = {
                                    selectedTab = index
                                },
                                icon = {
                                    Text(
                                        text = item.symbol,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        fontSize = 11.sp
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AppPrimary,
                                    selectedTextColor = AppPrimary,
                                    indicatorColor = AppPrimary.copy(
                                        alpha = 0.14f
                                    ),
                                    unselectedIconColor =
                                        AppTextSecondary,
                                    unselectedTextColor =
                                        AppTextSecondary
                                )
                            )
                        }
                    }
                }
            ) { innerPadding ->
                when (selectedTab) {
                    0 -> {
                        HomeScreen(
                            paddingValues = innerPadding,
                            libraryUiState = libraryUiState,
                            upNextUiState = upNextUiState,
                            onSearchClick = {
                                searchIsOpen = true
                            },
                            onMarkUpNextWatched =
                                libraryViewModel::markUpNextWatched,
                            onRetryUpNext =
                                libraryViewModel::retryUpNext,
                            onItemClick = { item ->
                                selectedLibraryItem = item
                            }
                        )
                    }

                    1 -> {
                        LibraryScreen(
                            paddingValues = innerPadding,
                            title = "Movies",
                            items = libraryUiState.movies,
                            isLoading = libraryUiState.isLoading,
                            onBackClick = { selectedTab = 0 },
                            onAddClick = {
                                searchIsOpen = true
                            },
                            onItemClick = { item ->
                                selectedLibraryItem = item
                            }
                        )
                    }

                    2 -> {
                        LibraryScreen(
                            paddingValues = innerPadding,
                            title = "TV Shows",
                            items = libraryUiState.tvShows,
                            isLoading = libraryUiState.isLoading,
                            onBackClick = { selectedTab = 0 },
                            onAddClick = {
                                searchIsOpen = true
                            },
                            onItemClick = { item ->
                                selectedLibraryItem = item
                            }
                        )
                    }

                    3 -> {
                        ListsScreen(
                            paddingValues = innerPadding,
                            state = libraryUiState,
                            onBackClick = { selectedTab = 0 },
                            onCreateList = libraryViewModel::createCustomList,
                            onUpdateList = libraryViewModel::updateCustomList,
                            onDeleteList = libraryViewModel::deleteCustomList,
                            onAddItem = libraryViewModel::addToCustomList,
                            onRemoveItem = libraryViewModel::removeFromCustomList,
                            onItemClick = { item -> selectedLibraryItem = item }
                        )
                    }

                    else -> {
                        StatisticsScreen(
                            paddingValues = innerPadding,
                            libraryUiState = libraryUiState,
                            backupUiState = backupUiState,
                            onExportBackup = libraryViewModel::exportBackup,
                            onInspectBackup = libraryViewModel::inspectBackup,
                            onRestoreBackup = libraryViewModel::restoreBackup,
                            onDismissBackupPreview =
                                libraryViewModel::dismissBackupPreview,
                            onClearBackupMessage =
                                libraryViewModel::clearBackupMessage,
                            onBackClick = { selectedTab = 0 }
                        )
                    }
                }
            }
        }
    }
}

