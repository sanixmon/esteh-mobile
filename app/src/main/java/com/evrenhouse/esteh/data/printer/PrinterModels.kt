package com.evrenhouse.esteh.data.printer

import com.evrenhouse.esteh.data.remote.dto.DaySummary
import com.evrenhouse.esteh.data.remote.dto.Transaction
import java.nio.charset.Charset

enum class PrinterType { BLUETOOTH_CLASSIC }

data class PrinterDevice(
    val name: String,
    val address: String,
    val type: PrinterType = PrinterType.BLUETOOTH_CLASSIC
)

/** Structured, user-facing printer errors. Extends Exception so it can flow through Result. */
sealed class PrinterError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    data object BluetoothDisabled : PrinterError("Bluetooth dinonaktifkan")
    data object PermissionDenied : PrinterError("Izin Bluetooth ditolak")
    data object DeviceNotFound : PrinterError("Printer tidak ditemukan")
    data object ConnectionFailed : PrinterError("Gagal terhubung ke printer")
    data object NotConnected : PrinterError("Printer belum terhubung")
    data object PrintFailed : PrinterError("Gagal mencetak ke printer")
    data object UnsupportedCommand : PrinterError("Perintah tidak didukung printer")
    data object EncodingFailure : PrinterError("Gagal meng-encode teks struk")
    data class Unknown(override val cause: Throwable) : PrinterError(cause.message ?: "Terjadi kesalahan tak dikenal", cause)
}

sealed interface PrinterConnectionState {
    data object Disconnected : PrinterConnectionState
    data object Connecting : PrinterConnectionState
    data class Connected(val device: PrinterDevice) : PrinterConnectionState
    data class Error(val message: String) : PrinterConnectionState
}

data class PrinterPaperConfig(
    val paperWidthMm: Int,
    val charactersPerLine: Int
)

data class PrinterProfile(
    val name: String,
    val system: String,
    val encoding: Charset,
    val paper: PrinterPaperConfig,
    val supportsQr: Boolean,
    val supportsBarcode: Boolean,
    val supportsCut: Boolean,
    val supportsBold: Boolean,
    val supportsDoubleSize: Boolean
) {
    companion object {
        /** X583 V2 (ESC), Bluetooth name RP02N. */
        val X583_V2 = PrinterProfile(
            name = "RP02N",
            system = "X583 V2 (ESC)",
            encoding = Charset.forName("GBK"),
            paper = PrinterPaperConfig(paperWidthMm = 58, charactersPerLine = 32),
            supportsQr = true,
            supportsBarcode = false,
            supportsCut = true,
            supportsBold = true,
            supportsDoubleSize = true
        )

        /** Generic 58mm profile alias. */
        val GENERIC_58MM = X583_V2
    }
}

enum class QrErrorCorrection { L, M, Q, H }

enum class BarcodeType { CODE128, EAN13, CODE39 }

enum class ReceiptType { ES_TEH, ES_TEH_RECAP, MULAI, SELESAI, ADDITIONAL, SETTLEMENT }

data class ReceiptItem(
    val name: String,
    val qty: Int,
    val price: Long,
    val subtotal: Long
)

data class Receipt(
    val type: ReceiptType = ReceiptType.ES_TEH,
    val storeName: String = "Es Teh Evren",
    val subtitle: String = "Segar & Alami",
    val txnId: String = "",
    val dateString: String = "",
    val timeString: String = "",
    val items: List<ReceiptItem> = emptyList(),
    val totalQty: Int = 0,
    val paymentMethod: String = "CASH",
    val totalAmount: Long = 0,
    val isReprint: Boolean = false,
    val summary: DaySummary? = null,
    val dateRangeLabel: String = "",
    val footer: String = "Terima kasih!",
    // Compatibility fields for kasir-mobile formats if needed
    val queueNo: Int = 0,
    val nama: String = "",
    val shift: String? = null,
    val tanggal: String = "",
    val startTime: String = "",
    val endTime: String? = null,
    val durasi: String? = null,
    val itemsText: String = "",
    val otText: String? = null,
    val totalPokok: Long = 0,
    val payAwal: String? = null,
    val overtime: Long? = null,
    val total: Long = 0,
    val cash: Long? = null,
    val qris: Long? = null,
    val qrText: String? = null,
    val qrCaption: String? = null,
    val totalTxns: Int = 0
) {
    companion object {
        fun fromTransaction(tx: Transaction, isReprint: Boolean = false): Receipt {
            return Receipt(
                type = ReceiptType.ES_TEH,
                storeName = "Es Teh Evren",
                txnId = tx.id,
                dateString = tx.dateString,
                timeString = tx.timeString,
                items = tx.items.map { item ->
                    ReceiptItem(
                        name = item.name,
                        qty = item.qty,
                        price = item.price.toLong(),
                        subtotal = item.subtotal.toLong()
                    )
                },
                totalQty = tx.totalQty,
                paymentMethod = tx.paymentMethod.uppercase(),
                totalAmount = tx.totalAmount.toLong(),
                isReprint = isReprint,
                footer = "Terima kasih!"
            )
        }

        fun fromSummary(summary: DaySummary, dateRangeLabel: String, printTime: String): Receipt {
            return Receipt(
                type = ReceiptType.ES_TEH_RECAP,
                storeName = "Es Teh Evren",
                subtitle = "REKAP PENJUALAN",
                dateRangeLabel = dateRangeLabel,
                timeString = printTime,
                summary = summary,
                totalQty = summary.totalCups,
                totalAmount = summary.totalRevenue.toLong(),
                footer = "Terima kasih!"
            )
        }

        fun dummyTest(): Receipt {
            return Receipt(
                type = ReceiptType.ES_TEH,
                storeName = "Es Teh Evren",
                txnId = "TRX-TEST-0001",
                dateString = "Jumat, 16 Oktober 2026",
                timeString = "10:00:00",
                items = listOf(
                    ReceiptItem(name = "Es Teh", qty = 1, price = 10_000L, subtotal = 10_000L),
                    ReceiptItem(name = "Lemon Tea", qty = 1, price = 11_000L, subtotal = 11_000L)
                ),
                totalQty = 2,
                paymentMethod = "CASH",
                totalAmount = 21_000L,
                isReprint = false,
                footer = "Terima kasih!"
            )
        }
    }
}
