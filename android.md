# Spek Android — Kasir Es Teh Evren (Material 3 + Jetpack Compose)

Paritas penuh dengan web POS: flow sama, fitur sama. Backend dipakai ulang
(`https://esteh.evrenhouse.online/api/*` atau base URL yang sama saat self-host).

## 0. Lokasi & rilis

- Repo baru **`esteh-mobile`** (terpisah dari `kasir-mobile`; hanya paket
  `data/printer` yang disalin — lihat §9.2).
- `applicationId = "com.evrenhouse.esteh"`, min SDK 26, target SDK terbaru.
- `API_BASE_URL` via env/property Gradle (ikuti pola kasir-mobile, jangan
  hardcode). Signing release mengikuti pola keystore kasir-mobile.

## 1. Stack

| Kebutuhan      | Pilihan |
|---|---|
| Bahasa / UI    | Kotlin + Jetpack Compose (BOM terbaru) + Material 3 |
| Navigasi       | 1 Activity, tanpa NavGraph kompleks (2 destination: `pos`, opsional `recap` sebagai bottom sheet — lihat §4) |
| State          | ViewModel + StateFlow / `collectAsStateWithLifecycle` |
| Network        | Retrofit + OkHttp + kotlinx.serialization (atau Ktor client) |
| Offline mirror | Room (tabel `transactions`, `settings_kv`) — paritas fallback localStorage di web |
| Preferensi     | DataStore Preferences (`last_payment`, `prices`) |
| Format locale  | `NumberFormat("id","ID")`, `java.time` + `DateTimeFormatter("EEEE, d MMMM yyyy","id")` |
| Font           | Sistem saja: `FontFamily.SansSerif` + `FontFamily.Monospace`. Tanpa bundel font. |

Min SDK 26, target SDK terbaru. Tablet landscape + HP portrait didukung (§4).

## 2. Paritas fitur (wajib sama dengan web)

1. Katalog 2 menu (Es Teh, Lemon Tea) + foto, nama, kategori, harga dari settings.
   Foto SVG web dikonversi ke VectorDrawable (atau Coil + AndroidSVG) —
   SVG mentah tidak bisa dipakai langsung di Android.
2. Keranjang: tambah (tap kartu/tombol), +/- qty, hapus item, kosongkan (dialog konfirmasi).
3. Metode: Cash / QRIS, diingat per perangkat, otomatis diterapkan saat keranjang diisi.
4. Tanpa hitung kembalian. Cash = catat pemasukan sebesar total.
5. CTA hanya angka total (`Rp 15.000`), disabled sampai item + metode terisi.
6. Struk sukses (modal): rincian item, total cup, metode, total akhir, tombol Cetak + Transaksi Baru (Space/Enter diganti aksi tombol; tidak ada shortcut keyboard di Android).
7. Rekap harian (bottom sheet di HP, dialog di tablet): hero total pendapatan + `x Transaksi • x Cup`, kartu Cash, kartu QRIS, rincian menu (chip wrap), riwayat + filter Semua/Cash/QRIS, nota pendek `#XXXX` (full ID di long-press/copy), empty state, loading shimmer.
8. Pengaturan harga (default Es Teh 10.000 / Lemon Tea 11.000): tersimpan ke backend, fallback lokal. Validasi 1–10.000.000.
9. Snackbar (pengganti toast web): item ditambah/dihapus, harga disimpan, offline notice.
10. Tanpa fitur reset harian (sudah dihapus di web — jangan dibawa).
11. Tanpa gambar QRIS dan tanpa teks instruksi (sudah dihapus di web — QRIS hanya badge + nominal).

## 3. Flow (target ≤ 2 tap per penjualan)

```
Tambah (tap kartu) → [metode otomatis dari preferensi] → tap Total → Struk
```

- Kosong: CTA `Rp 0` disabled.
- Sukses: simpan via Repository → refresh summary → tampil struk → Transaksi Baru (reset cart + tutup sheet di HP).
- Gagal network: simpan ke Room + tandai `pendingSync`, Worker sync saat online; badge offline di header ("Offline — tersimpan lokal").

## 4. Layout

- **HP (< 600dp)**: kolom menu di atas; checkout = ModalBottomSheet (handle + tombol tutup eksplisit ✕, backdrop tap = tutup); bottom bar sticky (x item, total, tombol Lihat & Bayar).
- **600–840dp** (tablet kecil/foldable): pakai layout HP.
- **Tablet landscape (≥ 840dp)**: 2 kolom (`menu 1fr | checkout 400dp`), tanpa scroll halaman, daftar keranjang scroll internal.
- Rekap di HP = bottom sheet `92%` tinggi; di tablet = dialog `660dp`.
- Area dinamis Cash/QRIS tingginya dikunci (±76dp visual) agar CTA tidak loncat saat ganti metode.

## 5. Design token → M3 (custom scheme wajib)

Dilarang pakai `lightColorScheme()` bawaan M3 (bernada ungu `#FEF7FF`).
Definisikan custom scheme dengan nilai eksak web:

