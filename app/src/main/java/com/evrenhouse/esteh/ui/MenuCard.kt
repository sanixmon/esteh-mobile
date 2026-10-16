package com.evrenhouse.esteh.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/** Kartu menu 2 varian. Tap = tambah ke keranjang (§2.1–2.2). */
@Composable
fun MenuCard(name: String, price: Int, onTap: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onTap),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = price.toRp(),
                style = MaterialTheme.typography.labelLarge,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

/** Baris contoh kosong saat katalog belum ada (tak dipakai saat harga valid). */
@Composable
fun EmptyMenuHint(modifier: Modifier = Modifier) {
    Column(modifier.padding(16.dp)) {
        Text("Katalog kosong", style = MaterialTheme.typography.bodyMedium)
    }
}
