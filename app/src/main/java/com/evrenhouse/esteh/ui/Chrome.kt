package com.evrenhouse.esteh.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
