package com.evrenhouse.esteh.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.evrenhouse.esteh.ui.theme.LocalEstehExtraColors
import com.evrenhouse.esteh.viewmodel.Snack
import com.evrenhouse.esteh.viewmodel.SnackKind

/**
 * Elemen krom khas web POS: label mikro mono uppercase, snackbar tinta.
 * Bukan Toast bawaan Android — M3 Snackbar dengan warna skema Es Teh.
 */
@Composable
fun MicroLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/**
 * Overlay snackbar terklasifikasi: hijau ✓ / merah ✕ / kuning ! / tinta i.
 * Kompak (maks 400dp, tengah, maks 2 baris), auto-hilang 3 detik, ketuk tutup.
 * Bukan Toast bawaan Android.
 */
@Composable
fun SnackOverlay(snack: Snack?, onDone: () -> Unit) {
    if (snack == null) return
    LaunchedEffect(snack) {
        kotlinx.coroutines.delay(3_000)
        onDone()
    }
    val extra = LocalEstehExtraColors.current
    val (container, onContainer, glyph) = when (snack.kind) {
        SnackKind.SUCCESS -> Triple(extra.cashContainer, extra.onCashContainer, "✓")
        SnackKind.ERROR -> Triple(extra.qrisContainer, extra.onQrisContainer, "✕")
        SnackKind.WARNING -> Triple(extra.warnContainer, extra.onWarnContainer, "!")
        SnackKind.INFO -> Triple(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary,
            "i",
        )
    }
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            color = container,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 6.dp,
            modifier = Modifier.widthIn(max = 400.dp).clickable(onClick = onDone),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier.size(24.dp).clip(CircleShape)
                        .background(onContainer.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = glyph,
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = onContainer,
                    )
                }
                Text(
                    text = snack.text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = onContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Double-bezel: cangkang low + inti kartu putih, sudut konsentris.
 * Semua kartu utama dibungkus ini, bukan flat di background.
 */
@Composable
fun EstehBezel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier,
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.padding(1.5.dp),
        ) { content() }
    }
}
