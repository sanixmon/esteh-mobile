package com.evrenhouse.esteh.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.evrenhouse.esteh.data.remote.dto.Settings

/** Pengaturan harga, default 10.000/11.000, validasi 1–10.000.000 (§2.8). */
@Composable
fun SettingsDialog(
    current: Settings,
    onDismiss: () -> Unit,
    onSave: (Int, Int) -> Unit,
) {
    var esTeh by remember(current) { mutableStateOf(current.esTeh.toString()) }
    var lemonTea by remember(current) { mutableStateOf(current.lemonTea.toString()) }
    val esTehNum = esTeh.toIntOrNull()
    val lemonNum = lemonTea.toIntOrNull()
    val valid = esTehNum != null && lemonNum != null &&
        Settings(esTehNum, lemonNum).isValid()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pengaturan harga") },
        text = {
            Column {
                OutlinedTextField(
                    value = esTeh,
                    onValueChange = { esTeh = it.filter(Char::isDigit).take(8) },
                    label = { Text("Es Teh") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = lemonTea,
                    onValueChange = { lemonTea = it.filter(Char::isDigit).take(8) },
                    label = { Text("Lemon Tea") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!valid) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Harga harus 1–10.000.000.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(esTehNum ?: 0, lemonNum ?: 0) },
                enabled = valid,
            ) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        },
    )
}
