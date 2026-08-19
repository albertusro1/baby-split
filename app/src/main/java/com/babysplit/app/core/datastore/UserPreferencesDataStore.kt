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
        val BANK_ACCOUNT_HOLDER = stringPreferencesKey("bank_account_holder")
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
            accountHolderName = prefs[BANK_ACCOUNT_HOLDER],
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
        prefs[USER_NAME] ?: "Guest"
    }

    val userEmailFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[USER_EMAIL]
    }

    suspend fun saveUserProfile(name: String, email: String?, avatarUrl: String? = null) {
        context.dataStore.edit { prefs ->
            prefs[USER_NAME] = name
            if (email != null) prefs[USER_EMAIL] = email else prefs.remove(USER_EMAIL)
            if (avatarUrl != null) prefs[USER_AVATAR] = avatarUrl else prefs.remove(USER_AVATAR)
        }
    }

    suspend fun signOut() {
        context.dataStore.edit { prefs ->
            prefs.remove(USER_EMAIL)
            prefs.remove(USER_AVATAR)
            prefs[USER_NAME] = "Guest"
        }
    }

    suspend fun savePaymentDetails(
        bankName: String?,
        bankAccountHolder: String?,
        bankAccount: String?,
        eWalletName: String?,
        eWalletHandle: String?,
        paymentNote: String?,
        defaultCurrency: String
    ) {
        context.dataStore.edit { prefs ->
            if (!bankName.isNullOrBlank()) prefs[BANK_NAME] = bankName else prefs.remove(BANK_NAME)
            if (!bankAccountHolder.isNullOrBlank()) prefs[BANK_ACCOUNT_HOLDER] = bankAccountHolder else prefs.remove(BANK_ACCOUNT_HOLDER)
            if (!bankAccount.isNullOrBlank()) prefs[BANK_ACCOUNT] = bankAccount else prefs.remove(BANK_ACCOUNT)
            if (!eWalletName.isNullOrBlank()) prefs[EWALLET_NAME] = eWalletName else prefs.remove(EWALLET_NAME)
            if (!eWalletHandle.isNullOrBlank()) prefs[EWALLET_HANDLE] = eWalletHandle else prefs.remove(EWALLET_HANDLE)
            if (!paymentNote.isNullOrBlank()) prefs[PAYMENT_NOTE] = paymentNote else prefs.remove(PAYMENT_NOTE)
            prefs[DEFAULT_CURRENCY] = defaultCurrency
        }
    }
}
