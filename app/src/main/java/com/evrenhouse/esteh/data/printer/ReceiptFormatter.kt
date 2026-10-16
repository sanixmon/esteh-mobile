package com.evrenhouse.esteh.data.printer

import java.util.Locale

/**
 * Turns a domain [Receipt] into ESC/POS bytes, sized to the printer paper.
 * Implements ES_TEH 58mm 32-column receipt layout (android.md §9.1).
 */
class ReceiptFormatter(
    private val profile: PrinterProfile = PrinterProfile.X583_V2,
    private val charset: PrinterCharset = GbkPrinterCharset()
) {
    private val width: Int get() = profile.paper.charactersPerLine
    private val dashes: String get() = "-".repeat(width)

    fun toCommands(receipt: Receipt): ByteArray {
        val e = EscPosEncoder(charset).init()
        val w = width

        if (receipt.type == ReceiptType.ES_TEH) {
            formatEsTeh(e, receipt, w)
            return e.build()
        }
        if (receipt.type == ReceiptType.ES_TEH_RECAP) {
            formatEsTehRecap(e, receipt, w)
            return e.build()
        }

        // Fallback for legacy formats if needed
        val title = when (receipt.type) {
            ReceiptType.MULAI -> "Struk Mulai Sewa"
            ReceiptType.SELESAI -> "Struk Selesai Sewa"
            ReceiptType.ADDITIONAL -> "Struk Tambahan Sewa"
            ReceiptType.SETTLEMENT -> "Struk Tutup Shift"
            ReceiptType.ES_TEH, ReceiptType.ES_TEH_RECAP -> "Struk Es Teh"
        }

        if (receipt.type == ReceiptType.ADDITIONAL) {
            e.alignCenter().bold(true).text("*** ADDITIONAL ORDER ***").bold(false).line()
            e.alignLeft().text(dashes).line()
        }
        e.alignCenter().bold(true).size(1, 2).text(receipt.storeName).size(1, 1).bold(false).line()
        e.alignCenter().text(receipt.subtitle).line()
        e.alignCenter().text(title).line()
        e.alignLeft().text(dashes).line()

        if (receipt.type == ReceiptType.MULAI || receipt.type == ReceiptType.ADDITIONAL) {
            e.alignCenter().text("Queue Number: ${receipt.queueNo}").line()
            e.alignLeft().text("Tgl: ${receipt.tanggal} | ${receipt.startTime}").line()
            e.text("Shift: ${receipt.shift ?: "-"}").line()
            e.text("Nama: ${receipt.nama}").line()
        } else if (receipt.type == ReceiptType.SETTLEMENT) {
            e.text("Tgl Shift: ${receipt.tanggal}").line()
            e.text("Jam Tutup: ${receipt.endTime ?: "-"}").line()
            e.text("Petugas: ${receipt.shift ?: "-"}").line()
            e.text("Total Trx: ${receipt.totalTxns} Selesai").line()
        } else {
            e.text("Shift: ${receipt.shift ?: "-"}").line()
            e.text("Nama: ${receipt.nama}").line()
            e.text("Mulai: ${receipt.startTime} | Selesai: ${receipt.endTime ?: "-"}").line()
            receipt.durasi?.let { e.text("Durasi: $it").line() }
        }
        e.text(dashes).line()

        if (receipt.type != ReceiptType.SETTLEMENT) {
            receipt.itemsText.split("\n").forEach { line ->
                if (line.isBlank()) return@forEach
                itemLines(line, w).forEach { wrapped -> e.text(wrapped).line() }
            }
            if (receipt.type == ReceiptType.SELESAI) {
                receipt.otText?.let { e.text("OT: $it").line() }
            }
            e.text(dashes).line()
        }

        when (receipt.type) {
            ReceiptType.MULAI -> {
                e.bold(true).text(twoCol("Total Pokok:", rp(receipt.totalPokok), w)).bold(false).line()
            }
            ReceiptType.ADDITIONAL -> {
                val payLabel = receipt.payAwal?.uppercase(Locale.getDefault()) ?: "CASH"
                e.bold(true).text(twoCol("Total Tambahan:", "${rp(receipt.totalPokok)} ($payLabel)", w)).bold(false).line()
            }
            ReceiptType.SELESAI -> {
                val payLabel = receipt.payAwal?.uppercase(Locale.getDefault()) ?: "CASH"
                e.text(twoCol("Sewa Pokok:", "${rp(receipt.totalPokok)} ($payLabel)", w)).line()
                receipt.overtime?.takeIf { it > 0 }?.let {
                    e.text(twoCol("Overtime:", rp(it), w)).line()
                }
                e.text(dashes).line()
                e.bold(true).text(twoCol("TOTAL:", rp(receipt.total), w)).bold(false).line()
                receipt.cash?.takeIf { it > 0 }?.let { e.text(twoCol("Cash:", rp(it), w)).line() }
                receipt.qris?.takeIf { it > 0 }?.let { e.text(twoCol("QRIS:", rp(it), w)).line() }
            }
            ReceiptType.SETTLEMENT -> {
                if (receipt.totalPokok > 0) {
                    e.text(twoCol("Sewa Pokok:", rp(receipt.totalPokok), w)).line()
                }
                receipt.overtime?.takeIf { it > 0 }?.let {
                    e.text(twoCol("Overtime:", rp(it), w)).line()
                }
                e.text(dashes).line()
                e.bold(true).text(twoCol("TOTAL OMZET:", rp(receipt.total), w)).bold(false).line()
                e.text(twoCol("Total Tunai:", rp(receipt.cash ?: 0), w)).line()
                e.text(twoCol("Total QRIS:", rp(receipt.qris ?: 0), w)).line()
            }
            ReceiptType.ES_TEH, ReceiptType.ES_TEH_RECAP -> Unit
        }
        e.text(dashes).line()

        if ((receipt.type == ReceiptType.MULAI || receipt.type == ReceiptType.ADDITIONAL) && profile.supportsQr && receipt.qrText != null) {
            e.alignCenter().qr(receipt.qrText, 6, QrErrorCorrection.M)
            receipt.qrCaption?.let { e.alignCenter().text(it).line() }
            e.alignLeft().text(dashes).line()
        }

        e.feed(1)
        e.alignCenter().text(receipt.footer).line()
        e.feed(2)
        if (profile.supportsCut) e.cut()
        return e.build()
    }

    private fun formatEsTeh(e: EscPosEncoder, receipt: Receipt, w: Int) {
        // ── 1. Header ────────────────────────────────────────────────────────
        e.alignCenter().bold(true).size(1, 2).text(receipt.storeName).size(1, 1).bold(false).line()
        if (receipt.dateString.isNotBlank()) {
            e.alignCenter().text(receipt.dateString).line()
        }
        val timeAndId = if (receipt.timeString.isNotBlank() && receipt.txnId.isNotBlank()) {
            "${receipt.timeString} | ${receipt.txnId}"
        } else {
            receipt.timeString.ifBlank { receipt.txnId }
        }
        if (timeAndId.isNotBlank()) {
            if (timeAndId.length <= w) {
                e.alignCenter().text(timeAndId).line()
            } else {
                if (receipt.timeString.isNotBlank()) e.alignCenter().text(receipt.timeString).line()
                if (receipt.txnId.isNotBlank()) e.alignCenter().text(receipt.txnId).line()
            }
        }
        if (receipt.isReprint) {
            e.alignCenter().bold(true).text("*** SALINAN / CETAK ULANG ***").bold(false).line()
        }
        e.alignLeft().text(dashes).line()

        // ── 2. Items ─────────────────────────────────────────────────────────
        receipt.items.forEach { item ->
            val line1 = "${item.name} x${item.qty}  ${rp(item.subtotal)}"
            itemLines(line1, w).forEach { wrapped -> e.text(wrapped).line() }
            val line2 = "  ${item.qty} cup x ${rp(item.price)}"
            e.text(line2.take(w)).line()
        }
        e.text(dashes).line()

        // ── 3. Totals ────────────────────────────────────────────────────────
        e.text(twoCol("Total Cup:", "${receipt.totalQty} cup", w)).line()
        e.text(twoCol("Metode Bayar:", receipt.paymentMethod.uppercase(Locale.getDefault()), w)).line()
        e.bold(true).text(twoCol("TOTAL:", rp(receipt.totalAmount), w)).bold(false).line()
        e.text(dashes).line()

        // ── 4. Footer ────────────────────────────────────────────────────────
        e.feed(1)
        e.alignCenter().text(receipt.footer).line()
        e.feed(2)
        if (profile.supportsCut) e.cut()
    }

    private fun formatEsTehRecap(e: EscPosEncoder, receipt: Receipt, w: Int) {
        val s = receipt.summary
        val equalsLine = "=".repeat(w)

        // ── 1. Header ────────────────────────────────────────────────────────
        e.alignCenter().bold(true).size(1, 2).text(receipt.storeName).size(1, 1).bold(false).line()
        e.alignCenter().bold(true).text(receipt.subtitle.ifBlank { "REKAP PENJUALAN" }).bold(false).line()
        e.alignLeft().text(dashes).line()

        // ── 2. Periode & Waktu ────────────────────────────────────────────────
        if (receipt.dateRangeLabel.isNotBlank()) {
            if (receipt.dateRangeLabel.length + 9 <= w) {
                e.text(twoCol("Periode:", receipt.dateRangeLabel, w)).line()
            } else {
                e.text("Periode:").line()
                e.alignCenter().text(receipt.dateRangeLabel).line().alignLeft()
            }
        }
        if (receipt.timeString.isNotBlank()) {
            e.text(twoCol("Waktu Cetak:", receipt.timeString, w)).line()
        }
        e.text(dashes).line()

        // ── 3. Ringkasan Agregat ──────────────────────────────────────────────
        e.bold(true).text("RINGKASAN PENJUALAN:").bold(false).line()
        val txnsCount = s?.totalTransactions ?: 0
        val cupsCount = s?.totalCups ?: receipt.totalQty
        e.text(twoCol("Total Transaksi:", "$txnsCount trx", w)).line()
        e.text(twoCol("Total Volume:", "$cupsCount cup", w)).line()
        e.text(dashes).line()

        // ── 4. Rincian Produk ─────────────────────────────────────────────────
        if (s != null && s.perMenu.isNotEmpty()) {
            e.bold(true).text("RINCIAN PRODUK:").bold(false).line()
            s.perMenu.values.forEach { m ->
                val line1 = twoCol(m.name, "${m.qty} cup", w)
                e.text(line1).line()
                val line2 = twoCol("  Omzet", rp(m.revenue.toLong()), w)
                e.text(line2).line()
            }
            e.text(dashes).line()
        }

        // ── 5. Metode Pembayaran ──────────────────────────────────────────────
        if (s != null) {
            e.bold(true).text("METODE PEMBAYARAN:").bold(false).line()
            e.text(twoCol("Cash (${s.cash.count} trx):", rp(s.cash.revenue.toLong()), w)).line()
            e.text(twoCol("QRIS (${s.qris.count} trx):", rp(s.qris.revenue.toLong()), w)).line()
            e.text(dashes).line()
        }

        // ── 6. Total Omzet ────────────────────────────────────────────────────
        val totalRevenue = s?.totalRevenue?.toLong() ?: receipt.totalAmount
        e.bold(true).text(twoCol("TOTAL OMZET:", rp(totalRevenue), w)).bold(false).line()
        e.text(equalsLine).line()

        // ── 7. Footer ─────────────────────────────────────────────────────────
        e.feed(1)
        e.alignCenter().text(receipt.footer).line()
        e.feed(2)
        if (profile.supportsCut) e.cut()
    }

    /**
     * Right-aligns the price (the part after a double space, per the item
     * format `name xQty  RpPrice`) on the last line of a wrapped item, keeping
     * every amount on the same right column as "TOTAL:".
     */
    private fun itemLines(line: String, w: Int): List<String> {
        val idx = line.lastIndexOf("  ")
        if (idx < 0) return wrapWords(line, w)
        val left = line.substring(0, idx)
        val price = line.substring(idx + 2)
        val leftWidth = (w - price.length - 1).coerceAtLeast(1)
        val leftLines = wrapWords(left, leftWidth)
        return leftLines.mapIndexed { i, l ->
            if (i == leftLines.size - 1) twoCol(l, price, w) else l
        }
    }
}

