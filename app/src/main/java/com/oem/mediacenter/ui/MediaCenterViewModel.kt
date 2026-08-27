package com.oem.mediacenter.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.oem.mediacenter.data.BrowseNode
import com.oem.mediacenter.data.ConnectionState
import com.oem.mediacenter.data.MediaCenterRepository
import com.oem.mediacenter.data.MediaSource
import com.oem.mediacenter.data.NowPlayingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SourcesUiState(
    val sources: List<MediaSource> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

data class BrowseUiState(
    val title: String = "",
    val nodes: List<BrowseNode> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val pathIds: List<String> = emptyList(),
    val pathTitles: List<String> = emptyList(),
)

class MediaCenterViewModel(
    private val repository: MediaCenterRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _sourcesState = MutableStateFlow(SourcesUiState())
    val sourcesState: StateFlow<SourcesUiState> = _sourcesState.asStateFlow()

    private val _browseState = MutableStateFlow(BrowseUiState())
    val browseState: StateFlow<BrowseUiState> = _browseState.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = repository.connectionState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConnectionState.Idle)

    val nowPlaying: StateFlow<NowPlayingState> = repository.nowPlaying
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NowPlayingState.Empty)

    private var cachedSources: List<MediaSource> = emptyList()

    init {
        refreshSources()
        val lastPackage = savedStateHandle.get<String>(KEY_LAST_PACKAGE)
        if (!lastPackage.isNullOrBlank()) {
            viewModelScope.launch {
                // Wait for first discovery then reconnect
                refreshSourcesBlocking()
                cachedSources.firstOrNull { it.packageName == lastPackage }?.let { connect(it) }
            }
        }
    }

    fun refreshSources() {
        viewModelScope.launch { refreshSourcesBlocking() }
    }

    private fun refreshSourcesBlocking() {
        _sourcesState.value = _sourcesState.value.copy(isLoading = true, error = null)
        try {
            cachedSources = repository.discoverSources()
            _sourcesState.value = SourcesUiState(sources = cachedSources, isLoading = false)
        } catch (e: Exception) {
            _sourcesState.value = SourcesUiState(
                sources = emptyList(),
                isLoading = false,
                error = e.message ?: "Discovery failed",
            )
        }
    }

    fun connect(source: MediaSource) {
        savedStateHandle[KEY_LAST_PACKAGE] = source.packageName
        repository.connect(source)
        openRoot()
    }

    fun openRoot() {
        viewModelScope.launch {
            _browseState.value = BrowseUiState(isLoading = true)
            val rootResult = repository.loadLibraryRoot()
            rootResult.fold(
                onSuccess = { root ->
                    loadChildrenInternal(
                        parentId = root.mediaId,
                        title = root.title,
                        pathIds = listOf(root.mediaId),
                        pathTitles = listOf(root.title),
                    )
                },
                onFailure = { e ->
                    _browseState.value = BrowseUiState(
                        isLoading = false,
                        error = e.message ?: "Failed to load library",
                    )
                },
            )
        }
    }

    fun openFolder(node: BrowseNode) {
        if (!node.isBrowsable) return
        val current = _browseState.value
        viewModelScope.launch {
            loadChildrenInternal(
                parentId = node.mediaId,
                title = node.title,
                pathIds = current.pathIds + node.mediaId,
                pathTitles = current.pathTitles + node.title,
            )
        }
    }

    fun navigateBack(): Boolean {
        val current = _browseState.value
        if (current.pathIds.size <= 1) return false
        val newIds = current.pathIds.dropLast(1)
        val newTitles = current.pathTitles.dropLast(1)
        viewModelScope.launch {
            loadChildrenInternal(
                parentId = newIds.last(),
                title = newTitles.last(),
                pathIds = newIds,
                pathTitles = newTitles,
            )
        }
        return true
    }

    fun play(node: BrowseNode) {
        repository.playItem(node)
    }

    fun togglePlayPause() = repository.togglePlayPause()
    fun skipNext() = repository.skipNext()
    fun skipPrevious() = repository.skipPrevious()
    fun seekTo(positionMs: Long) = repository.seekTo(positionMs)

    private suspend fun loadChildrenInternal(
        parentId: String,
        title: String,
        pathIds: List<String>,
        pathTitles: List<String>,
    ) {
        _browseState.value = _browseState.value.copy(
            title = title,
            isLoading = true,
            error = null,
            pathIds = pathIds,
            pathTitles = pathTitles,
        )
        repository.loadChildren(parentId).fold(
            onSuccess = { nodes ->
                _browseState.value = BrowseUiState(
                    title = title,
                    nodes = nodes,
                    isLoading = false,
                    pathIds = pathIds,
                    pathTitles = pathTitles,
                )
            },
            onFailure = { e ->
                _browseState.value = BrowseUiState(
                    title = title,
                    isLoading = false,
                    error = e.message ?: "Browse failed",
                    pathIds = pathIds,
                    pathTitles = pathTitles,
                )
            },
        )
    }

    companion object {
        private const val KEY_LAST_PACKAGE = "last_package"

        fun factory(repository: MediaCenterRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                    return MediaCenterViewModel(
                        repository = repository,
                        savedStateHandle = extras.createSavedStateHandle(),
                    ) as T
                }
            }
    }
}