| Web | M3 custom |
|---|---|
| canvas `#FBFBFA` | `surface` |
| subtle `#F7F6F3` | `surfaceContainerLow` |
| card `#FFFFFF` | `surfaceContainer` = `#FFFFFF`, `surface` = `#FBFBFA` |
| line `#EAEAEA` | `outlineVariant` |
| ink `#111111` | `onSurface`; `primary = #111111`, `onPrimary = #FFFFFF` |
| muted `#787774` | `onSurfaceVariant` |
| pastel green `#EDF3EC`/`#346538` | custom `cashContainer` + `onCashContainer` |
| pastel red `#FDEBEC`/`#9F2F2D` | custom `qrisContainer` + `onQrisContainer` |
| pastel blue `#E1F3FE`/`#1F6C9F` | custom `infoContainer` + `onInfoContainer` |
| pastel yellow `#FBF3DB`/`#956400` | custom `warnContainer` + `onWarnContainer` |

Definisikan keempat container di atas via `CompositionLocal` (jangan pakai
`tertiaryContainer` bawaan yang ungu).
| Pill mono uppercase 11sp | `labelSmall` + `FontFamily.Monospace` |
| Radius 6/8/12/9999 | `Shapes`: small 6dp, medium 8/12dp, pill `CircleShape` |
| Touch target ≥ 44dp | tombol qty/hapus/close minimal 40–48dp |

Elevation: kartu 0–1dp, sheet/modal 3dp. Tanpa blur (performa HP murah).

## 6. Model data (Kotlin)

```kotlin
@Serializable
data class MenuItem(val menuId: String, val name: String, val price: Int,
                    val qty: Int, val subtotal: Int)

@Serializable
data class Transaction(
  val id: String,                 // TRX-YYYYMMDD-XXXX
  val timestamp: String,          // ISO-8601
  val dateKey: String,            // yyyy-MM-dd (zona Asia/Jakarta)
  val timeString: String,         // HH:mm:ss
  val dateString: String,         // "Kamis, 15 Oktober 2026"
  val items: List<MenuItem>,
  val totalQty: Int,
  val totalAmount: Int,
  val paymentMethod: String,      // "cash" | "qris"
  val cashReceived: Int? = null,
  val change: Int? = null,
)

@Serializable
data class DaySummary(
  val totalTransactions: Int, val totalRevenue: Int, val totalCups: Int,
  val perMenu: Map<String, PerMenu>,   // id -> { name, qty, revenue }
  val cash: PayStats, val qris: PayStats,
)

@Serializable
data class PerMenu(val name: String, val qty: Int, val revenue: Int)

@Serializable
data class PayStats(val count: Int, val revenue: Int)

@Serializable
data class Settings(
  @SerialName("es-teh") val esTeh: Int,
  @SerialName("lemon-tea") val lemonTea: Int,
)
```

## 7. Kontrak API (dipakai ulang; pengecualian antrean cetak §9.3)

Endpoint operasional di bawah ini dipakai apa adanya — jangan ubah backend
untuknya. Antrean cetak (§9.3) adalah tahap 2 dan memang butuh perubahan
backend, tercantum eksplisit di sana.

- `GET /api/summary` → `{ summary: DaySummary }` — badge + angka rekap (ringan).
- `GET /api/today` → `{ transactions: [Transaction] }` — hanya saat rekap dibuka.
- `POST /api/transactions` (body `Transaction` tanpa validasi kembalian) → `201 { transaction }` / `400 { error }`.
- `GET /api/settings` → `{ settings }`, `PUT /api/settings` → `{ settings }` / `400`.
- Semua `date_key` dihitung server dalam WIB — aplikasi kirim `dateKey` lokal tapi server yang menentukan hari via `CURRENT_DATE`.

## 8. Arsitektur

```
ui/ (PosScreen, CartSection, MenuCard, ReceiptSheet, RecapSheet, SettingsDialog)
viewmodel/ PosViewModel (UIState: cart, payment, prices, daySummary, txns, sheets, snackbar)
data/ PosRepository (network-first, Room-fallback) + SettingsRepository
data/local/ (Room DB, DataStore)   data/remote/ (Retrofit ApiService, dto)
```

- Satu `UiState` data class; event via fungsi ViewModel (`addItem`, `setPayment`, `checkout`, `openRecap`, `savePrices`).
- Jam: `StateFlow` jam di ViewModel terpisah / `LaunchedEffect` di composable `Clock` kecil — dilarang re-render seluruh layar tiap detik.
- Checkout: guard ganda (tombol disabled + cek di ViewModel), cegah double-tap (`isCompleting`).

## 9. Cetak struk 58mm (ikuti kasir-mobile)

Referensi: `~/mobile/kasir-mobile/app/src/main/java/com/kasir/mobile/data/printer/`
(7 file: `PrinterTransport`, `PrinterManager`, `PrinterRepository`,
`EscPosEncoder`, `ReceiptFormatter`, `PrinterModels`, `PrinterCharset`).

### 9.1 Cara terhubung (hasil reverse dari kasir-mobile)

