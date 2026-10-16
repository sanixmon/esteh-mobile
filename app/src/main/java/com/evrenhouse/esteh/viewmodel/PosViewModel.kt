package com.evrenhouse.esteh.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evrenhouse.esteh.data.CheckoutResult
import com.evrenhouse.esteh.data.PosRepository
import com.evrenhouse.esteh.data.SavePricesResult
import com.evrenhouse.esteh.data.SettingsRepository
import com.evrenhouse.esteh.data.printer.NoOpPrinterRepository
import com.evrenhouse.esteh.data.printer.PrinterDevice
import com.evrenhouse.esteh.data.printer.PrinterRepository
import com.evrenhouse.esteh.data.printer.Receipt
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

/** Snackbar terklasifikasi: warna + ikon mengikuti jenisnya. */
enum class SnackKind { INFO, SUCCESS, WARNING, ERROR }

data class Snack(val text: String, val kind: SnackKind = SnackKind.INFO)

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
    val selectedPrinter: PrinterDevice? = null,
    val pairedPrinters: List<PrinterDevice> = emptyList(),
    val autoPrintEnabled: Boolean = true,
    val isPrinterConnected: Boolean = false,
    val isPrinting: Boolean = false,
    val snackbar: Snack? = null,
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
    private val printer: PrinterRepository = NoOpPrinterRepository(),
    private val suffixGen: () -> String = { defaultSuffix() },
) : ViewModel() {

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    init {
        viewModelScope.launch {
            combine(
                settings.pricesFlow,
                settings.showReceiptFlow,
            ) { p, show -> p to show }.collect { (p, show) ->
                _state.update { it.copy(prices = p, showReceipt = show) }
            }
        }
        viewModelScope.launch {
            combine(
                settings.printerMacFlow,
                settings.printerNameFlow,
                settings.autoPrintFlow,
            ) { mac, name, autoPrint ->
                Triple(mac, name, autoPrint)
            }.collect { (mac, name, autoPrint) ->
                val device = if (!mac.isNullOrBlank()) PrinterDevice(name = name ?: mac, address = mac) else null
                _state.update {
                    it.copy(
                        selectedPrinter = device,
                        autoPrintEnabled = autoPrint,
                        isPrinterConnected = printer.isConnected(),
                    )
                }
            }
        }
        refreshPrinters()
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
                    snackbar = Snack("${def.name} ditambah", SnackKind.INFO),
                )
            }
        }
    }

    fun changeQty(menuId: String, delta: Int) {
        val line = _state.value.cart[menuId] ?: return
        val qty = line.qty + delta
        _state.update {
            if (qty <= 0) {
                it.copy(cart = it.cart - menuId, snackbar = Snack("${line.name} dihapus", SnackKind.INFO))
            } else {
                it.copy(cart = it.cart + (menuId to line.copy(qty = qty)))
            }
        }
    }

    fun removeItem(menuId: String) {
        val line = _state.value.cart[menuId] ?: return
        _state.update { it.copy(cart = it.cart - menuId, snackbar = Snack("${line.name} dihapus", SnackKind.INFO)) }
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
            // Jaring pengaman: bug repository tak boleh crash + flag nyangkut.
            val result = try {
                pos.checkout(tx)
            } catch (e: Exception) {
                _state.update {
                    it.copy(isCompleting = false, snackbar = Snack("Gagal checkout: ${e.message ?: "unknown"}", SnackKind.ERROR))
                }
                return@launch
            }
            when (result) {
                is CheckoutResult.Synced -> {
                    _state.update {
                        if (it.showReceipt) {
                            it.copy(
                                isCompleting = false, receipt = result.transaction,
                                sheet = Sheet.RECEIPT, offline = false,
                            )
                        } else {
                            it.copy(
                                isCompleting = false, cart = emptyMap(), receipt = null,
                                sheet = Sheet.NONE, offline = false,
                                snackbar = Snack("Transaksi ${result.transaction.id} tersimpan", SnackKind.SUCCESS),
                            )
                        }
                    }
                    triggerAutoPrint(result.transaction)
                }
                is CheckoutResult.SavedOffline -> {
                    _state.update {
                        if (it.showReceipt) {
                            it.copy(
                                isCompleting = false, receipt = result.transaction,
                                sheet = Sheet.RECEIPT, offline = true,
                                snackbar = Snack("Offline — tersimpan lokal", SnackKind.WARNING),
                            )
                        } else {
                            it.copy(
                                isCompleting = false, cart = emptyMap(), receipt = null,
                                sheet = Sheet.NONE, offline = true,
                                snackbar = Snack("Offline — tersimpan lokal", SnackKind.WARNING),
                            )
                        }
                    }
                    triggerAutoPrint(result.transaction)
                }
                is CheckoutResult.Rejected -> _state.update {
                    it.copy(isCompleting = false, snackbar = Snack("Ditolak: ${result.message}", SnackKind.ERROR))
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
                    it.copy(sheet = Sheet.NONE, snackbar = Snack("Harga disimpan", SnackKind.SUCCESS))
                }
                SavePricesResult.SavedOffline -> _state.update {
                    it.copy(sheet = Sheet.NONE, offline = true, snackbar = Snack("Harga disimpan (offline)", SnackKind.WARNING))
                }
                SavePricesResult.Invalid -> _state.update {
                    it.copy(snackbar = Snack("Harga harus 1–10.000.000", SnackKind.ERROR))
                }
            }
        }
    }

    fun setShowReceipt(show: Boolean) {
        viewModelScope.launch { settings.setShowReceipt(show) }
    }

    fun refreshPrinters() {
        viewModelScope.launch {
            val paired = printer.getPairedPrinters()
            _state.update { it.copy(pairedPrinters = paired, isPrinterConnected = printer.isConnected()) }
        }
    }

    fun selectPrinter(device: PrinterDevice?) {
        viewModelScope.launch {
            settings.setPrinter(device?.address, device?.name)
            _state.update { it.copy(selectedPrinter = device) }
        }
    }

    fun setAutoPrint(enabled: Boolean) {
        viewModelScope.launch {
            settings.setAutoPrint(enabled)
            _state.update { it.copy(autoPrintEnabled = enabled) }
        }
    }

    fun testPrint() {
        val target = _state.value.selectedPrinter
        if (target == null) {
            _state.update { it.copy(snackbar = Snack("Pilih printer terlebih dahulu di Pengaturan", SnackKind.WARNING)) }
            return
        }
        _state.update { it.copy(isPrinting = true) }
        viewModelScope.launch {
            val res = printer.printReceipt(target, Receipt.dummyTest())
            _state.update {
                it.copy(
                    isPrinting = false,
                    isPrinterConnected = printer.isConnected(),
                    snackbar = if (res.isSuccess) {
                        Snack("Uji cetak berhasil dikirim ke printer", SnackKind.SUCCESS)
                    } else {
                        Snack("Gagal uji cetak: ${res.exceptionOrNull()?.message ?: "periksa printer"}", SnackKind.ERROR)
                    }
                )
            }
        }
    }

    fun printReceipt(tx: Transaction? = null, isReprint: Boolean = false) {
        val targetPrinter = _state.value.selectedPrinter
        if (targetPrinter == null) {
            _state.update { it.copy(snackbar = Snack("Pilih printer terlebih dahulu di Pengaturan", SnackKind.WARNING)) }
            return
        }
        val targetTx = tx ?: _state.value.receipt ?: return
        _state.update { it.copy(isPrinting = true) }
        viewModelScope.launch {
            val receipt = Receipt.fromTransaction(targetTx, isReprint = isReprint)
            val res = printer.printReceipt(targetPrinter, receipt)
            _state.update {
                it.copy(
                    isPrinting = false,
                    isPrinterConnected = printer.isConnected(),
                    snackbar = if (res.isSuccess) {
                        Snack(
                            if (isReprint) "Struk salinan ${targetTx.id} dicetak" else "Struk ${targetTx.id} dicetak",
                            SnackKind.SUCCESS
                        )
                    } else {
                        Snack("Gagal mencetak struk: ${res.exceptionOrNull()?.message ?: "periksa printer"}", SnackKind.ERROR)
                    }
                )
            }
        }
    }

    private fun triggerAutoPrint(tx: Transaction) {
        val s = _state.value
        if (!s.autoPrintEnabled || s.selectedPrinter == null) return
        val targetPrinter = s.selectedPrinter
        viewModelScope.launch {
            val receipt = Receipt.fromTransaction(tx, isReprint = false)
            val printRes = printer.printReceipt(targetPrinter, receipt)
            if (printRes.isFailure) {
                _state.update {
                    it.copy(
                        receipt = tx,
                        sheet = Sheet.RECEIPT,
                        isPrinterConnected = printer.isConnected(),
                        snackbar = Snack(
                            "Transaksi tersimpan, tapi gagal cetak struk: ${printRes.exceptionOrNull()?.message ?: "printer offline"}",
                            SnackKind.WARNING
                        )
                    )
                }
            } else {
                _state.update { it.copy(isPrinterConnected = printer.isConnected()) }
            }
        }
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
