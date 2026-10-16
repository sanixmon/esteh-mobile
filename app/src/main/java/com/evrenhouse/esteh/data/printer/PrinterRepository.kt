package com.evrenhouse.esteh.data.printer

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

interface PrinterRepository {
    suspend fun getPairedPrinters(): List<PrinterDevice>
    suspend fun scanPrinters(): List<PrinterDevice>
    suspend fun connect(device: PrinterDevice): Result<Unit>
    suspend fun disconnect()
    fun isConnected(): Boolean
    suspend fun printReceipt(receipt: Receipt): Result<Unit>
    suspend fun printReceipt(device: PrinterDevice, receipt: Receipt): Result<Unit>
}

class NoOpPrinterRepository : PrinterRepository {
    override suspend fun getPairedPrinters(): List<PrinterDevice> = emptyList()
    override suspend fun scanPrinters(): List<PrinterDevice> = emptyList()
    override suspend fun connect(device: PrinterDevice): Result<Unit> = Result.success(Unit)
    override suspend fun disconnect() {}
    override fun isConnected(): Boolean = false
    override suspend fun printReceipt(receipt: Receipt): Result<Unit> = Result.success(Unit)
    override suspend fun printReceipt(device: PrinterDevice, receipt: Receipt): Result<Unit> = Result.success(Unit)
}

class BluetoothPrinterRepository(context: Context) : PrinterRepository {
    private val appContext = context.applicationContext
    private val charset = GbkPrinterCharset()
    private val profile = PrinterProfile.X583_V2
    private val transport = BluetoothPrinterTransport(appContext)
    private val manager = BluetoothPrinterManager(
        transport = transport,
        profile = profile,
        charset = charset,
        formatter = ReceiptFormatter(profile, charset)
    )

    private val deviceCache = ConcurrentHashMap<String, BluetoothDevice>()

    private val adapter: BluetoothAdapter?
        get() = (appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    @SuppressLint("MissingPermission")
    override suspend fun getPairedPrinters(): List<PrinterDevice> = withContext(Dispatchers.IO) {
        val a = adapter ?: return@withContext emptyList()
        if (!hasConnectPermission()) return@withContext emptyList()
        val bonded = runCatching { a.bondedDevices ?: emptySet() }.getOrDefault(emptySet())
        bonded.mapNotNull { bt ->
            if (bt.address == null) return@mapNotNull null
            deviceCache[bt.address] = bt
            transport.cacheDevice(bt)
            PrinterDevice(name = bt.name ?: "Perangkat ${bt.address.takeLast(4)}", address = bt.address)
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun scanPrinters(): List<PrinterDevice> = withContext(Dispatchers.IO) {
        val a = adapter ?: return@withContext emptyList()
        if (!hasConnectPermission() || !a.isEnabled) return@withContext emptyList()
        if (!hasScanPermission()) return@withContext emptyList()

        val found = LinkedHashMap<String, PrinterDevice>()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                if (intent.action != BluetoothDevice.ACTION_FOUND) return
                val bt = parseDevice(intent) ?: return
                val name = bt.name
                if (name.isNullOrBlank() || bt.address == null) return
                found[bt.address] = PrinterDevice(name = name, address = bt.address)
                deviceCache[bt.address] = bt
                transport.cacheDevice(bt)
            }
        }
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiverSafe(receiver, filter)
        runCatching { a.cancelDiscovery() }
        val started = runCatching { a.startDiscovery() }.getOrDefault(false)
        if (started) delay(12_000)
        runCatching { a.cancelDiscovery() }
        runCatching { appContext.unregisterReceiver(receiver) }
        found.values.toList()
    }

    override suspend fun connect(device: PrinterDevice): Result<Unit> = manager.connect(device)

    override suspend fun disconnect() = manager.disconnect()

    override fun isConnected(): Boolean = manager.isConnected()

    override suspend fun printReceipt(receipt: Receipt): Result<Unit> = manager.printReceipt(receipt)

    override suspend fun printReceipt(device: PrinterDevice, receipt: Receipt): Result<Unit> {
        if (!isConnected()) {
            val connectResult = connect(device)
            if (connectResult.isFailure) return connectResult
        }
        val printResult = printReceipt(receipt)
        if (printResult.isFailure) {
            val retryConnect = connect(device)
            if (retryConnect.isSuccess) {
                return printReceipt(receipt)
            }
        }
        return printResult
    }

    private fun hasConnectPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            appContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    private fun hasScanPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            appContext.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            appContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }

    private fun registerReceiverSafe(receiver: BroadcastReceiver, filter: IntentFilter) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            appContext.registerReceiver(receiver, filter)
        }
    }

    @Suppress("DEPRECATION")
    private fun parseDevice(intent: Intent): BluetoothDevice? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
}