- Transport: **Bluetooth Classic SPP (RFCOMM)**, bukan BLE. Printer 58mm
  (profil `X583_V2`, nama BT `RP02N`) di-pair dulu di Settings Android.
- Koneksi: `BluetoothDevice.createRfcommSocketToServiceRecord(SPP_UUID
  00001101-0000-1000-8000-00805F9B34FB)` → `connect()` di `Dispatchers.IO`.
  Baud tidak relevan di RFCOMM. Izin: `BLUETOOTH_CONNECT` (+ `BLUETOOTH_SCAN`
  untuk discovery, target SDK 31+).
- Data: bytes ESC/POS ditulis ke `socket.outputStream` + `flush()`.
  Semua job diserialkan via `Mutex` (satu struk dalam satu waktu).
- Perintah yang dipakai: `ESC @` init, `ESC a n` align, `ESC E n` bold,
  `GS ! n` size, `ESC d n` feed, `GS V 0` cut. Command QR model 2
  (`GS ( k ...`) tersedia di `EscPosEncoder` tapi **tidak dipakai** untuk
  struk Es Teh (paritas web: QR dihapus).
- Kertas 58mm = **32 kolom**; encoding profil `GBK` (teks warung latin aman
  via ASCII printable; non-latin diganti `?`).
- Layout struk Es Teh (cermin `ReceiptFormatter`, tipe baru `ES_TEH`):
  header center bold double-tinggi nama toko → tanggal/jam/id center →
  dashes → baris item (`Nama xQty  RpSubtotal`, harga rata kanan kolom
  terakhir, wrap per kata) + sub-baris `N cup x RpHarga` → dashes →
  `Total Cup:` / `Bayar: CASH/QRIS` / bold `TOTAL:` → dashes → feed →
  `Terima kasih!` → feed → cut. Tanpa QR (paritas web: QR dihapus).

### 9.2 Reuse, jangan tulis ulang

- Salin utuh paket `data/printer` kasir-mobile ke modul/app ini (namespace
  disesuaikan). Tambah `ReceiptType.ES_TEH` + branch format di
  `ReceiptFormatter.toCommands` mengikuti §9.1.
- `PrinterRepository` dipakai untuk simpan printer terpilih (MAC + nama)
  dan state koneksi (`Disconnected/Connecting/Connected/Error`).
- `ReceiptFormatterTest`-style unit test wajib untuk `twoCol`/`wrapWords`/
  layout struk Es Teh (kasir-mobile sudah punya polanya).

### 9.3 Alur cetak (dua jalur — tahap 2, butuh perubahan backend)

Browser tidak bisa SPP (Web Bluetooth hanya BLE GATT), jadi web tidak
konek langsung ke printer. App ini berperan ganda: kasir + bridge.

**Jalur A — struk lokal (utama):** setelah checkout sukses di app ini,
langsung `BluetoothPrinterManager.printReceipt` ke printer terpair.
Tanpa antrean, tanpa polling.

**Jalur B — job dari web (tahap 2):** perlu tabel `print_jobs` +
endpoint baru di backend, lalu polling:

```
Web [Cetak Struk] → POST /api/print (snapshot transaksi)
  → backend format ESC/POS → tabel print_jobs (queued)
App ini polling GET /api/print-queue tiap 2–3 dtk
  (foreground service — WorkManager terlalu lambat, min 15 mnt)
  → BluetoothPrinterManager.printReceipt → POST /api/print-ack
Web: jika backend offline → fallback window.print() (browser)
```

- Kontrak antrean (backend, **belum diimplementasikan** — bagian dari tahap 2):
  `POST /api/print { transaction } → { jobId }`,
  `GET /api/print-queue → { jobs: [{ jobId, payloadBase64 }] }`,
  `POST /api/print-ack { jobId, ok, error? }`.
- Payload = bytes §9.1, base64. Status job: `queued → done | error`.
  Retry maks 3x lalu tandai `error` + tampilkan di layar printer; job
  `done` tidak diambil lagi.
- Izin: `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN` (discovery),
  `FOREGROUND_SERVICE` + `POST_NOTIFICATIONS` (notifikasi persisten service).
- Layar Pengaturan printer di app: daftar device paired, tombol
  Hubungkan/Uji cetak, indikator status koneksi.

## 10. Kriteria terima
- [ ] Rilis debug di HP + tablet: 2-tap sale Cash & QRIS tercatat di backend.
- [ ] Matikan internet: jualan tetap jalan, tersimpan lokal, tersinkron saat online.
- [ ] Rekap HP tanpa scroll horizontal; filter Cash/QRIS benar; nota pendek + long-press copy full ID.
- [ ] Ganti harga di Pengaturan → transaksi baru pakai harga baru, nota lama tidak berubah.
- [ ] Tidak ada full-recompose tiap detik (cek Layout Inspector), tidak ada blur, font hanya sistem sans + mono.
- [ ] Rotasi layar / resize tidak menghilangkan cart.
- [ ] Printer: pair RP02N → uji cetak keluar struk Es Teh 32 kolom;
  job web (tahap 2) tercetak ≤ 10 dtk setelah tap Cetak.
