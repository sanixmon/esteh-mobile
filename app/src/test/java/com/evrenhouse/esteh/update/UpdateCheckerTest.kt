package com.evrenhouse.esteh.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun `newer tag wins`() {
        assertEquals(1, UpdateChecker.compareVersions("1.0.1", "1.0.0"))
        assertEquals(1, UpdateChecker.compareVersions("1.1.0", "1.0.9"))
        assertEquals(1, UpdateChecker.compareVersions("2.0", "1.9.9"))
    }

    @Test
    fun `equal versions`() {
        assertEquals(0, UpdateChecker.compareVersions("1.0.0", "1.0.0"))
        assertEquals(0, UpdateChecker.compareVersions("1.0", "1.0.0"))
    }

    @Test
    fun `older tag loses`() {
        assertEquals(-1, UpdateChecker.compareVersions("0.9.9", "1.0.0"))
    }

    @Test
    fun `non numeric parts treated as zero`() {
        assertTrue(UpdateChecker.compareVersions("1.0.x", "1.0.0") == 0)
    }
}
