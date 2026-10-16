package com.evrenhouse.esteh.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.evrenhouse.esteh.data.printer.PrinterDevice
import com.evrenhouse.esteh.data.remote.dto.Settings

/** Pengaturan harga, struk, dan printer Bluetooth 58mm (android.md §2.8, §9). */
@Composable
fun SettingsDialog(
    current: Settings,
    showReceipt: Boolean,
    onToggleReceipt: (Boolean) -> Unit,
    selectedPrinter: PrinterDevice? = null,
    pairedPrinters: List<PrinterDevice> = emptyList(),
    autoPrintEnabled: Boolean = true,
    isPrinterConnected: Boolean = false,
    isPrinting: Boolean = false,
    onSelectPrinter: (PrinterDevice?) -> Unit = {},
    onToggleAutoPrint: (Boolean) -> Unit = {},
    onRefreshPrinters: () -> Unit = {},
    onTestPrint: () -> Unit = {},
    onRequestPermission: (() -> Unit)? = null,
    currentVersion: String = "0.4.0",
    isCheckingUpdate: Boolean = false,
    onCheckUpdate: () -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (Int, Int) -> Unit,
) {
    var esTeh by remember(current) { mutableStateOf(current.esTeh.toString()) }
    var lemonTea by remember(current) { mutableStateOf(current.lemonTea.toString()) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val esTehNum = esTeh.toIntOrNull()
    val lemonNum = lemonTea.toIntOrNull()
    val valid = esTehNum != null && lemonNum != null &&
        Settings(esTehNum, lemonNum).isValid()

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pengaturan") },
        text = {
            Column(modifier = Modifier.verticalScroll(scrollState)) {
                MicroLabel("Harga Jual")
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = esTeh,
                    onValueChange = { esTeh = it.filter(Char::isDigit).take(8) },
                    label = { Text("Es Teh") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = lemonTea,
                    onValueChange = { lemonTea = it.filter(Char::isDigit).take(8) },
                    label = { Text("Lemon Tea") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!valid) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Harga harus 1–10.000.000.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                MicroLabel("Tampilan Struk")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Tampilkan struk setelah bayar",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = showReceipt, onCheckedChange = onToggleReceipt)
                }

                Spacer(modifier = Modifier.height(16.dp))
                MicroLabel("Printer Struk 58mm")
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Cetak otomatis setelah bayar",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = autoPrintEnabled, onCheckedChange = onToggleAutoPrint)
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Printer Bluetooth Terpilih",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        onClick = {
                            onRequestPermission?.invoke()
                            onRefreshPrinters()
                            dropdownExpanded = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedPrinter?.name ?: "Pilih printer Bluetooth…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selectedPrinter != null) FontWeight.SemiBold else FontWeight.Normal,
                                )
                                if (selectedPrinter != null) {
                                    Text(
                                        text = selectedPrinter.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Text("▼", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.85f),
                    ) {
                        if (pairedPrinters.isEmpty()) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Belum ada perangkat ter-pair di Android. Tap untuk muat ulang.",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                },
                                onClick = {
                                    onRefreshPrinters()
                                    dropdownExpanded = false
                                },
                            )
                        } else {
                            pairedPrinters.forEach { device ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(device.name, style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                device.address,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    },
                                    onClick = {
                                        onSelectPrinter(device)
                                        dropdownExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val dotColor = when {
                            isPrinterConnected -> Color(0xFF2E7D32)
                            selectedPrinter != null -> Color(0xFF1565C0)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        val statusText = when {
                            isPrinterConnected -> "Terhubung"
                            selectedPrinter != null -> "Siap (on-demand)"
                            else -> "Belum dipilih"
                        }
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(dotColor, CircleShape),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            onRequestPermission?.invoke()
                            onTestPrint()
                        },
                        enabled = selectedPrinter != null && !isPrinting,
                    ) {
                        Text(if (isPrinting) "Mencetak…" else "Uji Cetak")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = "Versi Aplikasi",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "v$currentVersion",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    TextButton(
                        onClick = onCheckUpdate,
                        enabled = !isCheckingUpdate,
                    ) {
                        Text(if (isCheckingUpdate) "Memeriksa…" else "Periksa Pembaruan")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(esTehNum ?: 0, lemonNum ?: 0) },
                enabled = valid,
            ) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        },
    )
}
