package com.oem.mediacenter.discovery

import android.content.Context
import android.content.pm.PackageManager
import androidx.media3.session.SessionToken
import com.oem.mediacenter.data.MediaSource

/**
 * Discovers Media3 library / session services installed on the device.
 * Filtering is pure so unit tests can inject token lists without PackageManager.
 */
class SourceDiscovery(
    private val context: Context,
    private val tokenProvider: () -> Collection<SessionToken> = {
        SessionToken.getAllServiceTokens(context)
    },
) {
    fun discover(): List<MediaSource> {
        return tokenProvider()
            .filter(::isSupported)
            .mapNotNull(::toMediaSource)
            .sortedBy { it.label.toString().lowercase() }
    }

    fun toMediaSource(token: SessionToken): MediaSource? {
        val packageName = token.packageName
        val serviceName = token.serviceName ?: return null
        if (packageName.isBlank() || serviceName.isBlank()) return null
        val label = resolveLabel(packageName)
        return MediaSource(
            packageName = packageName,
            serviceName = serviceName,
            label = label,
            token = token,
        )
    }

    private fun resolveLabel(packageName: String): CharSequence {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo)
        } catch (_: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    companion object {
        fun isSupportedType(type: Int): Boolean {
            return when (type) {
                SessionToken.TYPE_LIBRARY_SERVICE,
                SessionToken.TYPE_SESSION_SERVICE,
                -> true
                else -> false
            }
        }

        fun isSupported(token: SessionToken): Boolean = isSupportedType(token.type)

        fun filterSupported(tokens: Collection<SessionToken>): List<SessionToken> =
            tokens.filter(::isSupported)
    }
}