// ── Pure text helpers (unit-testable) ───────────────────────────────────────

/** 50000 -> "50.000", 1500000 -> "1.500.000". */
fun formatRupiah(value: Long): String {
    val negative = value < 0
    val s = if (negative) (-value).toString() else value.toString()
    val sb = StringBuilder()
    for (i in s.indices) {
        if (i > 0 && (s.length - i) % 3 == 0) sb.append('.')
        sb.append(s[i])
    }
    return if (negative) "-$sb" else sb.toString()
}

/** Format Rp: 10000 -> "Rp 10.000". */
fun rp(value: Long): String = "Rp ${formatRupiah(value)}"

fun twoCol(left: String, right: String, width: Int): String =
    left + " ".repeat((width - left.length - right.length).coerceAtLeast(1)) + right

/** Word-boundary wrap so prices/tokens are never split mid-word. */
fun wrapWords(text: String, width: Int): List<String> {
    if (text.length <= width) return listOf(text)
    val words = text.split(" ")
    val out = ArrayList<String>()
    var cur = ""
    for (word in words) {
        cur = when {
            cur.isEmpty() -> word
            cur.length + 1 + word.length <= width -> "$cur $word"
            else -> { out.add(cur); word }
        }
    }
    if (cur.isNotEmpty()) out.add(cur)
    return out
}
