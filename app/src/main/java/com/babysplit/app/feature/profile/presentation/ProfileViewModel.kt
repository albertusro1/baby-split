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
import kotlinx.coroutines.tasks.await

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
                    if (e is androidx.credentials.exceptions.GetCredentialCancellationException) {
                        _uiState.update { it.copy(isSigningIn = false, errorMessage = null) }
                    } else {
                        e.printStackTrace()
                        _uiState.update {
                            it.copy(isSigningIn = false, errorMessage = "Sign-in failed: ${e.localizedMessage ?: e.message}")
                        }
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

            // 1. Update all HOST / current user members in local Room DB
            val allGroups = app.database.groupDao().getAllGroupsDirect()
            val currentUserName = _uiState.value.userName
            for (g in allGroups) {
                val members = app.database.memberDao().getMembersForGroupDirect(g.id)
                for (m in members) {
                    if (m.memberType == "HOST" || m.name.equals(currentUserName, ignoreCase = true) || m.name.contains("Host", ignoreCase = true) || m.name.contains("You", ignoreCase = true)) {
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

            // 2. If signed in, sync to Firestore /users/{uid} and all trips containing this user
            val currentUser = authRepository.getCurrentUser()
            if (currentUser != null) {
                try {
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    val userMap = mutableMapOf<String, Any?>(
                        "name" to (_uiState.value.userName.ifBlank { currentUser.displayName ?: "User" }),
                        "email" to currentUser.email,
                        "bankName" to bankName,
                        "accountHolderName" to (bankAccountHolder ?: _uiState.value.userName),
                        "bankAccountNumber" to bankAccount,
                        "eWalletName" to eWalletName,
                        "eWalletHandle" to eWalletHandle,
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                    db.collection("users").document(currentUser.uid).set(userMap, com.google.firebase.firestore.SetOptions.merge())

                    // Update all trips where this user is a member
                    val tripsSnapshot = db.collection("trips")
                        .whereArrayContains("memberIds", currentUser.uid)
                        .get()
                        .await()

                    for (tripDoc in tripsSnapshot.documents) {
                        val membersSnapshot = tripDoc.reference.collection("members")
                            .whereEqualTo("firebaseUid", currentUser.uid)
                            .get()
                            .await()

                        for (memberDoc in membersSnapshot.documents) {
                            memberDoc.reference.update(
                                mapOf(
                                    "bankName" to bankName,
                                    "accountHolderName" to (bankAccountHolder ?: _uiState.value.userName),
                                    "bankAccountNumber" to bankAccount,
                                    "eWalletName" to eWalletName,
                                    "eWalletHandle" to eWalletHandle
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
