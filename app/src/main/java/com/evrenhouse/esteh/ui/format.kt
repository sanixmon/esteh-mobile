package com.evrenhouse.esteh.ui

import java.text.NumberFormat
import java.util.Locale

private val RUPIAH: NumberFormat = NumberFormat.getNumberInstance(Locale("id", "ID"))

/** 15000 → "Rp 15.000". */
fun Int.toRp(): String = "Rp " + RUPIAH.format(this)

/** TRX-20261015-AB12 → "#AB12". */
fun shortId(id: String): String = "#" + id.takeLast(4)
