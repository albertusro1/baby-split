package com.babysplit.app

import android.app.Application
import com.babysplit.app.core.database.BabySplitDatabase
import com.babysplit.app.core.datastore.UserPreferencesDataStore

class BabySplitApplication : Application() {
    val database: BabySplitDatabase by lazy { BabySplitDatabase.getDatabase(this) }
    val userPreferences: UserPreferencesDataStore by lazy { UserPreferencesDataStore(this) }

    override fun onCreate() {
        super.onCreate()
    }
}
