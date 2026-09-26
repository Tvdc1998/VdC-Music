package dev.mellow.core.data

import dev.mellow.core.data.scanner.LocalMediaScanner
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalMediaScannerTest {

    @Test
    fun localServerId_constantValue() {
        assertEquals("local_device", LocalMediaScanner.LOCAL_SERVER_ID)
    }
}
