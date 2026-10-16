package com.evrenhouse.esteh.data

import com.evrenhouse.esteh.data.local.TransactionDao
import com.evrenhouse.esteh.data.local.TransactionEntity
import com.evrenhouse.esteh.data.remote.ApiService
import com.evrenhouse.esteh.data.remote.dto.DaySummary
import com.evrenhouse.esteh.data.remote.dto.Transaction
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.withContext

/** Hasil checkout: tersinkron ke backend atau tersimpan lokal (offline). */
sealed interface CheckoutResult {
    data class Synced(val transaction: Transaction) : CheckoutResult
    data class SavedOffline(val transaction: Transaction) : CheckoutResult
}

interface PosRepository {
    suspend fun getSummary(): Result<DaySummary>
    suspend fun getToday(): Result<List<Transaction>>
    suspend fun checkout(tx: Transaction): CheckoutResult
    /** Dorong antrean pending; kembalikan jumlah yang berhasil. Berhenti saat offline. */
    suspend fun syncPending(): Int
}

/** Network-first, Room-fallback (android.md §3, §8). */
class PosRepositoryImpl(
    private val api: ApiService,
    private val txDao: TransactionDao,
    private val json: Json,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : PosRepository {

    override suspend fun getSummary(): Result<DaySummary> = withContext(io) {
        runCatching { api.getSummary().summary }
    }

    override suspend fun getToday(): Result<List<Transaction>> = withContext(io) {
        runCatching { api.getToday().transactions }
    }

    override suspend fun checkout(tx: Transaction): CheckoutResult = withContext(io) {
        try {
            val res = api.postTransaction(tx).transaction
            txDao.upsert(res.toEntity(pending = false))
            syncPendingInternal()
            CheckoutResult.Synced(res)
        } catch (e: IOException) {
            txDao.upsert(tx.toEntity(pending = true))
            CheckoutResult.SavedOffline(tx)
        }
    }

    override suspend fun syncPending(): Int = withContext(io) { syncPendingInternal() }

    private suspend fun syncPendingInternal(): Int {
        var count = 0
        for (e in txDao.pending()) {
            val tx = try {
                json.decodeFromString<Transaction>(e.payload)
            } catch (_: Exception) {
                continue // payload korup: lewati, jangan blokir antrean
            }
            try {
                api.postTransaction(tx)
                txDao.markSynced(e.id)
                count++
            } catch (_: IOException) {
                break // masih offline: berhenti, coba lagi nanti
            }
        }
        return count
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
