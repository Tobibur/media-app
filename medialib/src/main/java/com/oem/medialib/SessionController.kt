package com.oem.medialib

import com.oem.medialib.internal.ActiveSessionManager
import kotlinx.coroutines.flow.StateFlow

/**
 * Owns the single active connection to a media source.
 *
 * Connect → browse/play through the other [MediaHub] APIs → [disconnect]
 * or connect a different source. Only one source is bound at a time.
 */
class SessionController internal constructor(
    private val sessionManager: ActiveSessionManager,
) {
    val state: StateFlow<ConnectionState> = sessionManager.connectionState

    fun connect(source: MediaSource) = sessionManager.connect(source)

    fun disconnect() = sessionManager.disconnect()
}
