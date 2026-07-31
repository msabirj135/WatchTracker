package com.sabir.watchtracker.ui.library

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.sabir.watchtracker.data.local.LibraryItem
import com.sabir.watchtracker.data.local.LibraryStatus
import com.sabir.watchtracker.data.repository.LibraryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class LibraryUiState(
    val isLoading: Boolean = true,
    val items: List<LibraryItem> = emptyList(),
    val errorMessage: String? = null
) {
    val movies: List<LibraryItem>
        get() = items.filter { item ->
            item.mediaType == "movie"
        }

    val tvShows: List<LibraryItem>
        get() = items.filter { item ->
            item.mediaType == "tv"
        }

    val planToWatch: List<LibraryItem>
        get() = items.filter { item ->
            item.status == LibraryStatus.PLAN_TO_WATCH
        }

    val completed: List<LibraryItem>
        get() = items.filter { item ->
            item.status == LibraryStatus.COMPLETED
        }

    val dropped: List<LibraryItem>
        get() = items.filter { item ->
            item.status == LibraryStatus.DROPPED
        }

    val watchHistory: List<LibraryItem>
        get() = completed.sortedWith(
            compareByDescending<LibraryItem> { item ->
                item.watchDateEpochDay ?: Long.MIN_VALUE
            }.thenByDescending { item ->
                item.updatedAt
            }
        )

    val recentlyAdded: List<LibraryItem>
        get() = items.sortedByDescending { item ->
            item.addedAt
        }

    val movieCount: Int
        get() = movies.size

    val tvShowCount: Int
        get() = tvShows.size

    val completedCount: Int
        get() = completed.size

    val planToWatchCount: Int
        get() = planToWatch.size

    val droppedCount: Int
        get() = dropped.size

    val totalCount: Int
        get() = items.size
}

class LibraryViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = LibraryRepository(
        context = application.applicationContext
    )

    private val coroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    )

    var uiState = mutableStateOf(
        LibraryUiState()
    )
        private set

    init {
        observeLibrary()
    }

    private fun observeLibrary() {
        coroutineScope.launch {
            repository
                .observeAll()
                .catch { exception ->
                    uiState.value = LibraryUiState(
                        isLoading = false,
                        items = emptyList(),
                        errorMessage = exception.message
                            ?: "Unable to load your library."
                    )
                }
                .collect { items ->
                    uiState.value = LibraryUiState(
                        isLoading = false,
                        items = items,
                        errorMessage = null
                    )
                }
        }
    }

    fun deleteItem(
        item: LibraryItem
    ) {
        coroutineScope.launch {
            try {
                repository.deleteItem(item)
            } catch (exception: Exception) {
                uiState.value = uiState.value.copy(
                    errorMessage = exception.message
                        ?: "Unable to delete this title."
                )
            }
        }
    }

    fun clearError() {
        uiState.value = uiState.value.copy(
            errorMessage = null
        )
    }

    override fun onCleared() {
        coroutineScope.cancel()
        super.onCleared()
    }
}