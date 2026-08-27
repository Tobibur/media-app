package com.oem.medialib

import com.oem.medialib.internal.SourceDiscovery

/**
 * Lists installed Media3 library / session services.
 *
 * Call [list] whenever you want a fresh snapshot. Discovery is not a live
 * listener; re-run after an app install if you need the new source.
 */
class SourceCatalog internal constructor(
    private val listSources: () -> List<MediaSource>,
) {
    internal constructor(discovery: SourceDiscovery) : this(discovery::discover)

    fun list(): List<MediaSource> = listSources()
}
