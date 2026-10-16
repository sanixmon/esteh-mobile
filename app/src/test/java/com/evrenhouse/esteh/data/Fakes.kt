package com.evrenhouse.esteh.data

import com.evrenhouse.esteh.data.local.SettingEntity
import com.evrenhouse.esteh.data.local.SettingsDao
import com.evrenhouse.esteh.data.local.TransactionDao
import com.evrenhouse.esteh.data.local.TransactionEntity
import com.evrenhouse.esteh.data.remote.ApiService
import com.evrenhouse.esteh.data.remote.dto.Settings
import com.evrenhouse.esteh.data.remote.dto.SettingsResponse
import com.evrenhouse.esteh.data.remote.dto.SummaryResponse
import com.evrenhouse.esteh.data.remote.dto.TodayResponse
import com.evrenhouse.esteh.data.remote.dto.Transaction
import com.evrenhouse.esteh.data.remote.dto.TransactionResponse

/** Fake bersama untuk tes repository & ViewModel. */
class FakeApiService : ApiService {
    var summaryHandler: () -> SummaryResponse = { throw UnsupportedOperationException() }
    var todayHandler: () -> TodayResponse = { throw UnsupportedOperationException() }
    var postHandler: suspend (Transaction) -> TransactionResponse = { throw UnsupportedOperationException() }
    var getSettingsHandler: () -> SettingsResponse = { throw UnsupportedOperationException() }
    var putSettingsHandler: (Settings) -> SettingsResponse = { throw UnsupportedOperationException() }
    var postCalls = 0
    var putCalls = 0

    override suspend fun getSummary(from: String?, to: String?, date: String?): SummaryResponse = summaryHandler()
    override suspend fun getToday(from: String?, to: String?, date: String?): TodayResponse = todayHandler()
    override suspend fun postTransaction(tx: Transaction): TransactionResponse {
        postCalls++
        return postHandler(tx)
    }
    override suspend fun getSettings(): SettingsResponse = getSettingsHandler()
    override suspend fun putSettings(s: Settings): SettingsResponse {
        putCalls++
        return putSettingsHandler(s)
    }
}

class FakeTransactionDao : TransactionDao {
    val rows = mutableMapOf<String, TransactionEntity>()
    override suspend fun upsert(e: TransactionEntity) { rows[e.id] = e }
    override suspend fun pending(): List<TransactionEntity> =
        rows.values.filter { it.pendingSync }.sortedBy { it.createdAtMs }
    override suspend fun byId(id: String): TransactionEntity? = rows[id]
    override suspend fun markSynced(id: String) { rows[id]?.let { rows[id] = it.copy(pendingSync = false) } }
    override suspend fun delete(id: String) { rows.remove(id) }
    override suspend fun pendingCount(): Int = rows.values.count { it.pendingSync }
}

class FakeSettingsDao : SettingsDao {
    val kv = mutableMapOf<String, String>()
    override suspend fun put(e: SettingEntity) { kv[e.key] = e.value }
    override suspend fun get(key: String): String? = kv[key]
}
