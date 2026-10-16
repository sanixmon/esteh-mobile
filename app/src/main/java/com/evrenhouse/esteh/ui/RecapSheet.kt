package com.evrenhouse.esteh.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import com.evrenhouse.esteh.R
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.evrenhouse.esteh.data.remote.dto.DaySummary
import com.evrenhouse.esteh.data.remote.dto.Transaction
import com.evrenhouse.esteh.ui.theme.LocalEstehExtraColors
import com.evrenhouse.esteh.viewmodel.RecapFilter

/**
 * Isi rekap harian: hero pendapatan, kartu Cash/QRIS, rincian menu,
 * riwayat + filter, nota pendek + long-press copy full ID (§2.7).
 * Wadah (sheet 92% / dialog 660dp) diatur pemanggil sesuai lebar layar (§4).
 */
@Composable
fun RecapContent(
    summary: DaySummary?,
    txns: List<Transaction>,
    filter: RecapFilter,
    isLoading: Boolean,
    onFilter: (RecapFilter) -> Unit,
    onReprint: (Transaction) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        item {
            if (summary == null) {
                Text(
                    text = if (isLoading) "Memuat rekap…" else "Belum ada data hari ini.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                MicroLabel("Pendapatan hari ini")
                EstehBezel(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = summary.totalRevenue.toRp(),
                            style = MaterialTheme.typography.displaySmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "${summary.totalTransactions} TRANSAKSI • ${summary.totalCups} CUP",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val extra = LocalEstehExtraColors.current
                    PayCard(
                        "Cash", summary.cash.count, summary.cash.revenue,
                        extra.cashContainer, extra.onCashContainer, Modifier.weight(1f),
                    )
                    PayCard(
                        "QRIS", summary.qris.count, summary.qris.revenue,
                        extra.qrisContainer, extra.onQrisContainer, Modifier.weight(1f),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                summary.perMenu.values.forEach { m ->
                    Text(
                        text = "${m.name}: ${m.qty} cup • ${m.revenue.toRp()}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        item {
            MicroLabel("Riwayat")
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RecapFilter.entries.forEach { f ->
                    FilterChip(
                        selected = filter == f,
                        onClick = { onFilter(f) },
                        label = {
                            Text(
                                when (f) {
                                    RecapFilter.ALL -> "Semua"
                                    RecapFilter.CASH -> "Cash"
                                    RecapFilter.QRIS -> "QRIS"
                                },
                            )
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (isLoading && txns.isEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                ) { CircularProgressIndicator() }
            }
        } else if (txns.isEmpty()) {
            item {
                Text(
                    text = "Belum ada transaksi.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
        } else {
            items(txns, key = { it.id }) { tx -> TxnRow(tx, onReprint) }
        }
    }
}

@Composable
private fun PayCard(
    label: String,
    count: Int,
    revenue: Int,
    container: Color,
    onContainer: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = container),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = onContainer,
            )
            Text(
                text = revenue.toRp(),
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = onContainer,
            )
            Text(
                text = "$count TRANSAKSI",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = onContainer,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TxnRow(tx: Transaction, onReprint: (Transaction) -> Unit) {
    val clipboard = LocalClipboardManager.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = { clipboard.setText(AnnotatedString(tx.id)) },
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = shortId(tx.id),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                text = "${tx.timeString} • ${tx.totalQty} cup • ${tx.paymentMethod.uppercase()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = tx.totalAmount.toRp(),
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = FontFamily.Monospace,
        )
        Spacer(modifier = Modifier.width(4.dp))
        IconButton(
            onClick = { onReprint(tx) },
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_print),
                contentDescription = "Cetak ulang struk ${tx.id}",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
