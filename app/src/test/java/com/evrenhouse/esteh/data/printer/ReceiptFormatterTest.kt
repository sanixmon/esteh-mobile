package com.evrenhouse.esteh.data.printer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.Charset

class ReceiptFormatterTest {

    private val charsetCp437 = Charset.forName("CP437")

    @Test
    fun testEsTehReceiptFormatting() {
        val formatter = ReceiptFormatter(PrinterProfile.GENERIC_58MM, GbkPrinterCharset())
        val receipt = Receipt(
            type = ReceiptType.ES_TEH,
            storeName = "Es Teh Evren",
            txnId = "TRX-20261016-0001",
            dateString = "Jumat, 16 Oktober 2026",
            timeString = "14:30:00",
            items = listOf(
                ReceiptItem(name = "Es Teh", qty = 2, price = 10_000L, subtotal = 20_000L),
                ReceiptItem(name = "Lemon Tea", qty = 1, price = 11_000L, subtotal = 11_000L)
            ),
            totalQty = 3,
            paymentMethod = "CASH",
            totalAmount = 31_000L,
            isReprint = false,
            footer = "Terima kasih!"
        )

        val commands = formatter.toCommands(receipt)
        val decoded = String(commands, charsetCp437)

        assertTrue("Should contain store name", decoded.contains("Es Teh Evren"))
        assertTrue("Should contain date and time", decoded.contains("Jumat, 16 Oktober 2026 | 14:30:00"))
        assertTrue("Should contain txn id", decoded.contains("TRX-20261016-0001"))
        assertFalse("Original receipt should NOT have reprint banner", decoded.contains("*** SALINAN / CETAK ULANG ***"))
        assertTrue("Should contain Es Teh item line", decoded.contains("Es Teh x2"))
        assertTrue("Should contain Es Teh subtotal", decoded.contains("Rp 20.000"))
        assertTrue("Should contain Es Teh cup line", decoded.contains("2 cup x Rp 10.000"))
        assertTrue("Should contain Lemon Tea item line", decoded.contains("Lemon Tea x1"))
        assertTrue("Should contain Lemon Tea subtotal", decoded.contains("Rp 11.000"))
        assertTrue("Should contain Total Cup", decoded.contains("Total Cup:"))
        assertTrue("Should contain Bayar", decoded.contains("Bayar:"))
        assertTrue("Should contain CASH", decoded.contains("CASH"))
        assertTrue("Should contain TOTAL", decoded.contains("TOTAL:"))
        assertTrue("Should contain total amount", decoded.contains("Rp 31.000"))
        assertTrue("Should contain footer", decoded.contains("Terima kasih!"))
    }

    @Test
    fun testEsTehReprintFormatting() {
        val formatter = ReceiptFormatter(PrinterProfile.GENERIC_58MM, GbkPrinterCharset())
        val receipt = Receipt(
            type = ReceiptType.ES_TEH,
            storeName = "Es Teh Evren",
            txnId = "TRX-20261016-0002",
            dateString = "Jumat, 16 Oktober 2026",
            timeString = "15:00:00",
            items = listOf(
                ReceiptItem(name = "Es Teh", qty = 1, price = 10_000L, subtotal = 10_000L)
            ),
            totalQty = 1,
            paymentMethod = "QRIS",
            totalAmount = 10_000L,
            isReprint = true,
            footer = "Terima kasih!"
        )

        val commands = formatter.toCommands(receipt)
        val decoded = String(commands, charsetCp437)

        assertTrue("Reprint receipt MUST have reprint banner", decoded.contains("*** SALINAN / CETAK ULANG ***"))
        assertTrue("Should contain QRIS payment method", decoded.contains("QRIS"))
    }

    @Test
    fun testDummyTestReceiptFormatting() {
        val formatter = ReceiptFormatter(PrinterProfile.GENERIC_58MM, GbkPrinterCharset())
        val dummy = Receipt.dummyTest()

        val commands = formatter.toCommands(dummy)
        val decoded = String(commands, charsetCp437)

        assertTrue("Dummy receipt should contain store name", decoded.contains("Es Teh Evren"))
        assertTrue("Dummy receipt should contain test txn id", decoded.contains("TRX-TEST-0001"))
        assertTrue("Dummy receipt should contain Es Teh", decoded.contains("Es Teh"))
        assertTrue("Dummy receipt should contain Lemon Tea", decoded.contains("Lemon Tea"))
        assertTrue("Dummy receipt should contain TOTAL", decoded.contains("TOTAL:"))
    }

    @Test
    fun testTwoColWidthAndPadding() {
        val line = twoCol("Total Cup:", "3", 32)
        assertEquals(32, line.length)
        assertTrue(line.startsWith("Total Cup:"))
        assertTrue(line.endsWith("3"))

        val totalLine = twoCol("TOTAL:", "Rp 31.000", 32)
        assertEquals(32, totalLine.length)
        assertTrue(totalLine.startsWith("TOTAL:"))
        assertTrue(totalLine.endsWith("Rp 31.000"))
    }

    @Test
    fun testRupiahFormatting() {
        assertEquals("Rp 10.000", rp(10_000L))
        assertEquals("Rp 1.500.000", rp(1_500_000L))
        assertEquals("Rp 0", rp(0L))
    }
}
