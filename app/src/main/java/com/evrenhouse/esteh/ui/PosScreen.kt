package com.evrenhouse.esteh.ui
import android.content.Intent
import android.net.Uri
import com.evrenhouse.esteh.BuildConfig
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.evrenhouse.esteh.data.printer.PrinterDevice
import com.evrenhouse.esteh.data.RecapCsvExporter
import com.evrenhouse.esteh.data.remote.dto.Transaction
import com.evrenhouse.esteh.viewmodel.RecapDatePreset
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evrenhouse.esteh.ui.theme.LocalEstehExtraColors
import com.evrenhouse.esteh.viewmodel.ClockViewModel
import com.evrenhouse.esteh.viewmodel.PosViewModel
import com.evrenhouse.esteh.viewmodel.RecapFilter
import com.evrenhouse.esteh.viewmodel.Sheet
import com.evrenhouse.esteh.viewmodel.UiState
import com.evrenhouse.esteh.viewmodel.assetUrl
import com.evrenhouse.esteh.viewmodel.menuCatalog
import java.time.ZonedDateTime

/**
 * Layar kasir: TopAppBar + katalog | keranjang.
 * ≥600dp: split Row (katalog 0.62 / keranjang 0.38 + VerticalDivider).
 * <600dp: kolom (katalog atas, keranjang bawah).
 */
