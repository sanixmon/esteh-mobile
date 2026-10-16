package com.evrenhouse.esteh.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evrenhouse.esteh.BuildConfig
import com.evrenhouse.esteh.ui.theme.LocalEstehExtraColors
import com.evrenhouse.esteh.viewmodel.ClockViewModel
import com.evrenhouse.esteh.viewmodel.PosViewModel
import com.evrenhouse.esteh.viewmodel.Sheet
import com.evrenhouse.esteh.viewmodel.assetUrl
import com.evrenhouse.esteh.viewmodel.menuCatalog

/**
 * Layar POS adaptif (§4): HP = kolom + bottom bar sticky + checkout sheet;
 * tablet landscape (≥840dp) = 2 kolom, checkout 400dp tanpa scroll halaman.
 * Struk/receipt ditunda: sukses checkout → snackbar + reset cart.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(vm: PosViewModel, clockVm: ClockViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }
    var showCheckout by remember { mutableStateOf(false) }
    val vmMessage = state.snackbar

    LaunchedEffect(vmMessage) {
        if (vmMessage != null) {
            snack.showSnackbar(vmMessage)
            vm.dismissSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { EstehSnackbar(snack) },
    ) { inner ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(inner),
        ) {
            val tablet = maxWidth >= 840.dp
            if (tablet) {
                Row(modifier = Modifier.fillMaxSize()) {
                    MenuPane(
                        vm = vm,
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    )
                    Surface(
                        tonalElevation = 3.dp,
                        modifier = Modifier.width(400.dp).fillMaxSize(),
                    ) {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            Header(vm, clockVm)
                            CartPane(vm)
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Header(vm, clockVm, modifier = Modifier.padding(horizontal = 16.dp))
                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                    ) {
                        MenuPane(vm = vm)
                    }
                    BottomBar(
                        state.totalQty,
                        state.totalAmount,
                        onOpen = { showCheckout = true },
                    )
                }
            }

            if (showCheckout && !tablet) {
                ModalBottomSheet(
                    onDismissRequest = { showCheckout = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    dragHandle = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            BottomSheetDefaults.DragHandle()
                            IconButton(onClick = { showCheckout = false }) {
                                Icon(Icons.Filled.Close, contentDescription = "Tutup")
                            }
                        }
                    },
                ) {
                    CartPane(vm, onDone = { showCheckout = false })
                }
            }

            when (state.sheet) {
                Sheet.RECAP -> {
                    if (tablet) {
                        Dialog(
                            onDismissRequest = vm::closeSheet,
                            properties = DialogProperties(usePlatformDefaultWidth = false),
                        ) {
                            Surface(
                                tonalElevation = 3.dp,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.width(660.dp).heightIn(max = 600.dp),
                            ) {
                                Column(modifier = Modifier.padding(vertical = 16.dp)) {
                                    SheetTitle("Rekap harian", vm::closeSheet)
                                    RecapContent(
                                        summary = state.summary,
                                        txns = state.filteredTxns,
                                        filter = state.recapFilter,
                                        isLoading = state.isLoadingRecap,
                                        onFilter = vm::setRecapFilter,
                                    )
                                }
                            }
                        }
                    } else {
                        ModalBottomSheet(
                            onDismissRequest = vm::closeSheet,
                            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                            ) {
                                SheetTitle("Rekap harian", vm::closeSheet)
                                RecapContent(
                                    summary = state.summary,
                                    txns = state.filteredTxns,
                                    filter = state.recapFilter,
                                    isLoading = state.isLoadingRecap,
                                    onFilter = vm::setRecapFilter,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
                Sheet.SETTINGS -> SettingsDialog(
                    current = state.prices,
                    showReceipt = state.showReceipt,
                    onToggleReceipt = vm::setShowReceipt,
                    onDismiss = vm::closeSheet,
                    onSave = vm::savePrices,
                )
                Sheet.RECEIPT -> {
                    val tx = state.receipt
                    if (tx != null) {
                        if (tablet) {
                            Dialog(
                                onDismissRequest = vm::newTransaction,
                                properties = DialogProperties(usePlatformDefaultWidth = false),
                            ) {
                                Surface(
                                    tonalElevation = 3.dp,
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier.width(480.dp).heightIn(max = 600.dp),
                                ) {
                                    Column(modifier = Modifier.padding(vertical = 16.dp)) {
                                        ReceiptContent(
                                            tx = tx,
                                            onPrint = vm::printReceipt,
                                            onNew = vm::newTransaction,
                                        )
                                    }
                                }
                            }
                        } else {
                            ModalBottomSheet(
                                onDismissRequest = vm::newTransaction,
                                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                            ) {
                                ReceiptContent(
                                    tx = tx,
                                    onPrint = vm::printReceipt,
                                    onNew = vm::newTransaction,
                                )
                            }
                        }
                    }
                }
                Sheet.NONE -> Unit
            }
        }
    }
}

@Composable
private fun Header(vm: PosViewModel, clockVm: ClockViewModel, modifier: Modifier = Modifier) {
    val state by vm.state.collectAsStateWithLifecycle()
    val extras = MaterialTheme.colorScheme // akses via LocalEstehExtraColors di bawah
    Column(modifier = modifier.padding(vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                MicroLabel("Kasir")
                Text(
                    text = "Es Teh Evren",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                ClockOf(clockVm)
            }
            if (state.offline) {
                OfflinePill()
            }
            TextButton(onClick = vm::refreshAll) { Text("Muat ulang") }
            TextButton(onClick = vm::openRecap) { Text("Rekap") }
            IconButton(onClick = vm::openSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Pengaturan")
            }
        }
    }
}

@Composable
private fun OfflinePill() {
    val extra = LocalEstehExtraColors.current
    Box(
        modifier = Modifier.clip(CircleShape).background(extra.warnContainer)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = "OFFLINE",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = extra.onWarnContainer,
        )
    }
}

@Composable
private fun MenuPane(vm: PosViewModel, modifier: Modifier = Modifier) {
    val state by vm.state.collectAsStateWithLifecycle()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MicroLabel("Katalog")
        menuCatalog(state.prices).forEachIndexed { i, (def, price) ->
            StaggerIn(i) {
                MenuCard(
                    name = def.name,
                    price = price,
                    imageUrl = assetUrl(BuildConfig.API_BASE_URL, def.asset),
                    onTap = { vm.addItem(def.menuId) },
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun CartPane(vm: PosViewModel, onDone: (() -> Unit)? = null) {
    val state by vm.state.collectAsStateWithLifecycle()
    CartSection(
        cart = state.cart,
        onChangeQty = vm::changeQty,
        onRemove = vm::removeItem,
        onClear = vm::clearCart,
    )
    CheckoutBar(
        payment = state.payment,
        totalQty = state.totalQty,
        totalAmount = state.totalAmount,
        canCheckout = state.canCheckout,
        isCompleting = state.isCompleting,
        onPayment = vm::setPayment,
        onCheckout = {
            vm.checkout()
            onDone?.invoke()
        },
    )
}

@Composable
private fun BottomBar(qty: Int, total: Int, onOpen: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                MicroLabel(if (qty == 0) "Keranjang kosong" else "$qty item")
                Text(
                    text = total.toRp(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                )
            }
            Button(
                onClick = onOpen,
                enabled = qty > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) { Text("Lihat & Bayar") }
        }
    }
}

@Composable
private fun SheetTitle(title: String, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Tutup") }
    }
}
