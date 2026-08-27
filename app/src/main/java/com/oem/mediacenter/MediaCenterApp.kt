package com.oem.mediacenter

import android.app.Application
import com.oem.mediacenter.di.AppContainer

class MediaCenterApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    override fun onTerminate() {
        container.mediaHub.release()
        super.onTerminate()
    }
}
