package com.evrenhouse.esteh.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.evrenhouse.esteh.data.local.SettingEntity
import com.evrenhouse.esteh.data.local.SettingsDao
import com.evrenhouse.esteh.data.remote.ApiService
import com.evrenhouse.esteh.data.remote.dto.Settings
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed interface SavePricesResult {
    data object Saved : SavePricesResult
    data object SavedOffline : SavePricesResult
    data object Invalid : SavePricesResult
}

interface SettingsRepository {
    val pricesFlow: Flow<Settings>
    val lastPaymentFlow: Flow<String?>
    val showReceiptFlow: Flow<Boolean>
    suspend fun refreshFromBackend()
    suspend fun savePrices(settings: Settings): SavePricesResult
    suspend fun setLastPayment(method: String)
    suspend fun setShowReceipt(show: Boolean)
}

/**
 * Preferensi per perangkat + sinkron backend (android.md §1, §2.8).
 * DataStore = sumber utama; backend = sinkron; settings_kv = mirror.
 */
class SettingsRepositoryImpl(
    private val api: ApiService,
    private val store: DataStore<Preferences>,
    private val settingsDao: SettingsDao,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : SettingsRepository {

    /** Serialisasi save vs refresh agar tak ada update yang hilang. */
    private val mutex = Mutex()

    override val pricesFlow: Flow<Settings> = store.data.map { p ->
        Settings(
            esTeh = p[ES_TEH] ?: Settings.DEFAULT.esTeh,
            lemonTea = p[LEMON_TEA] ?: Settings.DEFAULT.lemonTea,
        )
    }

    override val lastPaymentFlow: Flow<String?> = store.data.map { it[LAST_PAYMENT] }

    override val showReceiptFlow: Flow<Boolean> = store.data.map { it[SHOW_RECEIPT] ?: true }

    override suspend fun refreshFromBackend() = withContext(io) {
        mutex.withLock {
            if (store.data.first()[DIRTY] == true) {
                // Lokal pernah ditulis saat offline: dorong ke backend, jangan pull.
                val local = Settings(
                    esTeh = store.data.first()[ES_TEH] ?: Settings.DEFAULT.esTeh,
                    lemonTea = store.data.first()[LEMON_TEA] ?: Settings.DEFAULT.lemonTea,
                )
                try {
                    api.putSettings(local)
                    store.edit { it[DIRTY] = false }
                } catch (_: IOException) {
                    // tetap dirty, coba lagi nanti
                }
                return@withLock
            }
            try {
                val s = api.getSettings().settings
                if (s.isValid()) saveLocal(s)
            } catch (_: IOException) {
                // offline: pertahankan lokal
            }
        }
    }

    override suspend fun savePrices(settings: Settings): SavePricesResult = withContext(io) {
        mutex.withLock {
            if (!settings.isValid()) return@withLock SavePricesResult.Invalid
            saveLocal(settings)
            try {
                api.putSettings(settings)
                store.edit { it[DIRTY] = false }
                SavePricesResult.Saved
            } catch (_: IOException) {
                store.edit { it[DIRTY] = true }
                SavePricesResult.SavedOffline
            }
        }
    }

    override suspend fun setLastPayment(method: String) {
        store.edit { it[LAST_PAYMENT] = method }
    }

    override suspend fun setShowReceipt(show: Boolean) {
        store.edit { it[SHOW_RECEIPT] = show }
    }

    private suspend fun saveLocal(s: Settings) {
        store.edit {
            it[ES_TEH] = s.esTeh
            it[LEMON_TEA] = s.lemonTea
        }
        settingsDao.put(SettingEntity(PRICES_KV, "${s.esTeh},${s.lemonTea}"))
    }

    companion object {
        val ES_TEH = intPreferencesKey("prices_es_teh")
        val LEMON_TEA = intPreferencesKey("prices_lemon_tea")
        val LAST_PAYMENT = stringPreferencesKey("last_payment")
        val SHOW_RECEIPT = booleanPreferencesKey("show_receipt")
        val DIRTY = booleanPreferencesKey("prices_dirty")
        const val PRICES_KV = "prices"
    }
}
