package com.evrenhouse.esteh.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.evrenhouse.esteh.data.remote.dto.Transaction

/**
 * Area pembayaran + CTA total. Tinggi visual dikunci agar CTA tak loncat (§4).
 * CTA hanya angka total, disabled sampai item + metode terisi (§2.5).
 */
@Composable
fun CheckoutBar(
    payment: String?,
    totalQty: Int,
    totalAmount: Int,
    canCheckout: Boolean,
    isCompleting: Boolean,
    onPayment: (String) -> Unit,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        // Area dinamis dikunci 76dp agar CTA tak loncat saat ganti metode (§4).
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().height(76.dp),
        ) {
            PaymentChip("Cash", Transaction.CASH, payment, onPayment)
            PaymentChip("QRIS", Transaction.QRIS, payment, onPayment)
        }
        Button(
            onClick = onCheckout,
            enabled = canCheckout,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(
                text = if (isCompleting) "Memproses…" else totalAmount.toRp(),
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Monospace,
            )
        }
        if (totalQty > 0) {
            Text(
                text = "$totalQty item",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun PaymentChip(label: String, value: String, selected: String?, onSelect: (String) -> Unit) {
    FilterChip(
        selected = selected == value,
        onClick = { onSelect(value) },
        label = { Text(label) },
    )
}
