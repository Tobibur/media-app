package com.oem.mediacenter.data

import android.net.Uri
import androidx.media3.session.SessionToken

data class MediaSource(
    val packageName: String,
    val serviceName: String,
    val label: CharSequence,
    val token: SessionToken,
)

data class BrowseNode(
    val mediaId: String,
    val title: String,
    val subtitle: String?,
    val isBrowsable: Boolean,
    val isPlayable: Boolean,
    val artworkUri: Uri?,
)

data class NowPlayingState(
    val title: String? = null,
    val subtitle: String? = null,
    val artworkUri: Uri? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val canPlayPause: Boolean = false,
    val canSkipNext: Boolean = false,
    val canSkipPrevious: Boolean = false,
    val canSeek: Boolean = false,
) {
    companion object {
        val Empty = NowPlayingState()
    }
}

sealed interface ConnectionState {
    data object Idle : ConnectionState
    data object Connecting : ConnectionState
    data class Connected(val packageName: String) : ConnectionState
    data class Failed(val message: String) : ConnectionState
    data object Disconnected : ConnectionState
}
