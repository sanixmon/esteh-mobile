package com.evrenhouse.esteh.data.remote

import com.evrenhouse.esteh.data.remote.dto.Settings
import com.evrenhouse.esteh.data.remote.dto.SettingsResponse
import com.evrenhouse.esteh.data.remote.dto.SummaryResponse
import com.evrenhouse.esteh.data.remote.dto.TodayResponse
import com.evrenhouse.esteh.data.remote.dto.Transaction
import com.evrenhouse.esteh.data.remote.dto.TransactionResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

/** Kontrak API dipakai ulang dari backend web (android.md §7). */
interface ApiService {

    /** Badge + angka rekap (ringan). */
    @GET("api/summary")
    suspend fun getSummary(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("date") date: String? = null,
    ): SummaryResponse

    /** Hanya dipanggil saat rekap dibuka. */
    @GET("api/today")
    suspend fun getToday(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("date") date: String? = null,
    ): TodayResponse

    /** Body Transaction tanpa validasi kembalian. 201 | 400 { error }. */
    @POST("api/transactions")
    suspend fun postTransaction(@Body transaction: Transaction): TransactionResponse

    @GET("api/settings")
    suspend fun getSettings(): SettingsResponse

    @PUT("api/settings")
    suspend fun putSettings(@Body settings: Settings): SettingsResponse
}
