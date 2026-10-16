package com.evrenhouse.esteh.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
 * Footer keranjang sticky: total dominan, metode segmented, tombol bayar
 * "Bayar Rp X" + alasan saat disabled (§2.5).
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
    val reason = when {
        totalQty == 0 -> "Pilih menu dulu"
        payment == null -> "Pilih metode bayar"
        else -> null
    }
    Column(modifier = modifier.fillMaxWidth()) {
        MicroLabel("Total")
        Text(
            text = totalAmount.toRp(),
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(EstehSpace.Xs))
        MicroLabel("Metode pembayaran")
        Spacer(modifier = Modifier.height(EstehSpace.Xxs))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, (label, value) ->
                SegmentedButton(
                    selected = payment == value,
                    onClick = { onPayment(value) },
                    shape = SegmentedButtonDefaults.itemShape(i, options.size),
                    label = { Text(label, style = MaterialTheme.typography.bodyLarge) },
                )
            }
        }
        Spacer(modifier = Modifier.height(EstehSpace.Sm))
        Button(
            onClick = onCheckout,
            enabled = canCheckout,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(
                text = if (isCompleting) "MEMPROSES…" else "Bayar ${totalAmount.toRp()}",
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
        }
        if (reason != null) {
            Spacer(modifier = Modifier.height(EstehSpace.Xxs))
            Text(
                text = reason,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
