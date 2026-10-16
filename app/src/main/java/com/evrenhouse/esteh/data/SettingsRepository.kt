package com.evrenhouse.esteh.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
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
import kotlinx.coroutines.withContext

sealed interface SavePricesResult {
    data object Saved : SavePricesResult
    data object SavedOffline : SavePricesResult
    data object Invalid : SavePricesResult
}

interface SettingsRepository {
    val pricesFlow: Flow<Settings>
    val lastPaymentFlow: Flow<String?>
    suspend fun refreshFromBackend()
    suspend fun savePrices(settings: Settings): SavePricesResult
    suspend fun setLastPayment(method: String)
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

    override val pricesFlow: Flow<Settings> = store.data.map { p ->
        Settings(
            esTeh = p[ES_TEH] ?: Settings.DEFAULT.esTeh,
            lemonTea = p[LEMON_TEA] ?: Settings.DEFAULT.lemonTea,
        )
    }

    override val lastPaymentFlow: Flow<String?> = store.data.map { it[LAST_PAYMENT] }

    override suspend fun refreshFromBackend() = withContext(io) {
        try {
            val s = api.getSettings().settings
            if (s.isValid()) saveLocal(s)
        } catch (_: IOException) {
            // offline: pertahankan lokal
        }
    }

    override suspend fun savePrices(settings: Settings): SavePricesResult = withContext(io) {
        if (!settings.isValid()) return@withContext SavePricesResult.Invalid
        saveLocal(settings)
        try {
            api.putSettings(settings)
            SavePricesResult.Saved
        } catch (_: IOException) {
            SavePricesResult.SavedOffline
        }
    }

    override suspend fun setLastPayment(method: String) {
        store.edit { it[LAST_PAYMENT] = method }
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
        const val PRICES_KV = "prices"
    }
}
