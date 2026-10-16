package com.evrenhouse.esteh.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Model data paritas web POS (android.md §6). Field name mengikuti backend apa adanya. */
@Serializable
data class MenuItem(
    val menuId: String,
    val name: String,
    val price: Int,
    val qty: Int,
    val subtotal: Int,
)

@Serializable
data class Transaction(
    val id: String, // TRX-YYYYMMDD-XXXX
    val timestamp: String, // ISO-8601
    val dateKey: String, // yyyy-MM-dd (zona Asia/Jakarta)
    val timeString: String, // HH:mm:ss
    val dateString: String, // "Kamis, 15 Oktober 2026"
    val items: List<MenuItem>,
    val totalQty: Int,
    val totalAmount: Int,
    val paymentMethod: String, // "cash" | "qris"
    val cashReceived: Int? = null,
    val change: Int? = null,
) {
    companion object {
        const val CASH = "cash"
        const val QRIS = "qris"
    }
}

@Serializable
data class DaySummary(
    val totalTransactions: Int,
    val totalRevenue: Int,
    val totalCups: Int,
    val perMenu: Map<String, PerMenu> = emptyMap(),
    val cash: PayStats,
    val qris: PayStats,
)

@Serializable
data class PerMenu(val name: String, val qty: Int, val revenue: Int)

@Serializable
data class PayStats(val count: Int, val revenue: Int)

@Serializable
data class Settings(
    @SerialName("es-teh") val esTeh: Int,
    @SerialName("lemon-tea") val lemonTea: Int,
) {
    companion object {
        const val MIN_PRICE = 1
        const val MAX_PRICE = 10_000_000
        val DEFAULT = Settings(esTeh = 10_000, lemonTea = 11_000)
    }

    /** Validasi android.md §2.8: 1–10.000.000. */
    fun isValid(): Boolean =
        esTeh in MIN_PRICE..MAX_PRICE && lemonTea in MIN_PRICE..MAX_PRICE
}

// Wrapper respons backend (android.md §7).
@Serializable
data class SummaryResponse(val summary: DaySummary)

@Serializable
data class TodayResponse(val transactions: List<Transaction> = emptyList())

@Serializable
data class TransactionResponse(val transaction: Transaction)

@Serializable
data class SettingsResponse(val settings: Settings)

@Serializable
data class ErrorResponse(val error: String = "")
