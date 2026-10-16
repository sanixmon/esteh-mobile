package com.evrenhouse.esteh.data

import com.evrenhouse.esteh.data.remote.dto.DaySummary
import com.evrenhouse.esteh.data.remote.dto.MenuItem
import com.evrenhouse.esteh.data.remote.dto.PayStats
import com.evrenhouse.esteh.data.remote.dto.PerMenu
import com.evrenhouse.esteh.data.remote.dto.Transaction
import org.junit.Assert.assertTrue
import org.junit.Test

class RecapCsvExporterTest {

    @Test
    fun testGenerateCsv() {
        val txns = listOf(
            Transaction(
                id = "TRX-20261016-0001",
                timestamp = "2026-10-16T10:00:00Z",
                dateKey = "2026-10-16",
                timeString = "10:00:00",
                dateString = "Jumat, 16 Oktober 2026",
                items = listOf(
                    MenuItem("es-teh", "Es Teh", 10_000, 2, 20_000),
                    MenuItem("lemon-tea", "Lemon Tea", 11_000, 1, 11_000)
                ),
                totalQty = 3,
                totalAmount = 31_000,
                paymentMethod = "cash",
            )
        )
        val summary = DaySummary(
            totalTransactions = 1,
            totalRevenue = 31_000,
            totalCups = 3,
            perMenu = mapOf(
                "es-teh" to PerMenu("Es Teh", 2, 20_000),
                "lemon-tea" to PerMenu("Lemon Tea", 1, 11_000)
            ),
            cash = PayStats(1, 31_000),
            qris = PayStats(0, 0)
        )

        val csv = RecapCsvExporter.generateCsv(txns, summary, "Hari Ini")

        assertTrue("Should contain title", csv.contains("Laporan Penjualan Es Teh Evren"))
        assertTrue("Should contain period", csv.contains("Periode,\"Hari Ini\""))
        assertTrue("Should contain columns", csv.contains("ID Transaksi,Tanggal,Jam,Rincian Pesanan,Total Cup,Metode Bayar,Total (Rp)"))
        assertTrue("Should contain txn id", csv.contains("TRX-20261016-0001"))
        assertTrue("Should contain items summary", csv.contains("Es Teh x2 + Lemon Tea x1"))
        assertTrue("Should contain total amount", csv.contains("31000"))
        assertTrue("Should contain total cups", csv.contains("Total Cup,3"))
        assertTrue("Should contain total omzet", csv.contains("TOTAL OMZET,31000"))
    }
}
