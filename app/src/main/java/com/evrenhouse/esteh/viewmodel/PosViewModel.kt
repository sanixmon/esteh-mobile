package com.evrenhouse.esteh.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evrenhouse.esteh.data.CheckoutResult
import com.evrenhouse.esteh.data.PosRepository
import com.evrenhouse.esteh.data.SavePricesResult
import com.evrenhouse.esteh.data.SettingsRepository
import com.evrenhouse.esteh.data.remote.WibTime
import com.evrenhouse.esteh.data.remote.dto.DaySummary
import com.evrenhouse.esteh.data.remote.dto.MenuItem
import com.evrenhouse.esteh.data.remote.dto.Settings
import com.evrenhouse.esteh.data.remote.dto.Transaction
import java.time.ZonedDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Satu baris keranjang. Harga dikunci saat item ditambah (nota lama tak berubah). */
data class CartLine(val menuId: String, val name: String, val price: Int, val qty: Int) {
    val subtotal: Int get() = price * qty
}

data class MenuDef(val menuId: String, val name: String, val asset: String)

/** Katalog 2 menu, harga dari settings, foto dari web (§2.1). */
fun menuCatalog(prices: Settings): List<Pair<MenuDef, Int>> = listOf(
    MenuDef("es-teh", "Es Teh", "es-teh.svg") to prices.esTeh,
    MenuDef("lemon-tea", "Lemon Tea", "lemon-tea.svg") to prices.lemonTea,
)

/** URL absolut aset web: API_BASE_URL + /assets/<file>. */
fun assetUrl(baseUrl: String, asset: String): String =
    baseUrl.trimEnd('/') + "/assets/" + asset.trimStart('/')

enum class Sheet { NONE, RECAP, SETTINGS, RECEIPT }
enum class RecapFilter { ALL, CASH, QRIS }

/** Satu UiState (android.md §8). */
data class UiState(
    val cart: Map<String, CartLine> = emptyMap(),
    val payment: String? = null,
    val prices: Settings = Settings.DEFAULT,
    val summary: DaySummary? = null,
    val txns: List<Transaction> = emptyList(),
    val recapFilter: RecapFilter = RecapFilter.ALL,
    val sheet: Sheet = Sheet.NONE,
    val receipt: Transaction? = null,
    val showReceipt: Boolean = true,
    val snackbar: String? = null,
    val isCompleting: Boolean = false,
    val isLoadingRecap: Boolean = false,
    /** true bila mutasi terakhir hanya tersimpan lokal. */
    val offline: Boolean = false,
) {
    val totalQty: Int get() = cart.values.sumOf { it.qty }
    val totalAmount: Int get() = cart.values.sumOf { it.subtotal }
    val canCheckout: Boolean get() = cart.isNotEmpty() && payment != null && !isCompleting
    val filteredTxns: List<Transaction> get() = when (recapFilter) {
        RecapFilter.ALL -> txns
        RecapFilter.CASH -> txns.filter { it.paymentMethod == Transaction.CASH }
        RecapFilter.QRIS -> txns.filter { it.paymentMethod == Transaction.QRIS }
    }
}