@Composable
fun PosScreen(vm: PosViewModel, clockVm: ClockViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val now by clockVm.now.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val bluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            vm.refreshPrinters()
        }
    }

    val checkAndRequestBluetooth = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                bluetoothLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                vm.refreshPrinters()
            }
        } else {
            vm.refreshPrinters()
        }
    }

    PosScreenContent(
        state = state,
        now = now,
        onAdd = vm::addItem,
        onChangeQty = vm::changeQty,
        onRemove = vm::removeItem,
        onClear = vm::clearCart,
        onPayment = vm::setPayment,
        onCheckout = vm::checkout,
        onNewTransaction = vm::newTransaction,
        onOpenRecap = vm::openRecap,
        onCloseSheet = vm::closeSheet,
        onOpenSettings = {
            checkAndRequestBluetooth()
            vm.openSettings()
        },
        onRefresh = vm::refreshAll,
        onFilter = vm::setRecapFilter,
        onSavePrices = vm::savePrices,
        onToggleReceipt = vm::setShowReceipt,
        onPrint = { vm.printReceipt() },
        onReprint = { tx -> vm.printReceipt(tx, isReprint = true) },
        onSelectRecapPreset = { preset, from, to, label ->
            vm.setRecapDatePreset(preset, from, to, label)
        },
        onPrintRecap = vm::printRecapReceipt,
        onExportCsv = {
            val shareIntent = RecapCsvExporter.shareCsv(
                context,
                state.filteredTxns,
                state.summary,
                state.recapDateRange.label
            )
            context.startActivity(shareIntent)
        },
        onSelectTxn = vm::viewTransactionDetail,
        onSelectPrinter = vm::selectPrinter,
        onToggleAutoPrint = vm::setAutoPrint,
        onRefreshPrinters = checkAndRequestBluetooth,
        onTestPrint = vm::testPrint,
        onRequestPermission = checkAndRequestBluetooth,
        onCheckUpdate = { vm.checkUpdate(manual = true) },
        onDismissUpdate = vm::dismissUpdate,
        onDismissSnackbar = vm::dismissSnackbar,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreenContent(
    state: UiState,
    now: ZonedDateTime,
    onAdd: (String) -> Unit,
    onChangeQty: (String, Int) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    onPayment: (String) -> Unit,
    onCheckout: () -> Unit,
    onNewTransaction: () -> Unit,
    onOpenRecap: () -> Unit,
    onCloseSheet: () -> Unit,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
    onFilter: (RecapFilter) -> Unit,
    onSavePrices: (Int, Int) -> Unit,
    onToggleReceipt: (Boolean) -> Unit,
    onPrint: () -> Unit,
    onReprint: (Transaction) -> Unit = {},
    onSelectRecapPreset: (RecapDatePreset, String?, String?, String?) -> Unit = { _, _, _, _ -> },
    onPrintRecap: () -> Unit = {},
    onExportCsv: () -> Unit = {},
    onSelectTxn: (Transaction) -> Unit = {},
    onSelectPrinter: (PrinterDevice?) -> Unit = {},
    onToggleAutoPrint: (Boolean) -> Unit = {},
    onRefreshPrinters: () -> Unit = {},
    onTestPrint: () -> Unit = {},
    onRequestPermission: (() -> Unit)? = null,
    onCheckUpdate: () -> Unit = {},
    onDismissUpdate: () -> Unit = {},
    onDismissSnackbar: () -> Unit,
) {
    Scaffold(
        topBar = {
            PosTopBar(
                now = now,
                offline = state.offline,
                onRefresh = onRefresh,
                onOpenRecap = onOpenRecap,
                onOpenSettings = onOpenSettings,
            )
        },
        snackbarHost = { SnackOverlay(state.snackbar, onDismissSnackbar) },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { inner ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(inner),
        ) {
            if (maxWidth < 600.dp) {
                Column(modifier = Modifier.fillMaxSize()) {
                    CatalogGrid(
                        state = state,
                        onAdd = onAdd,
                        modifier = Modifier.weight(1f),
                    )
                    HorizontalDivider()
                    CartPane(
                        state = state,
                        onChangeQty = onChangeQty,
                        onRemove = onRemove,
                        onClear = onClear,
                        onPayment = onPayment,
                        onCheckout = onCheckout,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    CatalogGrid(
                        state = state,
                        onAdd = onAdd,
                        modifier = Modifier.weight(0.62f).fillMaxHeight(),
                    )
                    VerticalDivider()
                    CartPane(
                        state = state,
                        onChangeQty = onChangeQty,
                        onRemove = onRemove,
                        onClear = onClear,
                        onPayment = onPayment,
                        onCheckout = onCheckout,
                        modifier = Modifier.weight(0.38f).fillMaxHeight(),
                    )
                }
            }

            when (state.sheet) {
                Sheet.RECAP -> {
                    if (maxWidth < 600.dp) {
                        ModalBottomSheet(
                            onDismissRequest = onCloseSheet,
                            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                        ) {
                            SheetTitle("Rekap harian", onCloseSheet)
                            RecapContent(
                                summary = state.summary,
                                txns = state.filteredTxns,
                                filter = state.recapFilter,
                                dateRange = state.recapDateRange,
                                isLoading = state.isLoadingRecap,
                                onFilter = onFilter,
                                onSelectPreset = onSelectRecapPreset,
                                onPrintRecap = onPrintRecap,
                                onExportCsv = onExportCsv,
                                onSelectTxn = onSelectTxn,
                                onReprint = onReprint,
                            )
                        }
                    } else {
                        Dialog(
                            onDismissRequest = onCloseSheet,
                            properties = DialogProperties(usePlatformDefaultWidth = false),
                        ) {
                            Surface(
                                tonalElevation = 3.dp,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.width(660.dp).heightIn(max = 600.dp),
                            ) {
                                Column(modifier = Modifier.padding(vertical = EstehSpace.Md)) {
                                    SheetTitle("Rekap harian", onCloseSheet)
                                    RecapContent(
                                        summary = state.summary,
                                        txns = state.filteredTxns,
                                        filter = state.recapFilter,
                                        dateRange = state.recapDateRange,
                                        isLoading = state.isLoadingRecap,
                                        onFilter = onFilter,
                                        onSelectPreset = onSelectRecapPreset,
                                        onPrintRecap = onPrintRecap,
                                        onExportCsv = onExportCsv,
                                        onSelectTxn = onSelectTxn,
                                        onReprint = onReprint,
                                    )
                                }
                            }
                        }
                    }
                }
                Sheet.SETTINGS -> SettingsDialog(
                    current = state.prices,
                    showReceipt = state.showReceipt,
                    onToggleReceipt = onToggleReceipt,
                    selectedPrinter = state.selectedPrinter,
                    pairedPrinters = state.pairedPrinters,
                    autoPrintEnabled = state.autoPrintEnabled,
                    isPrinterConnected = state.isPrinterConnected,
                    isPrinting = state.isPrinting,
                    onSelectPrinter = onSelectPrinter,
                    onToggleAutoPrint = onToggleAutoPrint,
                    onRefreshPrinters = onRefreshPrinters,
                    onTestPrint = onTestPrint,
                    onRequestPermission = onRequestPermission,
                    currentVersion = BuildConfig.VERSION_NAME,
                    isCheckingUpdate = state.isCheckingUpdate,
                    onCheckUpdate = onCheckUpdate,
                    onDismiss = onCloseSheet,
                    onSave = onSavePrices,
                )
                Sheet.RECEIPT -> {
                    val tx = state.receipt
                    if (tx != null) {
                        if (maxWidth < 600.dp) {
                            ModalBottomSheet(
                                onDismissRequest = onNewTransaction,
                                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                            ) {
                                ReceiptContent(tx = tx, onPrint = onPrint, onNew = onNewTransaction)
                            }
                        } else {
                            Dialog(
                                onDismissRequest = onNewTransaction,
                                properties = DialogProperties(usePlatformDefaultWidth = false),
                            ) {
                                Surface(
                                    tonalElevation = 3.dp,
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier.width(480.dp).heightIn(max = 600.dp),
                                ) {
                                    Column(modifier = Modifier.padding(vertical = EstehSpace.Md)) {
                                        ReceiptContent(tx = tx, onPrint = onPrint, onNew = onNewTransaction)
                                    }
                                }
                            }
                        }
                    }
                }
                Sheet.NONE -> Unit
            }

            val update = state.availableUpdate
            if (update != null) {
                val ctx = LocalContext.current
                UpdateDialog(
                    info = update,
                    currentVersion = BuildConfig.VERSION_NAME,
                    onConfirm = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(update.apkUrl)).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            ctx.startActivity(intent)
                        } catch (_: Exception) {}
                        onDismissUpdate()
                    },
                    onDismiss = onDismissUpdate,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PosTopBar(
    now: ZonedDateTime,
    offline: Boolean,
    onRefresh: () -> Unit,
    onOpenRecap: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Es Teh Evren",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Clock(now = now)
            }
        },
        actions = {
            if (offline) {
                OfflinePill()
            }
            TextButton(onClick = onRefresh) { Text("Muat ulang") }
            TextButton(onClick = onOpenRecap) { Text("Rekap") }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Pengaturan")
            }
        },
    )
}

