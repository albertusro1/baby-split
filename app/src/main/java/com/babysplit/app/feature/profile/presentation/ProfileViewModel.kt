package com.babysplit.app.feature.profile.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.babysplit.app.BabySplitApplication
import com.babysplit.app.core.auth.FirebaseAuthRepository
import com.babysplit.app.core.datastore.UserPreferencesDataStore
import com.babysplit.app.core.whatsapp.HostPaymentDetails
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isSignedIn: Boolean = false,
    val userName: String = "Guest",
    val userEmail: String? = null,
    val userPhotoUrl: String? = null,
    val paymentDetails: HostPaymentDetails? = null,
    val defaultCurrency: String = "USD",
    val isSigningIn: Boolean = false,
    val errorMessage: String? = null
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BabySplitApplication
    private val userPrefs: UserPreferencesDataStore = app.userPreferences
    val authRepository = FirebaseAuthRepository(application)

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        // Observe auth state
        viewModelScope.launch {
            authRepository.getAuthStateFlow().collectLatest { user ->
                _uiState.update {
                    it.copy(
                        isSignedIn = user != null,
                        userName = user?.displayName ?: "Guest",
                        userEmail = user?.email,
                        userPhotoUrl = user?.photoUrl?.toString()
                    )
                }
            }
        }

        // Observe payment details
        viewModelScope.launch {
            userPrefs.hostPaymentDetailsFlow.collect { details ->
                _uiState.update { it.copy(paymentDetails = details) }
            }
        }

        // Observe default currency
        viewModelScope.launch {
            userPrefs.defaultCurrencyFlow.collect { currency ->
                _uiState.update { it.copy(defaultCurrency = currency) }
            }
        }
    }

    fun signInWithGoogle(activityContext: android.content.Context, webClientId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSigningIn = true, errorMessage = null) }
            val result = authRepository.signInWithGoogle(activityContext, webClientId)
            result.fold(
                onSuccess = { user ->
                    // Save user info to DataStore
                    userPrefs.saveUserProfile(
                        name = user.displayName ?: "User",
                        email = user.email,
                        avatarUrl = user.photoUrl?.toString()
                    )
                    _uiState.update { it.copy(isSigningIn = false, errorMessage = null) }
                },
                onFailure = { e ->
                    e.printStackTrace()
                    _uiState.update {
                        it.copy(isSigningIn = false, errorMessage = "Sign-in failed: ${e.localizedMessage ?: e.message}")
                    }
                }
            )
        }
    }

    fun signOut(activityContext: android.content.Context? = null) {
        viewModelScope.launch {
            authRepository.signOut(activityContext)
            userPrefs.signOut()
        }
    }

    fun savePaymentDetails(
        bankName: String?,
        bankAccountHolder: String?,
        bankAccount: String?,
        eWalletName: String?,
        eWalletHandle: String?,
        paymentNote: String?,
        defaultCurrency: String
    ) {
        viewModelScope.launch {
            userPrefs.savePaymentDetails(
                bankName, bankAccountHolder, bankAccount,
                eWalletName, eWalletHandle, paymentNote, defaultCurrency
            )

            // Also update all HOST members in local Room DB
            val allGroups = app.database.groupDao().getAllGroupsDirect()
            for (g in allGroups) {
                val members = app.database.memberDao().getMembersForGroupDirect(g.id)
                for (m in members) {
                    if (m.memberType == "HOST" || m.name.contains("Host", ignoreCase = true) || m.name.contains("You", ignoreCase = true)) {
                        app.database.memberDao().updateMember(
                            m.copy(
                                bankName = bankName,
                                accountHolderName = bankAccountHolder ?: m.accountHolderName,
                                bankAccountNumber = bankAccount,
                                eWalletName = eWalletName,
                                eWalletHandle = eWalletHandle
                            )
                        )
                    }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
