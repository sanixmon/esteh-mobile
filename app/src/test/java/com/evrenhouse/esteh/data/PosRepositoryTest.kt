package com.evrenhouse.esteh.data

import com.evrenhouse.esteh.data.remote.ApiClient
import com.evrenhouse.esteh.data.remote.dto.DaySummary
import com.evrenhouse.esteh.data.remote.dto.PayStats
import com.evrenhouse.esteh.data.remote.dto.Transaction
import com.evrenhouse.esteh.data.remote.dto.TransactionResponse
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PosRepositoryTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val tx = Transaction(
        id = "TRX-20261015-AB12", timestamp = "2026-10-15T10:00:00Z",
        dateKey = "2026-10-15", timeString = "17:00:00", dateString = "Kamis, 15 Oktober 2026",
        items = emptyList(), totalQty = 0, totalAmount = 0, paymentMethod = Transaction.CASH,
    )

    private fun repo(api: FakeApiService, dao: FakeTransactionDao) =
        PosRepositoryImpl(api, dao, ApiClient.json, dispatcher)

    @Test
    fun `checkout online syncs`() = runTest {
        val api = FakeApiService()
        api.postHandler = { TransactionResponse(it) }
        val dao = FakeTransactionDao()
        val r = repo(api, dao).checkout(tx)
        assertTrue(r is CheckoutResult.Synced)
        assertEquals(false, dao.byId(tx.id)?.pendingSync)
    }

    @Test
    fun `checkout offline saves pending`() = runTest {
        val api = FakeApiService()
        api.postHandler = { throw IOException("offline") }
        val dao = FakeTransactionDao()
        val r = repo(api, dao).checkout(tx)
        assertTrue(r is CheckoutResult.SavedOffline)
        assertEquals(1, dao.pendingCount())
    }

    @Test
    fun `http 400 rejects and drops row`() = runTest {
        val api = FakeApiService()
        api.postHandler = {
            throw retrofit2.HttpException(
                retrofit2.Response.error<Any>(
                    400,
                    """{"error":"Transaksi tidak valid"}""".toResponseBody(),
                ),
            )
        }
        val dao = FakeTransactionDao()
        val r = repo(api, dao).checkout(tx)
        assertTrue(r is CheckoutResult.Rejected)
        assertEquals("Transaksi tidak valid", (r as CheckoutResult.Rejected).message)
        assertEquals(0, dao.pendingCount())
        assertNull(dao.byId(tx.id))
    }

    @Test
    fun `http 500 is transient keeps row`() = runTest {
        val api = FakeApiService()
        api.postHandler = {
            throw retrofit2.HttpException(retrofit2.Response.error<Any>(500, "".toResponseBody()))
        }
        val dao = FakeTransactionDao()
        val r = repo(api, dao).checkout(tx)
        assertTrue(r is CheckoutResult.SavedOffline)
        assertEquals(1, dao.pendingCount())
    }

    @Test
    fun `write ahead row exists before post returns`() = runTest {
        val api = FakeApiService()
        var rowDuringPost: Boolean? = null
        val dao = FakeTransactionDao()
        api.postHandler = {
            rowDuringPost = dao.byId(tx.id)?.pendingSync
            TransactionResponse(it)
        }
        repo(api, dao).checkout(tx)
        assertEquals(true, rowDuringPost)
        assertEquals(false, dao.byId(tx.id)?.pendingSync)
    }

    @Test
    fun `concurrent same id posts once`() = runTest {
        val api = FakeApiService()
        val gate = CompletableDeferred<Unit>()
        var posts = 0
        api.postHandler = {
            posts++
            if (posts == 1) gate.await()
            TransactionResponse(it)
        }
        val dao = FakeTransactionDao()
        val r = repo(api, dao)
        val a = async { r.checkout(tx) }
        advanceUntilIdle() // a jalan sampai gate
        val b = async { r.checkout(tx) }
        advanceUntilIdle() // b menunggu hasil a yang sama
        assertEquals(1, posts) // kedua pemanggil berbagi satu POST
        gate.complete(Unit)
        assertTrue(a.await() is CheckoutResult.Synced)
        assertTrue(b.await() is CheckoutResult.Synced)
        assertEquals(1, posts)
    }

    @Test
    fun `corrupt payload row deleted on sync`() = runTest {
        val api = FakeApiService()
        api.postHandler = { TransactionResponse(it) }
        val dao = FakeTransactionDao()
        dao.upsert(
            com.evrenhouse.esteh.data.local.TransactionEntity(
                "POISON", "2026-10-15", "t", "cash", 0, 0, "{bukan-json", true, 1,
            ),
        )
        assertEquals(0, repo(api, dao).syncPending())
        assertNull(dao.byId("POISON"))
        assertEquals(0, api.postCalls)
    }

    private fun pendingEntity(id: String, tx: Transaction, at: Long) =
        com.evrenhouse.esteh.data.local.TransactionEntity(
            id, tx.dateKey, tx.timestamp, tx.paymentMethod, 0, 0,
            ApiClient.json.encodeToString(Transaction.serializer(), tx.copy(id = id)), true, at,
        )

    @Test
    fun `syncPending stops at first offline`() = runTest {
        val api = FakeApiService()
        var n = 0
        api.postHandler = {
            n++
            if (n == 1) TransactionResponse(it) else throw IOException("drop")
        }
        val dao = FakeTransactionDao()
        dao.upsert(pendingEntity("A", tx, 1))
        dao.upsert(pendingEntity("B", tx, 2))
        assertEquals(1, repo(api, dao).syncPending())
        assertEquals(1, dao.pendingCount())
    }

    @Test
    fun `summary result wraps failure`() = runTest {
        val api = FakeApiService()
        api.summaryHandler = { throw IOException("down") }
        val r = repo(api, FakeTransactionDao()).getSummary()
        assertTrue(r.isFailure)
    }

    @Test
    fun `summary ok`() = runTest {
        val api = FakeApiService()
        val sum = DaySummary(1, 10000, 1, emptyMap(), PayStats(1, 10000), PayStats(0, 0))
        api.summaryHandler = { com.evrenhouse.esteh.data.remote.dto.SummaryResponse(sum) }
        assertEquals(sum, repo(api, FakeTransactionDao()).getSummary().getOrThrow())
    }
}
