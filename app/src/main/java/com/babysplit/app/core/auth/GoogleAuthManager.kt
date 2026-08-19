package com.babysplit.app.core.auth

import android.accounts.AccountManager
import android.content.Intent

object GoogleAuthManager {

    /**
     * Creates an Intent to launch the Android native Google Account picker.
     * Displays a system sheet of Google accounts on the device.
     */
    fun createGoogleAccountPickerIntent(): Intent {
        return try {
            AccountManager.newChooseAccountIntent(
                null,
                null,
                arrayOf("com.google"),
                null,
                null,
                null,
                null
            )
        } catch (e: Exception) {
            Intent(Intent.ACTION_MAIN)
        }
    }
}
