package com.example.karma.di

import android.content.Context
import com.example.karma.data.local.KarmaDatabase
import com.example.karma.data.repository.KarmaRepository

class AppContainer(context: Context) {

    private val database: KarmaDatabase = KarmaDatabase.getInstance(context)

    private val historyEntryDao = database.historyEntryDao()
    private val karmaSettingsDao = database.karmaSettingsDao()

    val repository: KarmaRepository = KarmaRepository(
        historyDao = historyEntryDao,
        settingsDao = karmaSettingsDao,
    )
}