class PosViewModel(
    private val pos: PosRepository,
    private val settings: SettingsRepository,
    private val suffixGen: () -> String = { defaultSuffix() },
) : ViewModel() {

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    init {
        viewModelScope.launch {
            combine(
                settings.pricesFlow,
                settings.lastPaymentFlow,
                settings.showReceiptFlow,
            ) { p, _, show -> p to show }
                .collect { (p, show) -> _state.update { it.copy(prices = p, showReceipt = show) } }
        }
        refreshAll()
    }

    fun refreshAll() {
        viewModelScope.launch {
            settings.refreshFromBackend()
            pos.syncPending()
            pos.getSummary().onSuccess { s ->
                _state.update { it.copy(summary = s, offline = false) }
            }.onFailure {
                _state.update { it.copy(offline = true) }
            }
        }
    }

    /** Tap kartu/tombol: tambah 1. Metode otomatis dari preferensi (§2.3). */
    fun addItem(menuId: String) {
        val s = _state.value
        val (def, price) = menuCatalog(s.prices).firstOrNull { it.first.menuId == menuId } ?: return
        val line = s.cart[menuId]
        val next = if (line == null) CartLine(menuId, def.name, price, 1) else line.copy(qty = line.qty + 1)
        viewModelScope.launch {
            val autoPayment = s.payment ?: settings.lastPaymentFlow.firstOrNull()
            _state.update {
                it.copy(
                    cart = it.cart + (menuId to next),
                    payment = it.payment ?: autoPayment,
                    snackbar = "${def.name} ditambah",
                )
            }
        }
    }

    fun changeQty(menuId: String, delta: Int) {
        val line = _state.value.cart[menuId] ?: return
        val qty = line.qty + delta
        _state.update {
            if (qty <= 0) {
                it.copy(cart = it.cart - menuId, snackbar = "${line.name} dihapus")
            } else {
                it.copy(cart = it.cart + (menuId to line.copy(qty = qty)))
            }
        }
    }

    fun removeItem(menuId: String) {
        val line = _state.value.cart[menuId] ?: return
        _state.update { it.copy(cart = it.cart - menuId, snackbar = "${line.name} dihapus") }
    }

    fun clearCart() {
        _state.update { it.copy(cart = emptyMap()) }
    }

    fun setPayment(method: String) {
        _state.update { it.copy(payment = method) }
        viewModelScope.launch { settings.setLastPayment(method) }
    }

    /** Guard ganda: tombol disabled (canCheckout) + cek di sini; cegah double-tap. */
    fun checkout() {
        val s = _state.value
        if (!s.canCheckout) return
        _state.update { it.copy(isCompleting = true) }
        viewModelScope.launch {
            val now = ZonedDateTime.now(WibTime.ZONE)
            val tx = Transaction(
                id = WibTime.newId(WibTime.dateKey(now), suffixGen()),
                timestamp = WibTime.timestamp(now.toInstant()),
                dateKey = WibTime.dateKey(now),
                timeString = WibTime.timeString(now),
                dateString = WibTime.dateString(now),
                items = s.cart.values.map { MenuItem(it.menuId, it.name, it.price, it.qty, it.subtotal) },
                totalQty = s.totalQty,
                totalAmount = s.totalAmount,
                paymentMethod = s.payment!!,
            )
            when (val r = pos.checkout(tx)) {
                is CheckoutResult.Synced -> _state.update {
                    if (it.showReceipt) {
                        it.copy(
                            isCompleting = false, receipt = r.transaction,
                            sheet = Sheet.RECEIPT, offline = false,
                        )
                    } else {
                        // Struk mati: langsung reset + kabari, tanpa sheet.
                        it.copy(
                            isCompleting = false, cart = emptyMap(), receipt = null,
                            sheet = Sheet.NONE, offline = false,
                            snackbar = "Transaksi ${r.transaction.id} tersimpan",
                        )
                    }
                }
                is CheckoutResult.SavedOffline -> _state.update {
                    if (it.showReceipt) {
                        it.copy(
                            isCompleting = false, receipt = r.transaction,
                            sheet = Sheet.RECEIPT, offline = true,
                            snackbar = "Offline — tersimpan lokal",
                        )
                    } else {
                        it.copy(
                            isCompleting = false, cart = emptyMap(), receipt = null,
                            sheet = Sheet.NONE, offline = true,
                            snackbar = "Offline — tersimpan lokal",
                        )
                    }
                }
            }
            pos.getSummary().onSuccess { sum -> _state.update { it.copy(summary = sum) } }
        }
    }

    /** Tombol "Transaksi Baru": reset cart + tutup sheet (HP). */
    fun newTransaction() {
        _state.update { it.copy(cart = emptyMap(), receipt = null, sheet = Sheet.NONE) }
    }

    fun openRecap() {
        _state.update { it.copy(sheet = Sheet.RECAP, isLoadingRecap = true) }
        viewModelScope.launch {
            pos.getToday().onSuccess { txns -> _state.update { it.copy(txns = txns) } }
            pos.getSummary().onSuccess { s -> _state.update { it.copy(summary = s, offline = false) } }
                .onFailure { _state.update { it.copy(offline = true) } }
            _state.update { it.copy(isLoadingRecap = false) }
        }
    }

    fun setRecapFilter(f: RecapFilter) {
        _state.update { it.copy(recapFilter = f) }
    }

    fun openSettings() {
        _state.update { it.copy(sheet = Sheet.SETTINGS) }
    }

    fun closeSheet() {
        _state.update { it.copy(sheet = Sheet.NONE) }
    }

    fun savePrices(esTeh: Int, lemonTea: Int) {
        viewModelScope.launch {
            when (settings.savePrices(Settings(esTeh, lemonTea))) {
                SavePricesResult.Saved -> _state.update {
                    it.copy(sheet = Sheet.NONE, snackbar = "Harga disimpan")
                }
                SavePricesResult.SavedOffline -> _state.update {
                    it.copy(sheet = Sheet.NONE, offline = true, snackbar = "Harga disimpan (offline)")
                }
                SavePricesResult.Invalid -> _state.update {
                    it.copy(snackbar = "Harga harus 1–10.000.000")
                }
            }
        }
    }

    fun setShowReceipt(show: Boolean) {
        viewModelScope.launch { settings.setShowReceipt(show) }
    }

    /** Tombol Cetak di struk — printer Bluetooth (§9) menyusul. */
    fun printReceipt() {
        _state.update { it.copy(snackbar = "Printer Bluetooth belum disiapkan") }
    }

    fun dismissSnackbar() {
        _state.update { it.copy(snackbar = null) }
    }
}

/**
 * Jam terpisah, tick 30 detik — dilarang re-render seluruh layar tiap detik (§8).
 * Layar POS meng-collect ini hanya di composable Clock kecil.
 */
class ClockViewModel : ViewModel() {
    val now: StateFlow<ZonedDateTime> = flow {
        while (true) {
            emit(ZonedDateTime.now(WibTime.ZONE))
            delay(30_000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ZonedDateTime.now(WibTime.ZONE))
}

private val SUFFIX_CHARS = ('A'..'Z') + ('0'..'9')

private fun defaultSuffix(): String = (1..4).map { SUFFIX_CHARS.random() }.joinToString("")
