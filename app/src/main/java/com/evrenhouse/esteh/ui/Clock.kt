package com.evrenhouse.esteh.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import com.evrenhouse.esteh.data.remote.WibTime
import java.time.ZonedDateTime

/** Jam kecil mandiri — satu-satunya composable yang re-compose tiap tick (§8). */
@Composable
fun Clock(now: ZonedDateTime, mono: Boolean = true) {
    Text(
        text = WibTime.dateString(now) + " • " + WibTime.timeString(now),
        style = MaterialTheme.typography.labelMedium,
        fontFamily = if (mono) FontFamily.Monospace else null,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
