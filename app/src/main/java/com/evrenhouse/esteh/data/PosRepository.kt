package com.evrenhouse.esteh.data

import com.evrenhouse.esteh.data.local.TransactionDao
import com.evrenhouse.esteh.data.local.TransactionEntity
import com.evrenhouse.esteh.data.remote.ApiService
import com.evrenhouse.esteh.data.remote.dto.DaySummary
import com.evrenhouse.esteh.data.remote.dto.ErrorResponse
import com.evrenhouse.esteh.data.remote.dto.Transaction
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/** Hasil checkout: tersinkron ke backend, tersimpan lokal (offline), atau ditolak backend. */
sealed interface CheckoutResult {
    data class Synced(val transaction: Transaction) : CheckoutResult
    data class SavedOffline(val transaction: Transaction) : CheckoutResult
    data class Rejected(val message: String) : CheckoutResult
}

interface PosRepository {
    suspend fun getSummary(from: String? = null, to: String? = null, date: String? = null): Result<DaySummary>
    suspend fun getToday(from: String? = null, to: String? = null, date: String? = null): Result<List<Transaction>>
    suspend fun checkout(tx: Transaction): CheckoutResult
    /** Dorong antrean pending; kembalikan jumlah yang berhasil. Berhenti saat offline. */
    suspend fun syncPending(): Int
}

/** Network-first, Room-fallback (android.md §3, §8).
 *
 * Robustness:
 * - Write-ahead: baris pending ditulis SEBELUM post — crash di tengah POST
 *   tetap bisa retry dengan ID yang sama.
 * - Retry aman: backend `ON CONFLICT (id) DO NOTHING`, ID stabil per checkout.
 * - Single-flight per ID: checkout/sync konkuren untuk ID sama hanya POST sekali.
 * - 4xx = baris beracun: dibuang + Rejected (retry takkan sukses).
 * - 5xx/offline = transient: baris dipertahankan, sync berhenti.
 */
class PosRepositoryImpl(
    private val api: ApiService,
    private val txDao: TransactionDao,
    private val json: Json,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : PosRepository {

    private val scope = CoroutineScope(SupervisorJob() + io)
    private val flightMutex = Mutex()
    private val inflight = mutableMapOf<String, Deferred<SyncRowResult>>()

    override suspend fun getSummary(from: String?, to: String?, date: String?): Result<DaySummary> = withContext(io) {
        runCatching { api.getSummary(from = from, to = to, date = date).summary }
    }

    override suspend fun getToday(from: String?, to: String?, date: String?): Result<List<Transaction>> = withContext(io) {
        runCatching { api.getToday(from = from, to = to, date = date).transactions }
    }

    override suspend fun checkout(tx: Transaction): CheckoutResult {
        // Write-ahead: catat dulu sebagai pending sebelum menyentuh network.
        withContext(io) {
            bestEffort { txDao.upsert(tx.toEntity(pending = true)) }
        }
        return when (val r = postSingleFlight(tx)) {
            is SyncRowResult.Ok -> {
                withContext(io) { bestEffort { txDao.markSynced(tx.id) } }
                syncPending()
                CheckoutResult.Synced(r.tx)
            }
            is SyncRowResult.Rejected -> {
                withContext(io) { bestEffort { txDao.delete(tx.id) } }
                CheckoutResult.Rejected(r.message)
            }
            SyncRowResult.Transient -> CheckoutResult.SavedOffline(tx)
        }
    }

    override suspend fun syncPending(): Int = withContext(io) {
        var count = 0
        for (e in bestEffortList { txDao.pending() }) {
            val tx = try {
                json.decodeFromString<Transaction>(e.payload)
            } catch (_: Exception) {
                bestEffort { txDao.delete(e.id) } // payload korup = racun, buang
                continue
            }
            when (postSingleFlight(tx)) {
                is SyncRowResult.Ok -> {
                    bestEffort { txDao.markSynced(e.id) }
                    count++
                }
                is SyncRowResult.Rejected -> {
                    bestEffort { txDao.delete(e.id) }
                    count++ // racun dibuang: jangan blokir antrean
                }
                SyncRowResult.Transient -> break // masih offline/gangguan: berhenti
            }
        }
        count
    }

    /** Satu POST per ID dalam satu waktu; pemanggil konkuren menunggu hasil sama. */
    private suspend fun postSingleFlight(tx: Transaction): SyncRowResult {
        val d = flightMutex.withLock {
            inflight.getOrPut(tx.id) { scope.async { postRow(tx) } }
        }
        try {
            return d.await()
        } finally {
            flightMutex.withLock { if (inflight[tx.id] === d) inflight.remove(tx.id) }
        }
    }

    private sealed interface SyncRowResult {
        data class Ok(val tx: Transaction) : SyncRowResult
        data class Rejected(val message: String) : SyncRowResult
        data object Transient : SyncRowResult
    }

    private suspend fun postRow(tx: Transaction): SyncRowResult {
        return try {
            SyncRowResult.Ok(api.postTransaction(tx).transaction)
        } catch (_: IOException) {
            SyncRowResult.Transient
        } catch (h: HttpException) {
            if (h.code() in 500..599) {
                SyncRowResult.Transient
            } else {
                SyncRowResult.Rejected(parseError(h))
            }
        } catch (_: Exception) {
            // Termasuk gagal decode respons sukses: POST mungkin sudah masuk
            // (backend idempoten) → transient agar retry dengan ID sama.
            SyncRowResult.Transient
        }
    }

    private fun parseError(h: HttpException): String {
        val body = try {
            h.response()?.errorBody()?.string().orEmpty()
        } catch (_: Exception) {
            ""
        }
        val msg = try {
            json.decodeFromString<ErrorResponse>(body).error.ifBlank { null }
        } catch (_: Exception) {
            null
        }
        return msg ?: "Transaksi ditolak (${h.code()})"
    }

    private suspend fun <T> bestEffortList(block: suspend () -> List<T>): List<T> =
        try {
            block()
        } catch (_: Exception) {
            emptyList()
        }

    private suspend fun bestEffort(block: suspend () -> Unit) {
        try {
            block()
        } catch (_: Exception) {
        }
    }

    private fun Transaction.toEntity(pending: Boolean) = TransactionEntity(
        id = id,
        dateKey = dateKey,
        timestamp = timestamp,
        paymentMethod = paymentMethod,
        totalQty = totalQty,
        totalAmount = totalAmount,
        payload = json.encodeToString(this),
        pendingSync = pending,
        createdAtMs = System.currentTimeMillis(),
    )
}
