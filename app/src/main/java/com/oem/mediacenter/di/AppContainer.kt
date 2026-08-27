package com.oem.mediacenter.di

import android.content.Context
import com.oem.medialib.MediaHub

class AppContainer(context: Context) {
    val mediaHub: MediaHub = MediaHub.create(context)
}
