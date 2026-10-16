package com.evrenhouse.esteh.viewmodel

import com.evrenhouse.esteh.data.CheckoutResult
import com.evrenhouse.esteh.data.PosRepository
import com.evrenhouse.esteh.data.SavePricesResult
import com.evrenhouse.esteh.data.SettingsRepository
import com.evrenhouse.esteh.data.printer.PrinterDevice
import com.evrenhouse.esteh.data.printer.PrinterRepository
import com.evrenhouse.esteh.data.printer.Receipt
import com.evrenhouse.esteh.data.printer.ReceiptType
import com.evrenhouse.esteh.data.remote.dto.DaySummary
import com.evrenhouse.esteh.data.remote.dto.PayStats
import com.evrenhouse.esteh.data.remote.dto.Settings
import com.evrenhouse.esteh.data.remote.dto.Transaction
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PosViewModelTest {

    private val main = UnconfinedTestDispatcher()

    class FakePos : PosRepository {
        var summary = DaySummary(0, 0, 0, emptyMap(), PayStats(0, 0), PayStats(0, 0))
        var txns: List<Transaction> = emptyList()
        var checkoutHandler: suspend (Transaction) -> CheckoutResult = { CheckoutResult.Synced(it) }
        var checkoutCalls = 0
        var synced = 0
        override suspend fun getSummary(from: String?, to: String?, date: String?) = Result.success(summary)
        override suspend fun getToday(from: String?, to: String?, date: String?) = Result.success(txns)
        override suspend fun checkout(tx: Transaction): CheckoutResult {
            checkoutCalls++
            return checkoutHandler(tx)
        }
        override suspend fun syncPending(): Int = synced
    }

    class FakeSettings(
        prices: Settings = Settings.DEFAULT,
        lastPayment: String? = null,
        showReceipt: Boolean = true,
    ) : SettingsRepository {
        val pricesState = MutableStateFlow(prices)
        val lastPaymentState = MutableStateFlow(lastPayment)
        val showReceiptState = MutableStateFlow(showReceipt)
        val printerMacState = MutableStateFlow<String?>(null)
        val printerNameState = MutableStateFlow<String?>(null)
        val autoPrintState = MutableStateFlow(true)
        override val pricesFlow = pricesState
        override val lastPaymentFlow = lastPaymentState
        override val showReceiptFlow = showReceiptState
        override val printerMacFlow = printerMacState
        override val printerNameFlow = printerNameState
        override val autoPrintFlow = autoPrintState
        var saveCalls = 0
        var saveResult: SavePricesResult = SavePricesResult.Saved
        override suspend fun refreshFromBackend() {}
        override suspend fun savePrices(s: Settings): SavePricesResult {
            if (!s.isValid()) return SavePricesResult.Invalid
            saveCalls++
            if (saveResult == SavePricesResult.Saved) pricesState.value = s
            return saveResult
        }
        override suspend fun setLastPayment(method: String) { lastPaymentState.value = method }
        override suspend fun setShowReceipt(show: Boolean) { showReceiptState.value = show }
        override suspend fun setPrinter(mac: String?, name: String?) {
            printerMacState.value = mac
            printerNameState.value = name
        }
        override suspend fun setAutoPrint(enabled: Boolean) { autoPrintState.value = enabled }
    }

    class FakePrinter(
        var paired: List<PrinterDevice> = emptyList(),
        var connectResult: Result<Unit> = Result.success(Unit),
        var printResult: Result<Unit> = Result.success(Unit),
    ) : PrinterRepository {
        var printCalls = 0
        var lastPrintedReceipt: Receipt? = null
        var lastPrintedDevice: PrinterDevice? = null
        var connected = false

        override suspend fun getPairedPrinters(): List<PrinterDevice> = paired
        override suspend fun scanPrinters(): List<PrinterDevice> = paired
        override suspend fun connect(device: PrinterDevice): Result<Unit> {
            connected = connectResult.isSuccess
            return connectResult
        }
        override suspend fun disconnect() { connected = false }
        override fun isConnected(): Boolean = connected
        override suspend fun printReceipt(receipt: Receipt): Result<Unit> {
            printCalls++
            lastPrintedReceipt = receipt
            return printResult
        }
        override suspend fun printReceipt(device: PrinterDevice, receipt: Receipt): Result<Unit> {
            lastPrintedDevice = device
            printCalls++
            lastPrintedReceipt = receipt
            return printResult
        }
    }

    class VmFixture(
        val vm: PosViewModel,
        val pos: FakePos,
        val settings: FakeSettings,
        val printer: FakePrinter,
    ) {
        operator fun component1() = vm
        operator fun component2() = pos
        operator fun component3() = settings
        operator fun component4() = printer
    }

    @Before
    fun setup() {
        Dispatchers.setMain(main)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun vm(
        pos: FakePos = FakePos(),
        settings: FakeSettings = FakeSettings(),
        printer: FakePrinter = FakePrinter(),
    ) = VmFixture(PosViewModel(pos, settings, printer, suffixGen = { "T001" }), pos, settings, printer)

    @Test
    fun `addItem prices from settings, accumulates`() = runTest {
        val (v, _, _) = vm()
        advanceUntilIdle()
        v.addItem("es-teh")
        v.addItem("es-teh")
        advanceUntilIdle()
        val line = v.state.value.cart.getValue("es-teh")
        assertEquals(2, line.qty)
        assertEquals(10000, line.price)
        assertEquals(20000, v.state.value.totalAmount)
    }

    @Test
    fun `auto payment from device preference`() = runTest {
        val (v, _, _) = vm(settings = FakeSettings(lastPayment = "qris"))
        advanceUntilIdle()
        v.addItem("lemon-tea")
        advanceUntilIdle()
        assertEquals("qris", v.state.value.payment)
    }

    @Test
    fun `old lines keep price after change`() = runTest {
        val settings = FakeSettings()
        val (v, _, _) = vm(settings = settings)
        advanceUntilIdle()
        v.addItem("es-teh")
        advanceUntilIdle()
        settings.pricesState.value = Settings(12000, 13000)
        advanceUntilIdle()
        v.addItem("es-teh")
        advanceUntilIdle()
        val line = v.state.value.cart.getValue("es-teh")
        assertEquals(10000, line.price)
        assertEquals(2, line.qty)
    }

    @Test
    fun `checkout blocked when empty or unpaid`() = runTest {
        val (v, pos, _) = vm()
        advanceUntilIdle()
        v.checkout()
        advanceUntilIdle()
        assertEquals(0, pos.checkoutCalls)
        v.addItem("es-teh") // tanpa payment & tanpa preferensi
        advanceUntilIdle()
        v.checkout()
        advanceUntilIdle()
        assertEquals(0, pos.checkoutCalls)
        assertNull(v.state.value.receipt)
    }

    @Test
    fun `checkout success shows receipt`() = runTest {
        val (v, pos, _) = vm()
        advanceUntilIdle()
        v.setPayment(Transaction.CASH)
        v.addItem("es-teh")
        advanceUntilIdle()
        v.checkout()
        advanceUntilIdle()
        assertEquals(1, pos.checkoutCalls)
        assertNotNull(v.state.value.receipt)
        assertEquals(Sheet.RECEIPT, v.state.value.sheet)
        assertEquals(false, v.state.value.isCompleting)
        // Transaksi Baru mereset cart
        v.newTransaction()
        assertTrue(v.state.value.cart.isEmpty())
        assertEquals(Sheet.NONE, v.state.value.sheet)
    }

    @Test
    fun `double tap posts once`() = runTest {
        val pos = FakePos()
        val gate = CompletableDeferred<CheckoutResult>()
        pos.checkoutHandler = { gate.await() }
        val (v, _, _) = vm(pos = pos)
        advanceUntilIdle()
        v.setPayment(Transaction.QRIS)
        v.addItem("es-teh")
        advanceUntilIdle()
        v.checkout() // jalan sampai gate, isCompleting=true
        v.checkout() // terblokir guard
        assertEquals(1, pos.checkoutCalls)
        gate.complete(
            CheckoutResult.Synced(
                Transaction("X", "", "", "", "", emptyList(), 0, 0, Transaction.QRIS),
            ),
        )
        advanceUntilIdle()
        assertEquals(1, pos.checkoutCalls)
        assertNotNull(v.state.value.receipt)
    }

    @Test
    fun `offline checkout flags offline + snackbar`() = runTest {
        val pos = FakePos()
        pos.checkoutHandler = { CheckoutResult.SavedOffline(it) }
        val (v, _, _) = vm(pos = pos)
        advanceUntilIdle()
        v.setPayment(Transaction.CASH)
        v.addItem("es-teh")
        advanceUntilIdle()
        v.checkout()
        advanceUntilIdle()
        assertEquals(true, v.state.value.offline)
        assertEquals("Offline — tersimpan lokal", v.state.value.snackbar?.text)
        assertNotNull(v.state.value.receipt)
    }

    @Test
    fun `savePrices invalid never hits repo`() = runTest {
        val settings = FakeSettings()
        val (v, _, _) = vm(settings = settings)
        advanceUntilIdle()
        v.savePrices(0, 13000)
        advanceUntilIdle()
        assertEquals(0, settings.saveCalls)
        assertEquals("Harga harus 1–10.000.000", v.state.value.snackbar?.text)
    }

    @Test
    fun `recap loads and filters`() = runTest {
        fun tx(id: String, m: String) = Transaction(
            id, "2026-10-15T10:00:00Z", "2026-10-15", "17:00:00", "Kamis, 15 Oktober 2026",
            emptyList(), 1, 10000, m,
        )
        val pos = FakePos()
        pos.txns = listOf(tx("A", Transaction.CASH), tx("B", Transaction.QRIS))
        val (v, _, _) = vm(pos = pos)
        advanceUntilIdle()
        v.openRecap()
        advanceUntilIdle()
        assertEquals(2, v.state.value.txns.size)
        v.setRecapFilter(RecapFilter.CASH)
        assertEquals(listOf("A"), v.state.value.filteredTxns.map { it.id })
        v.setRecapFilter(RecapFilter.QRIS)
        assertEquals(listOf("B"), v.state.value.filteredTxns.map { it.id })
    }

    @Test
    fun `rejected shows message no receipt stuck flag`() = runTest {
        val pos = FakePos()
        pos.checkoutHandler = { CheckoutResult.Rejected("Transaksi tidak valid") }
        val (v, _, _) = vm(pos = pos)
        advanceUntilIdle()
        v.setPayment(Transaction.CASH)
        v.addItem("es-teh")
        advanceUntilIdle()
        v.checkout()
        advanceUntilIdle()
        assertNull(v.state.value.receipt)
        assertEquals(false, v.state.value.isCompleting)
        assertEquals("Ditolak: Transaksi tidak valid", v.state.value.snackbar?.text)
        assertEquals(Sheet.NONE, v.state.value.sheet)
    }

    @Test
    fun `qty zero removes line`() = runTest {
        val (v, _, _) = vm()
        advanceUntilIdle()
        v.addItem("es-teh")
        advanceUntilIdle()
        v.changeQty("es-teh", -1)
        assertTrue(v.state.value.cart.isEmpty())
        assertEquals("Es Teh dihapus", v.state.value.snackbar?.text)
    }

    @Test
    fun `receipt hidden when toggle off resets cart`() = runTest {
        val (v, pos, _) = vm(settings = FakeSettings(showReceipt = false))
        advanceUntilIdle()
        v.setPayment(Transaction.CASH)
        v.addItem("es-teh")
        advanceUntilIdle()
        v.checkout()
        advanceUntilIdle()
        assertEquals(1, pos.checkoutCalls)
        assertTrue(v.state.value.cart.isEmpty())
        assertNull(v.state.value.receipt)
        assertEquals(Sheet.NONE, v.state.value.sheet)
        assertTrue(v.state.value.snackbar?.text?.contains("tersimpan") == true)
    }

    @Test
    fun `toggle receipt updates state`() = runTest {
        val settings = FakeSettings(showReceipt = true)
        val (v, _, _) = vm(settings = settings)
        advanceUntilIdle()
        assertEquals(true, v.state.value.showReceipt)
        v.setShowReceipt(false)
        advanceUntilIdle()
        assertEquals(false, v.state.value.showReceipt)
    }

    @Test
    fun `printReceipt without selected printer shows warning`() = runTest {
        val (v, _, _) = vm()
        advanceUntilIdle()
        v.printReceipt()
        assertEquals("Pilih printer terlebih dahulu di Pengaturan", v.state.value.snackbar?.text)
    }

    @Test
    fun `selectPrinter and testPrint succeeds`() = runTest {
        val printer = FakePrinter()
        val (v, _, settings) = vm(printer = printer)
        advanceUntilIdle()
        val device = PrinterDevice("RP02N", "00:11:22:33:44:55")
        v.selectPrinter(device)
        advanceUntilIdle()
        assertEquals("00:11:22:33:44:55", settings.printerMacState.value)
        assertEquals("RP02N", settings.printerNameState.value)
        assertEquals(device, v.state.value.selectedPrinter)

        v.testPrint()
        advanceUntilIdle()
        assertEquals(1, printer.printCalls)
        assertEquals("Uji cetak berhasil dikirim ke printer", v.state.value.snackbar?.text)
    }

    @Test
    fun `checkout with autoPrint prints receipt automatically`() = runTest {
        val printer = FakePrinter()
        val settings = FakeSettings()
        val (v, _, _) = vm(settings = settings, printer = printer)
        advanceUntilIdle()
        val device = PrinterDevice("RP02N", "00:11:22:33:44:55")
        v.selectPrinter(device)
        advanceUntilIdle()

        v.setPayment(Transaction.CASH)
        v.addItem("es-teh")
        advanceUntilIdle()
        v.checkout()
        advanceUntilIdle()

        assertEquals(1, printer.printCalls)
        assertNotNull(printer.lastPrintedReceipt)
        assertEquals("Es Teh Evren", printer.lastPrintedReceipt?.storeName)
    }

    @Test
    fun `checkout with autoPrint failure falls back to receipt sheet with warning`() = runTest {
        val printer = FakePrinter(printResult = Result.failure(Exception("Connection lost")))
        val settings = FakeSettings(showReceipt = false)
        val (v, _, _) = vm(settings = settings, printer = printer)
        advanceUntilIdle()
        val device = PrinterDevice("RP02N", "00:11:22:33:44:55")
        v.selectPrinter(device)
        advanceUntilIdle()

        v.setPayment(Transaction.CASH)
        v.addItem("es-teh")
        advanceUntilIdle()
        v.checkout()
        advanceUntilIdle()

        // Fallback: sheet is opened despite showReceipt being false, so cashier can reprint
        assertEquals(Sheet.RECEIPT, v.state.value.sheet)
        assertNotNull(v.state.value.receipt)
        assertTrue(v.state.value.snackbar?.text?.contains("gagal cetak struk") == true)
    }

    @Test
    fun `setRecapDatePreset updates date range and reloads`() = runTest {
        val (v, _, _) = vm()
        advanceUntilIdle()
        v.setRecapDatePreset(RecapDatePreset.YESTERDAY)
        advanceUntilIdle()
        assertEquals(RecapDatePreset.YESTERDAY, v.state.value.recapDateRange.preset)
        assertEquals("Kemarin", v.state.value.recapDateRange.label)

        v.setRecapDatePreset(RecapDatePreset.LAST_7_DAYS)
        advanceUntilIdle()
        assertEquals(RecapDatePreset.LAST_7_DAYS, v.state.value.recapDateRange.preset)
        assertEquals("7 Hari Terakhir", v.state.value.recapDateRange.label)

        v.setRecapDatePreset(RecapDatePreset.CUSTOM, "2026-10-01", "2026-10-10", "1 - 10 Okt")
        advanceUntilIdle()
        assertEquals(RecapDatePreset.CUSTOM, v.state.value.recapDateRange.preset)
        assertEquals("1 - 10 Okt", v.state.value.recapDateRange.label)
        assertEquals("2026-10-01", v.state.value.recapDateRange.from)
        assertEquals("2026-10-10", v.state.value.recapDateRange.to)
    }

    @Test
    fun `viewTransactionDetail sets receipt and opens RECEIPT sheet`() = runTest {
        val (v, _, _) = vm()
        advanceUntilIdle()
        val tx = Transaction("TRX-001", "2026-10-16T10:00:00Z", "2026-10-16", "10:00", "Jumat", emptyList(), 1, 10000, "cash")
        v.viewTransactionDetail(tx)
        assertEquals(tx, v.state.value.receipt)
        assertEquals(Sheet.RECEIPT, v.state.value.sheet)
    }

    @Test
    fun `printRecapReceipt prints summary`() = runTest {
        val printer = FakePrinter()
        val pos = FakePos()
        pos.summary = DaySummary(10, 100000, 10, emptyMap(), PayStats(5, 50000), PayStats(5, 50000))
        val (v, _, _) = vm(pos = pos, printer = printer)
        advanceUntilIdle()
        v.selectPrinter(PrinterDevice("RP02N", "00:11:22:33:44:55"))
        advanceUntilIdle()
        v.openRecap()
        advanceUntilIdle()

        v.printRecapReceipt()
        advanceUntilIdle()
        assertEquals(1, printer.printCalls)
        assertEquals("Es Teh Evren", printer.lastPrintedReceipt?.storeName)
        assertEquals(ReceiptType.ES_TEH_RECAP, printer.lastPrintedReceipt?.type)
        assertEquals("Struk rekap berhasil dicetak", v.state.value.snackbar?.text)
    }
}
