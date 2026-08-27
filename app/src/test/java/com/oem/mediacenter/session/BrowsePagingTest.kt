package com.oem.mediacenter.session

import androidx.media3.common.MediaMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowsePagingTest {

    @Test
    fun fromMetadata_mapsPlayableTrack() {
        val node = BrowsePaging.fromMetadata(
            mediaId = "track-1",
            title = "Song",
            subtitle = "Artist",
            isBrowsableFlag = false,
            isPlayableFlag = true,
            mediaType = MediaMetadata.MEDIA_TYPE_MUSIC,
            artworkUri = null,
        )

        assertEquals("track-1", node.mediaId)
        assertEquals("Song", node.title)
        assertEquals("Artist", node.subtitle)
        assertTrue(node.isPlayable)
        assertFalse(node.isBrowsable)
    }

    @Test
    fun fromMetadata_mapsBrowsableFolderByFlag() {
        val node = BrowsePaging.fromMetadata(
            mediaId = "folder-1",
            title = "Albums",
            subtitle = null,
            isBrowsableFlag = true,
            isPlayableFlag = false,
            mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS,
            artworkUri = null,
        )

        assertTrue(node.isBrowsable)
        assertFalse(node.isPlayable)
    }

    @Test
    fun isFolderType_detectsAlbumFolder() {
        assertTrue(BrowsePaging.isFolderType(MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS))
        assertFalse(BrowsePaging.isFolderType(MediaMetadata.MEDIA_TYPE_MUSIC))
    }

    @Test
    fun controlFlags_mapsCommandBooleans() {
        val flags = BrowsePaging.controlFlags(
            canPlayPause = true,
            canSeekNext = false,
            canSeekNextMedia = true,
            canSeekPrevious = false,
            canSeekPreviousMedia = false,
            canSeekInCurrent = true,
        )

        assertTrue(flags.canPlayPause)
        assertTrue(flags.canSkipNext)
        assertFalse(flags.canSkipPrevious)
        assertTrue(flags.canSeek)
    }
}
