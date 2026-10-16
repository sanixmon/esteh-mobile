package com.evrenhouse.esteh.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

/**
 * Offline mirror paritas fallback localStorage web (android.md §1, §3).
 * Seluruh transaksi disimpan; yang gagal POST ditandai pendingSync.
 */
// "transactions" adalah keyword SQLite → query di bawah memakai backtick.
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val dateKey: String,
    val timestamp: String,
    val paymentMethod: String,
    val totalQty: Int,
    val totalAmount: Int,
    /** Snapshot Transaction penuh sebagai JSON. */
    val payload: String,
    val pendingSync: Boolean,
    val createdAtMs: Long,
)

@Entity(tableName = "settings_kv")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String,
)

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: TransactionEntity)

    @Query("SELECT * FROM `transactions` WHERE pendingSync = 1 ORDER BY createdAtMs ASC")
    suspend fun pending(): List<TransactionEntity>

    @Query("SELECT * FROM `transactions` WHERE id = :id")
    suspend fun byId(id: String): TransactionEntity?

    @Query("UPDATE `transactions` SET pendingSync = 0 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("DELETE FROM `transactions` WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COUNT(*) FROM `transactions` WHERE pendingSync = 1")
    suspend fun pendingCount(): Int
}

@Dao
interface SettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(e: SettingEntity)

    @Query("SELECT value FROM settings_kv WHERE `key` = :key")
    suspend fun get(key: String): String?
}

@Database(entities = [TransactionEntity::class, SettingEntity::class], version = 1, exportSchema = false)
abstract class PosDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun settingsDao(): SettingsDao
}
