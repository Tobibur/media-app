package com.oem.medialib.internal

import androidx.media3.session.SessionToken
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceDiscoveryTest {

    @Test
    fun isSupportedType_acceptsLibraryAndSessionServices() {
        assertTrue(SourceDiscovery.isSupportedType(SessionToken.TYPE_LIBRARY_SERVICE))
        assertTrue(SourceDiscovery.isSupportedType(SessionToken.TYPE_SESSION_SERVICE))
    }

    @Test
    fun isSupportedType_rejectsSessionOnlyTokens() {
        assertFalse(SourceDiscovery.isSupportedType(SessionToken.TYPE_SESSION))
    }
}
