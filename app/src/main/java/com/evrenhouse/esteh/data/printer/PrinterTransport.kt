package com.evrenhouse.esteh.data.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

interface PrinterTransport {
    suspend fun connect(device: PrinterDevice): Result<Unit>
    suspend fun write(data: ByteArray): Result<Unit>
    suspend fun disconnect()
    fun isConnected(): Boolean
}

/**
 * Bluetooth Classic (BR/EDR -> RFCOMM/SPP) transport for the RP02N / X583 V2.
 * Connects on demand using the standard SPP UUID (00001101-0000-1000-8000-00805F9B34FB).
 */
class BluetoothPrinterTransport(context: Context) : PrinterTransport {

    private val appContext = context.applicationContext
    private val deviceCache = ConcurrentHashMap<String, BluetoothDevice>()

    private var socket: BluetoothSocket? = null
    private var output: OutputStream? = null

    @Volatile
    private var connected = false

    private val adapter: BluetoothAdapter?
        get() = (appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    /** Register a real [BluetoothDevice] so connect() works even when the MAC is redacted. */
    fun cacheDevice(device: BluetoothDevice) {
        if (device.address != null) deviceCache[device.address] = device
    }

    @SuppressLint("MissingPermission")
    override suspend fun connect(device: PrinterDevice): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val a = adapter ?: return@withContext Result.failure(PrinterError.BluetoothDisabled)
            if (!a.isEnabled) return@withContext Result.failure(PrinterError.BluetoothDisabled)
            closeQuietly()

            val target: BluetoothDevice = deviceCache[device.address] ?: a.getRemoteDevice(device.address)
            val s = target.createRfcommSocketToServiceRecord(SPP_UUID)
            s.connect()
            socket = s
            output = s.outputStream
            connected = true
            Result.success(Unit)
        } catch (e: SecurityException) {
            closeQuietly()
            Result.failure(PrinterError.PermissionDenied)
        } catch (e: IllegalArgumentException) {
            closeQuietly()
            Result.failure(PrinterError.DeviceNotFound)
        } catch (e: IOException) {
            closeQuietly()
            Result.failure(PrinterError.ConnectionFailed)
        } catch (e: Exception) {
            closeQuietly()
            Result.failure(PrinterError.Unknown(e))
        }
    }

    override suspend fun write(data: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val o = output ?: return@withContext Result.failure(PrinterError.NotConnected)
            o.write(data)
            o.flush()
            Result.success(Unit)
        } catch (e: SecurityException) {
            Result.failure(PrinterError.PermissionDenied)
        } catch (e: IOException) {
            closeQuietly()
            Result.failure(PrinterError.PrintFailed)
        }
    }

    override suspend fun disconnect() {
        withContext(Dispatchers.IO) { closeQuietly() }
    }

    override fun isConnected(): Boolean = connected

    private fun closeQuietly() {
        connected = false
        runCatching { output?.close() }
        runCatching { socket?.close() }
        output = null
        socket = null
    }

    private companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}
