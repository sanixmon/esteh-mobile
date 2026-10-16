package com.evrenhouse.esteh

import android.app.Application
import com.evrenhouse.esteh.di.AppContainer

class EstehApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
