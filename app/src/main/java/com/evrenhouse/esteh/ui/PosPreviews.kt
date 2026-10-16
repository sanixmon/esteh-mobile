package com.evrenhouse.esteh.ui

import android.content.res.Configuration
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.evrenhouse.esteh.data.remote.dto.DaySummary
import com.evrenhouse.esteh.data.remote.dto.PayStats
import com.evrenhouse.esteh.data.remote.dto.PerMenu
import com.evrenhouse.esteh.data.remote.dto.Settings
import com.evrenhouse.esteh.data.remote.dto.Transaction
import com.evrenhouse.esteh.ui.theme.EstehTheme
import com.evrenhouse.esteh.viewmodel.CartLine
import com.evrenhouse.esteh.viewmodel.UiState
import java.time.ZoneId
import java.time.ZonedDateTime

private val PREVIEW_NOW: ZonedDateTime =
    ZonedDateTime.of(2026, 10, 16, 17, 0, 0, 0, ZoneId.of("Asia/Jakarta"))

private val PREVIEW_PRICES = Settings.DEFAULT

private fun sampleSummary() = DaySummary(
    totalTransactions = 3,
    totalRevenue = 45000,
    totalCups = 5,
    perMenu = mapOf("es-teh" to PerMenu("Es Teh", 4, 40000)),
    cash = PayStats(2, 30000),
    qris = PayStats(1, 15000),
)

private fun sampleLine(i: Int) = CartLine(
    menuId = "m$i",
    name = "Es Teh $i",
    price = 10000,
    qty = (i % 3) + 1,
)

private val EMPTY = UiState()

private val TWO_ITEMS = UiState(
    cart = mapOf(
        "es-teh" to CartLine("es-teh", "Es Teh", 10000, 2),
        "lemon-tea" to CartLine("lemon-tea", "Lemon Tea", 11000, 1),
    ),
    payment = Transaction.CASH,
    prices = PREVIEW_PRICES,
    summary = sampleSummary(),
)

private val MANY_ITEMS = UiState(
    cart = (1..32).associate { "m$it" to sampleLine(it) },
    payment = Transaction.QRIS,
    prices = PREVIEW_PRICES,
    summary = sampleSummary(),
)

private val LONG_NAMES = UiState(
    cart = mapOf(
        "es-teh" to CartLine(
            "es-teh",
            "Es Teh Manis Jumbo Extra Gula Batu Spesial Campur Lemon Madu",
            10000,
            12,
        ),
        "lemon-tea" to CartLine(
            "lemon-tea",
            "Lemon Tea Hangat Less Sugar Double Cup Dengan Es Batu Sedikit",
            11000,
            1,
        ),
    ),
    payment = Transaction.CASH,
    prices = PREVIEW_PRICES,
)

@Composable
private fun PosPreview(state: UiState, dark: Boolean = false) {
    EstehTheme(dark = dark) {
        Surface {
            PosScreenContent(
                state = state,
                now = PREVIEW_NOW,
                onAdd = {},
                onChangeQty = { _, _ -> },
                onRemove = {},
                onClear = {},
                onPayment = {},
                onCheckout = {},
                onNewTransaction = {},
                onOpenRecap = {},
                onCloseSheet = {},
                onOpenSettings = {},
                onRefresh = {},
                onFilter = {},
                onSavePrices = { _, _ -> },
                onToggleReceipt = {},
                onPrint = {},
                onDismissSnackbar = {},
            )
        }
    }
}

@Preview(
    name = "Tablet landscape — 2 item",
    device = "spec:width=1280dp,height=800dp,dpi=240",
)
@Composable
private fun PreviewTabletLandscape() {
    PosPreview(TWO_ITEMS)
}

@Preview(
    name = "Tablet portrait — 2 item",
    device = "spec:width=800dp,height=1280dp,dpi=240",
)
@Composable
private fun PreviewTabletPortrait() {
    PosPreview(TWO_ITEMS)
}

@Preview(name = "Phone — kosong")
@Composable
private fun PreviewPhoneEmpty() {
    PosPreview(EMPTY)
}

@Preview(name = "Phone — 30+ item")
@Composable
private fun PreviewPhoneMany() {
    PosPreview(MANY_ITEMS)
}

@Preview(name = "Phone — nama panjang")
@Composable
private fun PreviewLongNames() {
    PosPreview(LONG_NAMES)
}

@Preview(name = "Phone — font 1.3x", fontScale = 1.3f)
@Composable
private fun PreviewFontScale() {
    PosPreview(TWO_ITEMS)
}

@Preview(
    name = "Tablet landscape — dark",
    device = "spec:width=1280dp,height=800dp,dpi=240",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PreviewDark() {
    PosPreview(TWO_ITEMS, dark = true)
}
