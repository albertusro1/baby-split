package com.babysplit.app.feature.dashboard.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.babysplit.app.BabySplitApplication
import com.babysplit.app.core.auth.FirebaseAuthRepository
import com.babysplit.app.core.database.LocalTripRepository
import com.babysplit.app.core.firestore.FirestoreTripRepository
import com.babysplit.app.core.repository.MemberData
import com.babysplit.app.core.repository.TripData
import com.babysplit.app.core.repository.TripRepository
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class DashboardUiState(
    val trips: List<TripData> = emptyList(),
    val isSignedIn: Boolean = false,
    val userName: String = "Guest",
    val userEmail: String? = null,
    val userPhotoUrl: String? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val joinTripResult: JoinTripResult? = null
)

sealed interface JoinTripResult {
    data class Success(val tripId: String) : JoinTripResult
    data class Error(val message: String) : JoinTripResult
}

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BabySplitApplication
    val authRepository = FirebaseAuthRepository(application)

    private val localRepository: TripRepository = LocalTripRepository(
        groupDao = app.database.groupDao(),
        memberDao = app.database.memberDao(),
        expenseDao = app.database.expenseDao()
    )

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    // The active repository, switched based on auth state
    private var _activeRepository: TripRepository = localRepository
    val activeRepository: TripRepository get() = _activeRepository

    init {
        // Observe auth state and switch repos accordingly
        viewModelScope.launch {
            authRepository.getAuthStateFlow().collectLatest { user ->
                updateAuthState(user)
                observeTrips()
            }
        }
    }

    private fun updateAuthState(user: FirebaseUser?) {
        _activeRepository = if (user != null) {
            FirestoreTripRepository(
                db = FirebaseFirestore.getInstance(),
                userId = user.uid
            )
        } else {
            localRepository
        }

        _uiState.update {
            it.copy(
                isSignedIn = user != null,
                userName = user?.displayName ?: "Guest",
                userEmail = user?.email,
                userPhotoUrl = user?.photoUrl?.toString()
            )
        }
    }

    private fun observeTrips() {
        viewModelScope.launch {
            _activeRepository.getTripsStream()
                .catch { e ->
                    _uiState.update { it.copy(errorMessage = e.message, isLoading = false) }
                }
                .collect { trips ->
                    _uiState.update {
                        it.copy(trips = trips, isLoading = false, errorMessage = null)
                    }
                }
        }
    }

    fun createTrip(
        name: String,
        emoji: String,
        currency: String,
        simplifyDebts: Boolean,
        hostName: String,
        hostBankName: String? = null,
        hostAccountHolderName: String? = null,
        hostBankAccountNumber: String? = null,
        hostEWalletName: String? = null,
        hostEWalletHandle: String? = null,
        onCreated: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val user = authRepository.getCurrentUser()
                val tripId = _activeRepository.createTrip(
                    TripData(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        currency = currency,
                        emoji = emoji,
                        simplifyDebts = simplifyDebts,
                        createdBy = user?.uid ?: "",
                        isCloud = user != null
                    )
                )

                // Add host as the first member
                _activeRepository.addMember(
                    tripId = tripId,
                    member = MemberData(
                        id = UUID.randomUUID().toString(),
                        tripId = tripId,
                        name = hostName.ifBlank { "You (Host)" },
                        memberType = "HOST",
                        firebaseUid = user?.uid,
                        email = user?.email,
                        role = "admin",
                        bankName = hostBankName,
                        accountHolderName = hostAccountHolderName,
                        bankAccountNumber = hostBankAccountNumber,
                        eWalletName = hostEWalletName,
                        eWalletHandle = hostEWalletHandle
                    )
                )

                onCreated(tripId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to create trip: ${e.message}") }
            }
        }
    }

    fun deleteTrip(tripId: String) {
        viewModelScope.launch {
            try {
                _activeRepository.deleteTrip(tripId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to delete: ${e.message}") }
            }
        }
    }

    fun joinTripByCode(code: String) {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            if (user == null) {
                _uiState.update {
                    it.copy(joinTripResult = JoinTripResult.Error("Please sign in to join a trip"))
                }
                return@launch
            }

            val paymentDetails = app.userPreferences.hostPaymentDetailsFlow.firstOrNull()

            val result = _activeRepository.joinTripByInviteCode(
                code = code.uppercase().trim(),
                userId = user.uid,
                userName = user.displayName ?: "Guest",
                bankName = paymentDetails?.bankName,
                accountHolderName = paymentDetails?.accountHolderName ?: user.displayName ?: "Guest",
                bankAccountNumber = paymentDetails?.bankAccountNumber,
                eWalletName = paymentDetails?.eWalletName,
                eWalletHandle = paymentDetails?.eWalletHandle
            )

            result.fold(
                onSuccess = { tripId ->
                    _uiState.update { it.copy(joinTripResult = JoinTripResult.Success(tripId)) }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(joinTripResult = JoinTripResult.Error(e.message ?: "Failed to join trip"))
                    }
                }
            )
        }
    }

    fun clearJoinTripResult() {
        _uiState.update { it.copy(joinTripResult = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
