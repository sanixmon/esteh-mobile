package com.evrenhouse.esteh.data.printer

/**
 * Minimal ESC/POS command builder for the X583 V2 (ESC) printer.
 * Commands: init, alignment, bold, font enlargement, line feed, feed, cut and
 * QR (model 2). All values are appended to an internal byte list and returned
 * by [build].
 */
class EscPosEncoder(private val charset: PrinterCharset) {

    private val out = ArrayList<Byte>(512)

    fun init(): EscPosEncoder {
        write(ESC, '@') // ESC @
        return this
    }

    fun alignLeft(): EscPosEncoder = align(0)
    fun alignCenter(): EscPosEncoder = align(1)
    fun alignRight(): EscPosEncoder = align(2)

    private fun align(n: Int): EscPosEncoder {
        write(ESC, 'a', n) // ESC a n
        return this
    }

    fun bold(on: Boolean): EscPosEncoder {
        write(ESC, 'E', if (on) 1 else 0) // ESC E n
        return this
    }

    /**
     * Font enlargement via GS ! n where bits 0-3 = width factor, bits 4-7 =
     * height factor. Factors are 1..8 (spec asks for normal/double/double+double).
     */
    fun size(width: Int, height: Int): EscPosEncoder {
        val w = width.coerceIn(1, 8)
        val h = height.coerceIn(1, 8)
        val n = ((h - 1) shl 4) or (w - 1)
        write(GS, '!', n)
        return this
    }

    fun text(s: String): EscPosEncoder {
        charset.encode(s).forEach { out.add(it) }
        return this
    }

    fun line(): EscPosEncoder {
        out.add(LF.toByte())
        return this
    }

    fun feed(lines: Int): EscPosEncoder {
        repeat(lines.coerceAtLeast(0)) { write(ESC, 'd', 1) } // ESC d 1
        return this
    }

    /** Full cut. Some portable printers ignore this gracefully. */
    fun cut(): EscPosEncoder {
        write(GS, 'V', 0) // GS V m (m=0 full cut)
        return this
    }

    /** ESC/POS QR (model 2). [size] is the module size (1..16). */
    fun qr(data: String, size: Int = 6, errorCorrection: QrErrorCorrection = QrErrorCorrection.M): EscPosEncoder {
        // Select model 2: GS ( k 04 00 31 41 32 00
        write(GS, '(', 'k', 0x04, 0x00, '1', 'A', '2', 0x00)
        // Set module size: GS ( k 03 00 31 43 n
        write(GS, '(', 'k', 0x03, 0x00, '1', 'C', size.coerceIn(1, 16))
        // Set error correction: GS ( k 03 00 31 45 n
        val ec = when (errorCorrection) {
            QrErrorCorrection.L -> '0'
            QrErrorCorrection.M -> '1'
            QrErrorCorrection.Q -> '2'
            QrErrorCorrection.H -> '3'
        }
        write(GS, '(', 'k', 0x03, 0x00, '1', 'E', ec)
        // Store data: GS ( k pL pH 31 50 30 [data]
        val bytes = charset.encode(data)
        val len = bytes.size + 3
        write(GS, '(', 'k', len and 0xFF, (len shr 8) and 0xFF, '1', 'P', '0')
        bytes.forEach { out.add(it) }
        // Print: GS ( k 03 00 31 51 30
        write(GS, '(', 'k', 0x03, 0x00, '1', 'Q', '0')
        return this
    }

    fun build(): ByteArray = out.toByteArray()

    private fun write(vararg values: Any) {
        values.forEach { v ->
            out.add(
                when (v) {
                    is Int -> v.toByte()
                    is Char -> v.code.toByte()
                    else -> v.toString().toInt().toByte()
                }
            )
        }
    }

    private companion object {
        const val ESC = 0x1B
        const val GS = 0x1D
        const val LF = 0x0A
    }
}
