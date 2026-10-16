package com.evrenhouse.esteh.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.unit.dp
import com.evrenhouse.esteh.viewmodel.CartLine

/** Daftar keranjang: +/- qty, hapus item, kosongkan (dialog konfirmasi). */
@Composable
fun CartSection(
    cart: Map<String, CartLine>,
    onChangeQty: (String, Int) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmClear by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MicroLabel(if (cart.isEmpty()) "Keranjang" else "Keranjang • ${cart.values.sumOf { it.qty }} cup")
            if (cart.isNotEmpty()) {
                TextButton(onClick = { confirmClear = true }) { Text("Kosongkan") }
            }
        }
        if (cart.isEmpty()) {
            Text(
                text = "Ketuk kartu menu untuk menambah.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        } else {
            cart.values.forEach { line ->
                CartRow(line, onChangeQty, onRemove)
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

@Composable
private fun CartRow(
    line: CartLine,
    onChangeQty: (String, Int) -> Unit,
    onRemove: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = line.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "${line.qty} cup × ${line.price.toRp()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
            )
        }
        TextButton(
            onClick = { onChangeQty(line.menuId, -1) },
            modifier = Modifier.size(44.dp),
        ) { Text("−", style = MaterialTheme.typography.titleLarge) }
        Text(
            text = line.qty.toString(),
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = FontFamily.Monospace,
        )
        IconButton(
            onClick = { onChangeQty(line.menuId, 1) },
            modifier = Modifier.size(44.dp),
        ) { Icon(Icons.Filled.Add, contentDescription = "Tambah") }
        IconButton(
            onClick = { onRemove(line.menuId) },
            modifier = Modifier.size(44.dp),
        ) { Icon(Icons.Filled.Delete, contentDescription = "Hapus") }
        Spacer(modifier = Modifier.height(0.dp))
        Text(
            text = line.subtotal.toRp(),
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = FontFamily.Monospace,
        )
    }
}
