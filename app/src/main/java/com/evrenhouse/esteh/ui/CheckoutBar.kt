package com.evrenhouse.esteh.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.evrenhouse.esteh.data.remote.dto.Transaction

/**
 * Area pembayaran + CTA total. Tinggi visual dikunci agar CTA tak loncat (§4).
 * CTA hanya angka total, disabled sampai item + metode terisi (§2.5).
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    val options = listOf("Cash" to Transaction.CASH, "QRIS" to Transaction.QRIS)
    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        MicroLabel("Metode pembayaran")
        Spacer(modifier = Modifier.height(6.dp))
        // Area dinamis dikunci 76dp agar CTA tak loncat saat ganti metode (§4).
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().height(76.dp)) {
            options.forEachIndexed { i, (label, value) ->
                SegmentedButton(
                    selected = payment == value,
                    onClick = { onPayment(value) },
                    shape = SegmentedButtonDefaults.itemShape(i, options.size),
                    label = { Text(label) },
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onCheckout,
            enabled = canCheckout,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            shape = CircleShape,
            contentPadding = PaddingValues(start = 20.dp, end = 8.dp),
        ) {
            Text(
                text = if (isCompleting) "MEMPROSES…" else totalAmount.toRp(),
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            // Ikon panah bersarang di lingkaran sendiri (button-in-button).
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "→",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
        if (totalQty > 0) {
            Text(
                text = "$totalQty ITEM",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
