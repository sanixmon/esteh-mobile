package com.evrenhouse.esteh.data

import com.evrenhouse.esteh.data.remote.ApiClient
import com.evrenhouse.esteh.data.remote.dto.DaySummary
import com.evrenhouse.esteh.data.remote.dto.PayStats
import com.evrenhouse.esteh.data.remote.dto.Transaction
import com.evrenhouse.esteh.data.remote.dto.TransactionResponse
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
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
    fun `http 400 propagates not swallowed`() = runTest {
        val api = FakeApiService()
        api.postHandler = { throw retrofit2.HttpException(retrofit2.Response.error<Any>(400, "".toResponseBody())) }
        var threw = false
        try {
            repo(api, FakeTransactionDao()).checkout(tx)
        } catch (_: retrofit2.HttpException) {
            threw = true
        }
        assertTrue(threw)
    }

    private fun pendingEntity(id: String, tx: Transaction, at: Long) =
        com.evrenhouse.esteh.data.local.TransactionEntity(
            id, tx.dateKey, tx.timestamp, tx.paymentMethod, 0, 0,
            ApiClient.json.encodeToString(Transaction.serializer(), tx), true, at,
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
