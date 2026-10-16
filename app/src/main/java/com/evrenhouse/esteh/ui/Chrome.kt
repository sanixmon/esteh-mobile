package com.evrenhouse.esteh.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

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

@Composable
fun EstehSnackbar(host: SnackbarHostState) {
    SnackbarHost(host) { data ->
        Snackbar(
            snackbarData = data,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            actionColor = MaterialTheme.colorScheme.onPrimary,
            dismissActionContentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(16.dp),
        )
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

/**
 * Entri bertahap: fade + geser naik dengan fisika pegas (transform/alpha saja,
 * murah di HP low-end). Index dibatasi agar daftar panjang tak antre lama.
 */
@Composable
fun StaggerIn(index: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(index) {
        kotlinx.coroutines.delay((index.coerceIn(0, 6) * 55).toLong())
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
            slideInVertically(
                spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.86f),
            ) { it / 6 },
    ) { content() }
}
