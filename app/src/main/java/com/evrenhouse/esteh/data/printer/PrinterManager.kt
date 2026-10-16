package com.evrenhouse.esteh.data.printer

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface PrinterManager {
    suspend fun connect(device: PrinterDevice): Result<Unit>
    suspend fun disconnect()
    fun isConnected(): Boolean

    suspend fun print(commands: ByteArray): Result<Unit>
    suspend fun printReceipt(receipt: Receipt): Result<Unit>
    suspend fun printQr(data: String, size: Int = 6, errorCorrection: QrErrorCorrection = QrErrorCorrection.M): Result<Unit>

    suspend fun feed(lines: Int): Result<Unit>
    suspend fun cut(): Result<Unit>
}

class BluetoothPrinterManager(
    private val transport: PrinterTransport,
    private val profile: PrinterProfile,
    private val charset: PrinterCharset,
    private val formatter: ReceiptFormatter
) : PrinterManager {

    /** Serializes every print job — two receipts must never write the same stream at once. */
    private val mutex = Mutex()

    override suspend fun connect(device: PrinterDevice): Result<Unit> = transport.connect(device)

    override suspend fun disconnect() = transport.disconnect()

    override fun isConnected(): Boolean = transport.isConnected()

    override suspend fun print(commands: ByteArray): Result<Unit> = mutex.withLock {
        if (!transport.isConnected()) return@withLock Result.failure(PrinterError.NotConnected)
        transport.write(commands)
    }

    override suspend fun printReceipt(receipt: Receipt): Result<Unit> =
        print(formatter.toCommands(receipt))

    override suspend fun printQr(data: String, size: Int, errorCorrection: QrErrorCorrection): Result<Unit> {
        if (!profile.supportsQr) return Result.failure(PrinterError.UnsupportedCommand)
        return print(EscPosEncoder(charset).init().qr(data, size, errorCorrection).feed(4).build())
    }

    override suspend fun feed(lines: Int): Result<Unit> =
        print(EscPosEncoder(charset).feed(lines).build())

    override suspend fun cut(): Result<Unit> {
        if (!profile.supportsCut) return Result.failure(PrinterError.UnsupportedCommand)
        return print(EscPosEncoder(charset).cut().build())
    }
}
