package com.babysplit.app.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.babysplit.app.core.whatsapp.HostPaymentDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "user_preferences")

class UserPreferencesDataStore(private val context: Context) {

    companion object {
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_AVATAR = stringPreferencesKey("user_avatar")
        val DEFAULT_CURRENCY = stringPreferencesKey("default_currency")
        val BANK_NAME = stringPreferencesKey("bank_name")
        val BANK_ACCOUNT = stringPreferencesKey("bank_account")
        val EWALLET_NAME = stringPreferencesKey("ewallet_name")
        val EWALLET_HANDLE = stringPreferencesKey("ewallet_handle")
        val PAYMENT_NOTE = stringPreferencesKey("payment_note")
        val GDRIVE_BACKUP_ENABLED = booleanPreferencesKey("gdrive_backup_enabled")
    }

    val hostPaymentDetailsFlow: Flow<HostPaymentDetails> = context.dataStore.data.map { prefs ->
        HostPaymentDetails(
            hostName = prefs[USER_NAME] ?: "Host",
            bankName = prefs[BANK_NAME],
            bankAccountNumber = prefs[BANK_ACCOUNT],
            eWalletName = prefs[EWALLET_NAME],
            eWalletHandle = prefs[EWALLET_HANDLE],
            customNote = prefs[PAYMENT_NOTE]
        )
    }

    val defaultCurrencyFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[DEFAULT_CURRENCY] ?: "USD"
    }

    val userNameFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[USER_NAME] ?: "Host"
    }

    val userEmailFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[USER_EMAIL]
    }

    suspend fun saveUserProfile(name: String, email: String?, avatarUrl: String? = null) {
        context.dataStore.edit { prefs ->
            prefs[USER_NAME] = name
            if (email != null) prefs[USER_EMAIL] = email
            if (avatarUrl != null) prefs[USER_AVATAR] = avatarUrl
        }
    }

    suspend fun savePaymentDetails(
        bankName: String?,
        bankAccount: String?,
        eWalletName: String?,
        eWalletHandle: String?,
        paymentNote: String?,
        defaultCurrency: String
    ) {
        context.dataStore.edit { prefs ->
            if (bankName != null) prefs[BANK_NAME] = bankName else prefs.remove(BANK_NAME)
            if (bankAccount != null) prefs[BANK_ACCOUNT] = bankAccount else prefs.remove(BANK_ACCOUNT)
            if (eWalletName != null) prefs[EWALLET_NAME] = eWalletName else prefs.remove(EWALLET_NAME)
            if (eWalletHandle != null) prefs[EWALLET_HANDLE] = eWalletHandle else prefs.remove(EWALLET_HANDLE)
            if (paymentNote != null) prefs[PAYMENT_NOTE] = paymentNote else prefs.remove(PAYMENT_NOTE)
            prefs[DEFAULT_CURRENCY] = defaultCurrency
        }
    }
}
