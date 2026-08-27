package com.oem.medialib

import org.junit.Assert.assertTrue
import org.junit.Test

class SourceCatalogTest {

    @Test
    fun list_returnsProviderResult() {
        val catalog = SourceCatalog { emptyList() }
        assertTrue(catalog.list().isEmpty())
    }
}
