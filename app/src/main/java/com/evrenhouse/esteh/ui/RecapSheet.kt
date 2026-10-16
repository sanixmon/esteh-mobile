package com.evrenhouse.esteh.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.evrenhouse.esteh.R
import com.evrenhouse.esteh.data.remote.dto.DaySummary
import com.evrenhouse.esteh.data.remote.dto.Transaction
import com.evrenhouse.esteh.ui.theme.LocalEstehExtraColors
import com.evrenhouse.esteh.viewmodel.RecapDatePreset
import com.evrenhouse.esteh.viewmodel.RecapDateRange
import com.evrenhouse.esteh.viewmodel.RecapFilter
import java.time.Instant
import java.time.ZoneId

/**
 * Isi rekap penjualan: rentang tanggal, ringkasan omzet & cup, rincian produk,
 * tombol Cetak Rekap 58mm, Ekspor Excel CSV, serta riwayat transaksi.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecapContent(
    summary: DaySummary?,
    txns: List<Transaction>,
    filter: RecapFilter,
    dateRange: RecapDateRange = RecapDateRange(),
    isLoading: Boolean = false,
    onFilter: (RecapFilter) -> Unit = {},
    onSelectPreset: (RecapDatePreset, String?, String?, String?) -> Unit = { _, _, _, _ -> },
    onPrintRecap: () -> Unit = {},
    onExportCsv: () -> Unit = {},
    onSelectTxn: (Transaction) -> Unit = {},
    onReprint: (Transaction) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showCustomDateDialog by remember { mutableStateOf(false) }

    if (showCustomDateDialog) {
        val datePickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showCustomDateDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val startMs = datePickerState.selectedStartDateMillis
                        val endMs = datePickerState.selectedEndDateMillis ?: startMs
                        if (startMs != null && endMs != null) {
                            val start = Instant.ofEpochMilli(startMs).atZone(ZoneId.of("UTC")).toLocalDate()
                            val end = Instant.ofEpochMilli(endMs).atZone(ZoneId.of("UTC")).toLocalDate()
                            val label = if (start == end) start.toString() else "$start s/d $end"
                            onSelectPreset(RecapDatePreset.CUSTOM, start.toString(), end.toString(), label)
                        }
                        showCustomDateDialog = false
                    },
                    enabled = datePickerState.selectedStartDateMillis != null,
                ) { Text("Terapkan") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDateDialog = false }) { Text("Batal") }
            },
        ) {
            DateRangePicker(
                state = datePickerState,
                title = { Text("Pilih Rentang Tanggal", modifier = Modifier.padding(16.dp)) },
                modifier = Modifier.weight(1f),
            )
        }
    }

    LazyColumn(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        item {
            MicroLabel("Rentang Waktu")
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            ) {
                FilterChip(
                    selected = dateRange.preset == RecapDatePreset.TODAY,
                    onClick = { onSelectPreset(RecapDatePreset.TODAY, null, null, null) },
                    label = { Text("Hari Ini") },
                )
                FilterChip(
                    selected = dateRange.preset == RecapDatePreset.YESTERDAY,
                    onClick = { onSelectPreset(RecapDatePreset.YESTERDAY, null, null, null) },
                    label = { Text("Kemarin") },
                )
                FilterChip(
                    selected = dateRange.preset == RecapDatePreset.LAST_7_DAYS,
                    onClick = { onSelectPreset(RecapDatePreset.LAST_7_DAYS, null, null, null) },
                    label = { Text("7 Hari") },
                )
                FilterChip(
                    selected = dateRange.preset == RecapDatePreset.CUSTOM,
                    onClick = { showCustomDateDialog = true },
                    label = { Text(if (dateRange.preset == RecapDatePreset.CUSTOM) dateRange.label else "Pilih Tanggal 📅") },
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            if (summary == null) {
                Text(
                    text = if (isLoading) "Memuat rekap…" else "Belum ada data untuk periode ini.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                MicroLabel("Pendapatan (${dateRange.label})")
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
                Spacer(modifier = Modifier.height(10.dp))

                // Tombol Aksi: Cetak Rekap & Ekspor Excel
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Button(
                        onClick = onPrintRecap,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_print),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cetak Rekap")
                    }
                    OutlinedButton(
                        onClick = onExportCsv,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Ekspor Excel")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
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
                Spacer(modifier = Modifier.height(10.dp))
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
            MicroLabel("Riwayat Transaksi")
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
                Text(
                    text = "Memuat riwayat transaksi…",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
        } else if (txns.isEmpty()) {
            item {
                Text(
                    text = "Belum ada transaksi pada periode ini.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
        } else {
            items(txns, key = { it.id }) { tx ->
                TxnRow(tx = tx, onSelect = onSelectTxn, onReprint = onReprint)
            }
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
private fun TxnRow(
    tx: Transaction,
    onSelect: (Transaction) -> Unit,
    onReprint: (Transaction) -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onSelect(tx) },
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
