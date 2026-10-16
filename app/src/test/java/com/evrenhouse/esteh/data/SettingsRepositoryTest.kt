package com.evrenhouse.esteh.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.evrenhouse.esteh.data.remote.dto.Settings
import com.evrenhouse.esteh.data.remote.dto.SettingsResponse
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun repo(api: FakeApiService, dir: File): Triple<SettingsRepository, FakeSettingsDao, FakeApiService> {
        val store = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob()),
            produceFile = { File(dir, "prefs.preferences_pb") },
        )
        val dao = FakeSettingsDao()
        return Triple(SettingsRepositoryImpl(api, store, dao, UnconfinedTestDispatcher()), dao, api)
    }

    @Test
    fun `defaults without backend`() = runTest {
        val dir = tmp.root
        try {
            val (r, _, _) = repo(FakeApiService(), dir)
            assertEquals(Settings.DEFAULT, r.pricesFlow.first())
            assertNull(r.lastPaymentFlow.first())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `savePrices online persists + mirrors kv`() = runTest {
        val dir = tmp.root
        try {
            val api = FakeApiService()
            api.putSettingsHandler = { SettingsResponse(it) }
            val (r, dao, _) = repo(api, dir)
            val s = Settings(12000, 13000)
            assertEquals(SavePricesResult.Saved, r.savePrices(s))
            assertEquals(s, r.pricesFlow.first())
            assertEquals("12000,13000", dao.get("prices"))
            assertEquals(1, api.putCalls)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `savePrices offline still local`() = runTest {
        val dir = tmp.root
        try {
            val api = FakeApiService()
            api.putSettingsHandler = { throw IOException("down") }
            val (r, _, _) = repo(api, dir)
            assertEquals(SavePricesResult.SavedOffline, r.savePrices(Settings(12000, 13000)))
            assertEquals(12000, r.pricesFlow.first().esTeh)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `savePrices invalid never hits backend`() = runTest {
        val dir = tmp.root
        try {
            val api = FakeApiService()
            val (r, _, _) = repo(api, dir)
            assertEquals(SavePricesResult.Invalid, r.savePrices(Settings(0, 13000)))
            assertEquals(0, api.putCalls)
            assertEquals(Settings.DEFAULT, r.pricesFlow.first())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `refresh pulls backend, offline keeps local`() = runTest {
        val dir = tmp.root
        try {
            val api = FakeApiService()
            api.getSettingsHandler = { SettingsResponse(Settings(9000, 9500)) }
            val (r, _, _) = repo(api, dir)
            r.refreshFromBackend()
            assertEquals(9000, r.pricesFlow.first().esTeh)
            api.getSettingsHandler = { throw IOException("down") }
            r.refreshFromBackend()
            assertEquals(9000, r.pricesFlow.first().esTeh)
            r.setLastPayment("qris")
            assertEquals("qris", r.lastPaymentFlow.first())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `refresh ignores invalid backend`() = runTest {
        val dir = tmp.root
        try {
            val api = FakeApiService()
            api.getSettingsHandler = { SettingsResponse(Settings(-5, 0)) }
            val (r, _, _) = repo(api, dir)
            r.refreshFromBackend()
            assertEquals(Settings.DEFAULT, r.pricesFlow.first())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `showReceipt defaults true and toggles`() = runTest {
        val dir = tmp.root
        try {
            val (r, _, _) = repo(FakeApiService(), dir)
            assertEquals(true, r.showReceiptFlow.first())
            r.setShowReceipt(false)
            assertEquals(false, r.showReceiptFlow.first())
            r.setShowReceipt(true)
            assertEquals(true, r.showReceiptFlow.first())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `offline save marks dirty refresh pushes when back`() = runTest {
        val dir = tmp.root
        try {
            val api = FakeApiService()
            api.putSettingsHandler = { throw IOException("down") }
            val (r, _, _) = repo(api, dir)
            assertEquals(SavePricesResult.SavedOffline, r.savePrices(Settings(12000, 13000)))
            // Refresh saat masih offline: lokal dipertahankan, tidak pull.
            api.getSettingsHandler = { SettingsResponse(Settings(9000, 9000)) }
            r.refreshFromBackend()
            assertEquals(12000, r.pricesFlow.first().esTeh)
            // Backend kembali: refresh mendorong lokal yang dirty.
            var pushed: Settings? = null
            api.putSettingsHandler = { pushed = it; SettingsResponse(it) }
            r.refreshFromBackend()
            assertEquals(Settings(12000, 13000), pushed)
            // Dirty bersih: refresh berikutnya pull normal.
            api.getSettingsHandler = { SettingsResponse(Settings(9000, 9500)) }
            r.refreshFromBackend()
            assertEquals(9000, r.pricesFlow.first().esTeh)
        } finally {
            dir.deleteRecursively()
        }
    }
}
