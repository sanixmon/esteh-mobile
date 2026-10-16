package com.evrenhouse.esteh.data.printer

import java.nio.charset.Charset

/**
 * Maps application text to printer bytes. The X583 V2 self-test reports
 * "Encode: gbk / Codepage: CP437", so receipts must never assume UTF-8.
 *
 * Receipt content is Latin (Indonesian/English), which shares byte values with
 * ASCII across GBK/CP437/Latin-1. We therefore sanitize to an ASCII-safe form
 * before encoding, so "Rp", "Es Teh", "Total" never come out corrupted.
 */
interface PrinterCharset {
    fun encode(text: String): ByteArray
}

class GbkPrinterCharset : PrinterCharset {
    private val target: Charset = runCatching { Charset.forName("GBK") }.getOrElse { Charsets.US_ASCII }

    override fun encode(text: String): ByteArray {
        val safe = PrinterText.ascii(text)
        return runCatching { safe.toByteArray(target) }
            .getOrElse { safe.toByteArray(Charsets.US_ASCII) }
    }
}

object PrinterText {
    /** Transliterate the few non-ASCII glyphs used in receipts; map the rest to '?'. */
    fun ascii(text: String): String = buildString(text.length) {
        for (c in text) {
            when (c) {
                '×' -> append('x')
                '½' -> append("0.5")
                '\u2019', '\u2018' -> append('\'')
                '\u201C', '\u201D' -> append('"')
                '\u2013', '\u2014' -> append('-')
                '•', '\u2022' -> append('|')
                '\u00A0' -> append(' ')
                in '\u0000'..'\u007F' -> append(c)
                else -> append('?')
            }
        }
    }
}
