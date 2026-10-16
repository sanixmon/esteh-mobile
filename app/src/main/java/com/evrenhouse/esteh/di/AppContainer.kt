package com.evrenhouse.esteh.di

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.evrenhouse.esteh.data.PosRepository
import com.evrenhouse.esteh.data.PosRepositoryImpl
import com.evrenhouse.esteh.data.SettingsRepository
import com.evrenhouse.esteh.data.SettingsRepositoryImpl
import com.evrenhouse.esteh.data.local.PosDatabase
import com.evrenhouse.esteh.data.remote.ApiClient
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.evrenhouse.esteh.viewmodel.ClockViewModel
import com.evrenhouse.esteh.viewmodel.PosViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Wiring manual tanpa framework DI: DB + DataStore + API + repo. */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val db: PosDatabase by lazy {
        Room.databaseBuilder(appContext, PosDatabase::class.java, "esteh.db").build()
    }

    private val dataStore by lazy {
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { appContext.preferencesDataStoreFile("esteh_prefs") },
        )
    }

    private val api by lazy { ApiClient.create() }

    val posRepository: PosRepository by lazy {
        PosRepositoryImpl(api, db.transactionDao(), ApiClient.json)
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(api, dataStore, db.settingsDao())
    }

    fun posViewModel(): PosViewModel = PosViewModel(posRepository, settingsRepository)

    /** Factory agar ViewModel survive rotasi (kriteria §10). */
    val vmFactory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T = when {
            modelClass.isAssignableFrom(PosViewModel::class.java) -> posViewModel() as T
            modelClass.isAssignableFrom(ClockViewModel::class.java) -> ClockViewModel() as T
            else -> throw IllegalArgumentException(modelClass.name)
        }
    }
}
