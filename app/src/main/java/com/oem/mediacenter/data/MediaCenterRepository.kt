package com.oem.mediacenter.data

import com.oem.mediacenter.discovery.SourceDiscovery
import com.oem.mediacenter.session.ActiveSessionManager
import kotlinx.coroutines.flow.StateFlow

class MediaCenterRepository(
    private val sourceDiscovery: SourceDiscovery,
    val sessionManager: ActiveSessionManager,
) {
    val connectionState: StateFlow<ConnectionState> = sessionManager.connectionState
    val nowPlaying: StateFlow<NowPlayingState> = sessionManager.nowPlaying

    fun discoverSources(): List<MediaSource> = sourceDiscovery.discover()

    fun connect(source: MediaSource) = sessionManager.connect(source)

    fun disconnect() = sessionManager.disconnect()

    suspend fun loadLibraryRoot() = sessionManager.loadLibraryRoot()

    suspend fun loadChildren(parentId: String) = sessionManager.loadChildren(parentId)

    fun playItem(node: BrowseNode) = sessionManager.playItem(node)

    fun togglePlayPause() = sessionManager.togglePlayPause()

    fun skipNext() = sessionManager.skipNext()

    fun skipPrevious() = sessionManager.skipPrevious()

    fun seekTo(positionMs: Long) = sessionManager.seekTo(positionMs)
}
