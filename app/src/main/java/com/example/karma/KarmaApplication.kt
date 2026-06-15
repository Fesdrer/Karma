package com.example.karma

import android.app.Application
import com.example.karma.di.AppContainer

class KarmaApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
