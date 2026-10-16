package com.evrenhouse.esteh.data.remote

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Jam zona Asia/Jakarta untuk field waktu Transaction (android.md §1, §6).
 * Semua fungsi menerima waktu eksplisit agar murni + testable;
 * default-nya jam perangkat yang dipaksa ke WIB.
 */
object WibTime {

    val ZONE: ZoneId = ZoneId.of("Asia/Jakarta")
    private val LOCALE_ID: Locale = Locale("id", "ID")

    /** ISO-8601 UTC, cth "2026-10-15T17:00:00Z". */
    fun timestamp(instant: Instant = Instant.now()): String = instant.toString()

    /** yyyy-MM-dd dalam WIB — server yang menentukan hari via CURRENT_DATE (§7). */
    fun dateKey(zoned: ZonedDateTime = ZonedDateTime.now(ZONE)): String =
        zoned.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

    /** HH:mm:ss dalam WIB. */
    fun timeString(zoned: ZonedDateTime = ZonedDateTime.now(ZONE)): String =
        zoned.format(DateTimeFormatter.ofPattern("HH:mm:ss"))

    /** Cth "Kamis, 15 Oktober 2026". */
    fun dateString(zoned: ZonedDateTime = ZonedDateTime.now(ZONE)): String =
        zoned.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", LOCALE_ID))

    /** TRX-YYYYMMDD-XXXX. suffix 4 char disuplai pemanggil (acak / counter). */
    fun newId(dateKey: String, suffix: String): String =
        "TRX-${dateKey.replace("-", "")}-$suffix"
}
