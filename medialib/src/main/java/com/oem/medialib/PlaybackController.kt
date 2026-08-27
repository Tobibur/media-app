package com.oem.medialib

import com.oem.medialib.internal.ActiveSessionManager
import kotlinx.coroutines.flow.StateFlow

/**
 * Transport controls and now-playing state for the connected source.
 *
 * Buttons should be gated on [NowPlayingState.canPlayPause],
 * [NowPlayingState.canSkipNext], [NowPlayingState.canSkipPrevious],
 * and [NowPlayingState.canSeek] rather than assumed always available.
 */
class PlaybackController internal constructor(
    private val sessionManager: ActiveSessionManager,
) {
    val nowPlaying: StateFlow<NowPlayingState> = sessionManager.nowPlaying

    fun play(item: BrowseNode) = sessionManager.playItem(item)

    fun play() = sessionManager.play()

    fun pause() = sessionManager.pause()

    fun togglePlayPause() = sessionManager.togglePlayPause()

    fun skipNext() = sessionManager.skipNext()

    fun skipPrevious() = sessionManager.skipPrevious()

    fun seekTo(positionMs: Long) = sessionManager.seekTo(positionMs)
}
