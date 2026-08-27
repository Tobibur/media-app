package com.oem.samplemedia

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Tiny MediaLibraryService used to smoke-test the OEM Media Center on an Automotive emulator.
 */
class SampleLibraryService : MediaLibraryService() {
    private var player: ExoPlayer? = null
    private var session: MediaLibrarySession? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        val exo = ExoPlayer.Builder(this).build()
        player = exo
        session = MediaLibrarySession.Builder(this, exo, Callback()).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return session
    }

    override fun onDestroy() {
        session?.release()
        player?.release()
        session = null
        player = null
        super.onDestroy()
    }

    private inner class Callback : MediaLibrarySession.Callback {
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            return Futures.immediateFuture(LibraryResult.ofItem(ROOT, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val children = when (parentId) {
                ROOT_ID -> ImmutableList.of(FOLDER_TRACKS)
                FOLDER_ID -> ImmutableList.copyOf(TRACKS)
                else -> ImmutableList.of()
            }
            return Futures.immediateFuture(LibraryResult.ofItemList(children, params))
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val item = TRACKS.firstOrNull { it.mediaId == mediaId }
                ?: return Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
            return Futures.immediateFuture(LibraryResult.ofItem(item, null))
        }

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val resolved = mediaItems.map { requested ->
                TRACKS.firstOrNull { it.mediaId == requested.mediaId } ?: requested
            }
            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(resolved, startIndex, startPositionMs),
            )
        }
    }

    companion object {
        private const val ROOT_ID = "root"
        private const val FOLDER_ID = "folder_tracks"

        private val ROOT = MediaItem.Builder()
            .setMediaId(ROOT_ID)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Sample Library")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build(),
            )
            .build()

        private val FOLDER_TRACKS = MediaItem.Builder()
            .setMediaId(FOLDER_ID)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Demo Tracks")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                    .build(),
            )
            .build()

        // Public domain / test streams commonly used in Android media samples.
        private val TRACKS = listOf(
            track(
                id = "track_1",
                title = "Test Stream 1",
                artist = "Sample",
                uri = "https://storage.googleapis.com/exoplayer-test-media-1/mp3/android-dev.mp3",
            ),
            track(
                id = "track_2",
                title = "Test Stream 2",
                artist = "Sample",
                uri = "https://storage.googleapis.com/exoplayer-test-media-1/mp3/frame-counter-onehour.mp3",
            ),
        )

        private fun track(id: String, title: String, artist: String, uri: String): MediaItem {
            return MediaItem.Builder()
                .setMediaId(id)
                .setUri(uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .setArtist(artist)
                        .setIsPlayable(true)
                        .setIsBrowsable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                        .build(),
                )
                .build()
        }
    }
}
