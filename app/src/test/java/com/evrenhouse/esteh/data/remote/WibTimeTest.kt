package com.evrenhouse.esteh.data.remote

import java.time.Instant
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class WibTimeTest {

    // 2026-10-15T10:00:00Z == 17:00:00 WIB.
    private val zoned: ZonedDateTime = ZonedDateTime.ofInstant(
        Instant.parse("2026-10-15T10:00:00Z"), WibTime.ZONE,
    )

    @Test
    fun `wib fields`() {
        assertEquals("2026-10-15", WibTime.dateKey(zoned))
        assertEquals("17:00:00", WibTime.timeString(zoned))
        assertEquals("Kamis, 15 Oktober 2026", WibTime.dateString(zoned))
        assertEquals("2026-10-15T10:00:00Z", WibTime.timestamp(zoned.toInstant()))
    }

    @Test
    fun `id format`() {
        assertEquals("TRX-20261015-AB12", WibTime.newId("2026-10-15", "AB12"))
    }
}
