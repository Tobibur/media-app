package com.oem.medialib

import android.net.Uri
import androidx.media3.session.SessionToken

/**
 * A discovered media app that can be connected as a source.
 *
 * Constructed only by the library. [token] stays internal so consumers never
 * touch Media3 types.
 */
class MediaSource internal constructor(
    val packageName: String,
    val serviceName: String,
    val label: CharSequence,
    internal val token: SessionToken,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MediaSource) return false
        return packageName == other.packageName && serviceName == other.serviceName
    }

    override fun hashCode(): Int = 31 * packageName.hashCode() + serviceName.hashCode()

    override fun toString(): String = "MediaSource($packageName/$serviceName)"
}

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
