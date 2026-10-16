package com.evrenhouse.esteh.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.evrenhouse.esteh.data.remote.dto.DaySummary
import com.evrenhouse.esteh.data.remote.dto.Transaction
import java.io.File
import java.io.FileWriter

object RecapCsvExporter {

    fun generateCsv(
        txns: List<Transaction>,
        summary: DaySummary?,
        periodLabel: String,
    ): String = buildString {
        appendLine("Laporan Penjualan Es Teh Evren")
        appendLine("Periode,\"$periodLabel\"")
        appendLine()
        appendLine("ID Transaksi,Tanggal,Jam,Rincian Pesanan,Total Cup,Metode Bayar,Total (Rp)")

        for (tx in txns) {
            val itemsSummary = tx.items.joinToString(" + ") { "${it.name} x${it.qty}" }
            append("\"${tx.id}\",")
            append("\"${tx.dateString}\",")
            append("\"${tx.timeString}\",")
            append("\"${escapeCsv(itemsSummary)}\",")
            append("${tx.totalQty},")
            append("\"${tx.paymentMethod.uppercase()}\",")
            appendLine("${tx.totalAmount}")
        }

        appendLine()
        val totalCups = summary?.totalCups ?: txns.sumOf { it.totalQty }
        val totalRev = summary?.totalRevenue ?: txns.sumOf { it.totalAmount }
        val totalCash = summary?.cash?.revenue ?: txns.filter { it.paymentMethod == Transaction.CASH }.sumOf { it.totalAmount }
        val totalQris = summary?.qris?.revenue ?: txns.filter { it.paymentMethod == Transaction.QRIS }.sumOf { it.totalAmount }

        appendLine("RINGKASAN REKAP")
        appendLine("Total Transaksi,${summary?.totalTransactions ?: txns.size}")
        appendLine("Total Cup,$totalCups")
        appendLine("Total Tunai (Cash),$totalCash")
        appendLine("Total QRIS,$totalQris")
        appendLine("TOTAL OMZET,$totalRev")
    }

    private fun escapeCsv(value: String): String =
        value.replace("\"", "\"\"")

    fun shareCsv(
        context: Context,
        txns: List<Transaction>,
        summary: DaySummary?,
        periodLabel: String,
    ): Intent {
        val csvContent = generateCsv(txns, summary, periodLabel)
        val cleanLabel = periodLabel.replace(Regex("[^A-Za-z0-9]"), "_")
        val fileName = "rekap_esteh_${cleanLabel}.csv"

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, fileName)
        FileWriter(file).use { it.write(csvContent) }

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Rekap Penjualan Es Teh Evren - $periodLabel")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(intent, "Bagikan Rekap Penjualan (Excel/CSV)")
    }
}
