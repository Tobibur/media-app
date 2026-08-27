package com.oem.medialib

import android.content.Context
import com.oem.medialib.internal.ActiveSessionManager
import com.oem.medialib.internal.CarMediaSourceSync
import com.oem.medialib.internal.SourceDiscovery

/**
 * Headless Media3 client. No UI, no ExoPlayer — this is a remote control.
 *
 * The consuming app owns screens. This library discovers sources, browses
 * their catalogs, and sends play/pause/next/seek to the source app that
 * actually decodes audio.
 *
 * ```
 * val hub = MediaHub.create(context)
 * val sources = hub.sources.list()
 * hub.session.connect(sources.first())
 * val root = hub.library.root().getOrThrow()
 * val tracks = hub.library.children(root.mediaId).getOrThrow()
 * hub.playback.play(tracks.first { it.isPlayable })
 * hub.playback.togglePlayPause()
 * hub.release()
 * ```
 *
 * Call [release] from `Application.onTerminate` or when the host is done.
 */
class MediaHub internal constructor(
    val sources: SourceCatalog,
    val session: SessionController,
    val library: LibraryBrowser,
    val playback: PlaybackController,
    private val sessionManager: ActiveSessionManager,
) {
    fun release() = sessionManager.release()

    companion object {
        @JvmStatic
        fun create(context: Context): MediaHub {
            val appContext = context.applicationContext
            val discovery = SourceDiscovery(appContext)
            val sessionManager = ActiveSessionManager(
                context = appContext,
                carMediaSync = CarMediaSourceSync(appContext),
            )
            return MediaHub(
                sources = SourceCatalog(discovery),
                session = SessionController(sessionManager),
                library = LibraryBrowser(sessionManager),
                playback = PlaybackController(sessionManager),
                sessionManager = sessionManager,
            )
        }
    }
}
