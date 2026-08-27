package com.oem.medialib

import com.oem.medialib.internal.ActiveSessionManager

/**
 * Walks the connected source's Media3 library tree.
 *
 * Must be connected ([SessionController.connect]) before calling [root]
 * or [children]. Folders have [BrowseNode.isBrowsable]; tracks have
 * [BrowseNode.isPlayable].
 */
class LibraryBrowser internal constructor(
    private val sessionManager: ActiveSessionManager,
) {
    suspend fun root(): Result<BrowseNode> = sessionManager.loadLibraryRoot()

    suspend fun children(parentId: String): Result<List<BrowseNode>> =
        sessionManager.loadChildren(parentId)
}