@Composable
private fun OfflinePill() {
    val extra = LocalEstehExtraColors.current
    Surface(
        color = extra.warnContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = "OFFLINE",
            style = MaterialTheme.typography.labelMedium,
            fontFamily = FontFamily.Monospace,
            color = extra.onWarnContainer,
            modifier = Modifier.padding(
                horizontal = EstehSpace.Sm,
                vertical = EstehSpace.Xs,
            ),
        )
    }
}

/** Katalog grid adaptif: kartu vertikal key=id menu. */
@Composable
private fun CatalogGrid(
    state: UiState,
    onAdd: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 168.dp),
        contentPadding = PaddingValues(EstehSpace.Md),
        horizontalArrangement = Arrangement.spacedBy(EstehSpace.Sm),
        verticalArrangement = Arrangement.spacedBy(EstehSpace.Sm),
        modifier = modifier.fillMaxWidth(),
    ) {
        items(
            items = menuCatalog(state.prices),
            key = { (def, _) -> def.menuId },
        ) { (def, price) ->
            MenuCard(
                name = def.name,
                price = price,
                imageUrl = assetUrl(BuildConfig.API_BASE_URL, def.asset),
                onTap = { onAdd(def.menuId) },
            )
        }
    }
}

/** Panel keranjang: daftar (Lazy, key=id) + footer sticky. */
@Composable
private fun CartPane(
    state: UiState,
    onChangeQty: (String, Int) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    onPayment: (String) -> Unit,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(
            start = EstehSpace.Md,
            end = EstehSpace.Md,
            top = EstehSpace.Md,
            bottom = EstehSpace.Md,
        ),
    ) {
        CartSection(
            cart = state.cart,
            onChangeQty = onChangeQty,
            onRemove = onRemove,
            onClear = onClear,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.height(EstehSpace.Sm))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(EstehSpace.Sm))
        CheckoutBar(
            payment = state.payment,
            totalQty = state.totalQty,
            totalAmount = state.totalAmount,
            canCheckout = state.canCheckout,
            isCompleting = state.isCompleting,
            onPayment = onPayment,
            onCheckout = onCheckout,
        )
    }
}

@Composable
private fun SheetTitle(title: String, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = EstehSpace.Md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = "Tutup")
        }
    }
}
