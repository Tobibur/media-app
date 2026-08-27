package com.oem.medialib.internal

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.oem.medialib.BrowseNode

internal object BrowsePaging {
    const val PAGE_SIZE: Int = 50

    fun toBrowseNode(item: MediaItem): BrowseNode {
        val metadata = item.mediaMetadata
        return fromMetadata(
            mediaId = item.mediaId,
            title = metadata.title?.toString()
                ?: metadata.displayTitle?.toString()
                ?: item.mediaId,
            subtitle = metadata.artist?.toString()
                ?: metadata.subtitle?.toString()
                ?: metadata.albumTitle?.toString(),
            isBrowsableFlag = metadata.isBrowsable,
            isPlayableFlag = metadata.isPlayable,
            mediaType = metadata.mediaType ?: MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
            artworkUri = metadata.artworkUri,
        )
    }

    fun fromMetadata(
        mediaId: String,
        title: String,
        subtitle: String?,
        isBrowsableFlag: Boolean?,
        isPlayableFlag: Boolean?,
        mediaType: Int,
        artworkUri: Uri?,
    ): BrowseNode {
        return BrowseNode(
            mediaId = mediaId,
            title = title,
            subtitle = subtitle,
            isBrowsable = isBrowsableFlag == true || isFolderType(mediaType),
            isPlayable = isPlayableFlag == true,
            artworkUri = artworkUri,
        )
    }

    fun isFolderType(mediaType: Int): Boolean {
        return mediaType == MediaMetadata.MEDIA_TYPE_FOLDER_MIXED ||
            mediaType == MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS ||
            mediaType == MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS ||
            mediaType == MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS ||
            mediaType == MediaMetadata.MEDIA_TYPE_FOLDER_GENRES ||
            mediaType == MediaMetadata.MEDIA_TYPE_FOLDER_YEARS ||
            mediaType == MediaMetadata.MEDIA_TYPE_FOLDER_MOVIES ||
            mediaType == MediaMetadata.MEDIA_TYPE_FOLDER_TV_SHOWS ||
            mediaType == MediaMetadata.MEDIA_TYPE_FOLDER_AUDIO_BOOKS ||
            mediaType == MediaMetadata.MEDIA_TYPE_FOLDER_RADIO_STATIONS ||
            mediaType == MediaMetadata.MEDIA_TYPE_FOLDER_PODCASTS ||
            mediaType == MediaMetadata.MEDIA_TYPE_FOLDER_VIDEOS
    }

    fun controlFlags(
        canPlayPause: Boolean,
        canSeekNext: Boolean,
        canSeekNextMedia: Boolean,
        canSeekPrevious: Boolean,
        canSeekPreviousMedia: Boolean,
        canSeekInCurrent: Boolean,
    ): ControlFlags {
        return ControlFlags(
            canPlayPause = canPlayPause,
            canSkipNext = canSeekNext || canSeekNextMedia,
            canSkipPrevious = canSeekPrevious || canSeekPreviousMedia,
            canSeek = canSeekInCurrent,
        )
    }

    data class ControlFlags(
        val canPlayPause: Boolean,
        val canSkipNext: Boolean,
        val canSkipPrevious: Boolean,
        val canSeek: Boolean,
    )
}
