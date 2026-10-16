package com.evrenhouse.esteh.data.remote

import com.evrenhouse.esteh.data.remote.dto.DaySummary
import com.evrenhouse.esteh.data.remote.dto.MenuItem
import com.evrenhouse.esteh.data.remote.dto.PayStats
import com.evrenhouse.esteh.data.remote.dto.PerMenu
import com.evrenhouse.esteh.data.remote.dto.Settings
import com.evrenhouse.esteh.data.remote.dto.SummaryResponse
import com.evrenhouse.esteh.data.remote.dto.TodayResponse
import com.evrenhouse.esteh.data.remote.dto.Transaction
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `summary backend decodes`() {
        val body = """
            {"summary":{"totalTransactions":3,"totalRevenue":45000,"totalCups":5,
            "perMenu":{"es-teh":{"name":"Es Teh","qty":4,"revenue":40000}},
            "cash":{"count":2,"revenue":30000},"qris":{"count":1,"revenue":15000},
            "extraFieldMasaDepan":1}}
        """.trimIndent()
        val res = json.decodeFromString<SummaryResponse>(body)
        assertEquals(3, res.summary.totalTransactions)
        assertEquals(40000, res.summary.perMenu.getValue("es-teh").revenue)
        assertEquals(PayStats(2, 30000), res.summary.cash)
    }

    @Test
    fun `transaction round trip`() {
        val tx = Transaction(
            id = "TRX-20261015-AB12",
            timestamp = "2026-10-15T10:00:00Z",
            dateKey = "2026-10-15",
            timeString = "17:00:00",
            dateString = "Kamis, 15 Oktober 2026",
            items = listOf(MenuItem("es-teh", "Es Teh", 10000, 2, 20000)),
            totalQty = 2,
            totalAmount = 20000,
            paymentMethod = Transaction.CASH,
        )
        val decoded = json.decodeFromString<Transaction>(json.encodeToString(Transaction.serializer(), tx))
        assertEquals(tx, decoded)
    }

    @Test
    fun `today empty defaults`() {
        assertEquals(0, json.decodeFromString<TodayResponse>("{}").transactions.size)
    }

    @Test
    fun `settings serial names + validation`() {
        val s = json.decodeFromString<Settings>("""{"es-teh":10000,"lemon-tea":11000}""")
        assertEquals(Settings.DEFAULT, s)
        assertTrue(s.isValid())
        assertFalse(Settings(0, 11000).isValid())
        assertFalse(Settings(10000, 10_000_001).isValid())
        // encode pakai serial name ber-tanda hubung
        val encoded = json.encodeToString(Settings.serializer(), s)
        assertTrue(encoded.contains("\"es-teh\"") && encoded.contains("\"lemon-tea\""))
    }

    @Test
    fun `day summary empty perMenu defaults`() {
        val body = """{"totalTransactions":0,"totalRevenue":0,"totalCups":0,
            "cash":{"count":0,"revenue":0},"qris":{"count":0,"revenue":0}}"""
        val d = json.decodeFromString<DaySummary>(body)
        assertTrue(d.perMenu.isEmpty())
    }
}
