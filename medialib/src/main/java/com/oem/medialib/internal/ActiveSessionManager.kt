package com.oem.medialib.internal

import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaLibraryService
import com.google.common.util.concurrent.ListenableFuture
import com.oem.medialib.BrowseNode
import com.oem.medialib.ConnectionState
import com.oem.medialib.MediaSource
import com.oem.medialib.NowPlayingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutionException

/**
 * Owns the single active MediaBrowser connection to a media source.
 */
internal class ActiveSessionManager(
    private val context: Context,
    private val carMediaSync: CarMediaSourceSync = CarMediaSourceSync(context),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _browser = MutableStateFlow<MediaBrowser?>(null)

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _nowPlaying = MutableStateFlow(NowPlayingState.Empty)
    val nowPlaying: StateFlow<NowPlayingState> = _nowPlaying.asStateFlow()

    private var connectJob: Job? = null
    private var positionJob: Job? = null
    private var browserFuture: ListenableFuture<MediaBrowser>? = null
    private var playerListener: Player.Listener? = null

    fun connect(source: MediaSource) {
        connectJob?.cancel()
        connectJob = scope.launch {
            releaseInternal()
            _connectionState.value = ConnectionState.Connecting
            try {
                val future = MediaBrowser.Builder(context, source.token)
                    .setListener(object : MediaBrowser.Listener {})
                    .buildAsync()
                browserFuture = future
                val browser = withContext(Dispatchers.IO) { future.await() }
                if (!isActive) {
                    browser.release()
                    return@launch
                }
                attachPlayerListener(browser)
                _browser.value = browser
                _connectionState.value = ConnectionState.Connected(source.packageName)
                carMediaSync.setMediaSource(
                    ComponentName(source.packageName, source.serviceName),
                )
                startPositionTicker(browser)
                refreshNowPlaying(browser)
            } catch (e: Exception) {
                val message = when (e) {
                    is ExecutionException -> e.cause?.message ?: e.message
                    else -> e.message
                } ?: "Connection failed"
                _connectionState.value = ConnectionState.Failed(message)
                _browser.value = null
            }
        }
    }

    fun disconnect() {
        connectJob?.cancel()
        scope.launch { releaseInternal() }
        _connectionState.value = ConnectionState.Idle
    }

    suspend fun loadChildren(parentId: String): Result<List<BrowseNode>> {
        val browser = _browser.value
            ?: return Result.failure(IllegalStateException("Not connected"))
        return try {
            val result = browser.getChildren(
                parentId,
                /* page = */ 0,
                BrowsePaging.PAGE_SIZE,
                /* params = */ null as MediaLibraryService.LibraryParams?,
            ).await()
            if (result.resultCode != 0) {
                Result.failure(IllegalStateException("Browse failed: ${result.resultCode}"))
            } else {
                val items = result.value.orEmpty().map(BrowsePaging::toBrowseNode)
                Result.success(items)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadLibraryRoot(): Result<BrowseNode> {
        val browser = _browser.value
            ?: return Result.failure(IllegalStateException("Not connected"))
        return try {
            val result = browser.getLibraryRoot(/* params = */ null).await()
            if (result.resultCode != 0 || result.value == null) {
                Result.failure(IllegalStateException("Root failed: ${result.resultCode}"))
            } else {
                Result.success(BrowsePaging.toBrowseNode(result.value!!))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun playItem(node: BrowseNode) {
        val browser = _browser.value ?: return
        val item = MediaItem.Builder().setMediaId(node.mediaId).build()
        browser.setMediaItem(item)
        browser.prepare()
        browser.play()
    }

    fun play() {
        _browser.value?.play()
    }

    fun pause() {
        _browser.value?.pause()
    }

    fun togglePlayPause() {
        val browser = _browser.value ?: return
        if (browser.isPlaying) browser.pause() else browser.play()
    }

    fun skipNext() {
        _browser.value?.seekToNextMediaItem()
    }

    fun skipPrevious() {
        _browser.value?.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) {
        _browser.value?.seekTo(positionMs)
    }

    private fun attachPlayerListener(browser: MediaBrowser) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                refreshNowPlaying(browser)
                if (events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) &&
                    player.playbackState == Player.STATE_IDLE &&
                    !player.playWhenReady
                ) {
                    // no-op; disconnect is handled via session listener if needed
                }
            }
        }
        playerListener = listener
        browser.addListener(listener)
    }

    private fun refreshNowPlaying(browser: MediaBrowser) {
        val meta = browser.mediaMetadata
        val commands = browser.availableCommands
        val flags = BrowsePaging.controlFlags(
            canPlayPause = commands.contains(Player.COMMAND_PLAY_PAUSE),
            canSeekNext = commands.contains(Player.COMMAND_SEEK_TO_NEXT),
            canSeekNextMedia = commands.contains(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM),
            canSeekPrevious = commands.contains(Player.COMMAND_SEEK_TO_PREVIOUS),
            canSeekPreviousMedia = commands.contains(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM),
            canSeekInCurrent = commands.contains(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM),
        )
        _nowPlaying.value = NowPlayingState(
            title = meta.title?.toString() ?: meta.displayTitle?.toString(),
            subtitle = meta.artist?.toString() ?: meta.subtitle?.toString(),
            artworkUri = meta.artworkUri,
            isPlaying = browser.isPlaying,
            positionMs = browser.currentPosition.coerceAtLeast(0L),
            durationMs = browser.duration.takeIf { it > 0 } ?: 0L,
            canPlayPause = flags.canPlayPause,
            canSkipNext = flags.canSkipNext,
            canSkipPrevious = flags.canSkipPrevious,
            canSeek = flags.canSeek,
        )
    }

    private fun startPositionTicker(browser: MediaBrowser) {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (isActive) {
                if (browser.isConnected) {
                    refreshNowPlaying(browser)
                } else {
                    _connectionState.value = ConnectionState.Disconnected
                    _nowPlaying.value = NowPlayingState.Empty
                    break
                }
                delay(500L)
            }
        }
    }

    private suspend fun releaseInternal() {
        positionJob?.cancel()
        positionJob = null
        val current = _browser.value
        val listener = playerListener
        if (current != null && listener != null) {
            current.removeListener(listener)
        }
        playerListener = null
        _browser.value = null
        _nowPlaying.value = NowPlayingState.Empty
        browserFuture?.let { future ->
            MediaBrowser.releaseFuture(future)
        }
        browserFuture = null
        current?.release()
    }

    fun release() {
        connectJob?.cancel()
        scope.launch {
            releaseInternal()
            _connectionState.value = ConnectionState.Idle
        }
        mainHandler.post {
            _browser.value?.release()
        }
    }
}
