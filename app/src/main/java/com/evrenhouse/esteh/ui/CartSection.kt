package com.evrenhouse.esteh.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.evrenhouse.esteh.viewmodel.CartLine

/** Daftar keranjang: header + LazyColumn (key=id) + empty state + dialog kosongkan. */
@Composable
fun CartSection(
    cart: Map<String, CartLine>,
    onChangeQty: (String, Int) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmClear by remember { mutableStateOf(false) }
    val lines = cart.values.toList()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MicroLabel(
                if (lines.isEmpty()) "Keranjang"
                else "Keranjang • ${lines.sumOf { it.qty }} cup",
            )
            if (lines.isNotEmpty()) {
                TextButton(onClick = { confirmClear = true }) { Text("Kosongkan") }
            }
        }
        if (lines.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = EstehSpace.Lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Keranjang kosong",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(EstehSpace.Xxs))
                Text(
                    text = "Ketuk kartu menu untuk menambah.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(lines, key = { it.menuId }) { line ->
                    CartRow(line, onChangeQty, onRemove)
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Kosongkan keranjang?") },
            text = { Text("Semua item akan dihapus.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClear()
                }) { Text("Ya, kosongkan") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Batal") }
            },
        )
    }
}

/** Baris item: nama+harga kiri, stepper tengah, subtotal kanan, hapus. */
@Composable
private fun CartRow(
    line: CartLine,
    onChangeQty: (String, Int) -> Unit,
    onRemove: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = EstehSpace.Xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = line.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = line.price.toRp(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
            )
        }
        Spacer(modifier = Modifier.width(EstehSpace.Xs))
        Stepper(
            qty = line.qty,
            onMinus = { onChangeQty(line.menuId, -1) },
            onPlus = { onChangeQty(line.menuId, 1) },
        )
        Spacer(modifier = Modifier.width(EstehSpace.Xs))
        Text(
            text = line.subtotal.toRp(),
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End,
            modifier = Modifier.width(EstehSpace.Xl * 3),
        )
        IconButton(onClick = { onRemove(line.menuId) }) {
            Icon(Icons.Filled.Delete, contentDescription = "Hapus ${line.name}")
        }
    }
}

/** Stepper − / qty / + — semua target min 48dp. */
@Composable
private fun Stepper(qty: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(
            onClick = onMinus,
            modifier = Modifier.size(48.dp),
        ) {
            Text(text = "−", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            text = qty.toString(),
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = EstehSpace.Xxs),
        )
        TextButton(
            onClick = onPlus,
            modifier = Modifier.size(48.dp),
        ) {
            Text(text = "+", style = MaterialTheme.typography.titleLarge)
        }
    }
}
