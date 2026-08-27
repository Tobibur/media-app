package com.oem.mediacenter.di

import android.content.Context
import com.oem.mediacenter.data.MediaCenterRepository
import com.oem.mediacenter.discovery.SourceDiscovery
import com.oem.mediacenter.session.ActiveSessionManager
import com.oem.mediacenter.session.CarMediaSourceSync

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val sourceDiscovery = SourceDiscovery(appContext)
    val sessionManager = ActiveSessionManager(
        context = appContext,
        carMediaSync = CarMediaSourceSync(appContext),
    )
    val repository = MediaCenterRepository(
        sourceDiscovery = sourceDiscovery,
        sessionManager = sessionManager,
    )
}
