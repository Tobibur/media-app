package com.oem.mediacenter.session

import android.content.ComponentName
import android.content.Context
import android.util.Log

/**
 * Syncs the selected media source with AAOS CarMediaManager when the Car API is present.
 * Uses reflection so the app still builds/runs when android.car is unavailable at runtime.
 */
class CarMediaSourceSync(private val context: Context) {
    fun setMediaSource(componentName: ComponentName) {
        try {
            val carClass = Class.forName("android.car.Car")
            val createCar = carClass.getMethod(
                "createCar",
                Context::class.java,
            )
            val car = createCar.invoke(null, context) ?: return
            val getCarManager = carClass.getMethod("getCarManager", String::class.java)
            val mediaServiceName = carClass.getField("CAR_MEDIA_SERVICE").get(null) as String
            val mediaManager = getCarManager.invoke(car, mediaServiceName) ?: return
            val setMediaSource = mediaManager.javaClass.getMethod(
                "setMediaSource",
                ComponentName::class.java,
                Int::class.javaPrimitiveType,
            )
            // MEDIA_SOURCE_MODE_PLAYBACK == 0 on AAOS
            setMediaSource.invoke(mediaManager, componentName, 0)
        } catch (t: Throwable) {
            Log.d(TAG, "CarMediaManager sync skipped: ${t.message}")
        }
    }

    companion object {
        private const val TAG = "CarMediaSourceSync"
    }
}
