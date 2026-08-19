package com.babysplit.app.feature.group.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babysplit.app.core.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class GroupDetailUiState(
    val trip: TripData? = null,
    val members: List<MemberData> = emptyList(),
    val expenses: List<ExpenseData> = emptyList(),
    val inviteCode: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class GroupDetailViewModel(
    private val tripId: String,
    private val repository: TripRepository,
    private val currentUserId: String = ""
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupDetailUiState())
    val uiState: StateFlow<GroupDetailUiState> = _uiState.asStateFlow()

    init {
        observeTripData()
    }

    private fun observeTripData() {
        // Observe trip
        viewModelScope.launch {
            repository.getTripStream(tripId)
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collect { trip ->
                    _uiState.update {
                        it.copy(
                            trip = trip,
                            inviteCode = trip?.inviteCode ?: "",
                            isLoading = false
                        )
                    }
                }
        }

        // Observe members
        viewModelScope.launch {
            repository.getMembersStream(tripId)
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collect { members ->
                    _uiState.update { it.copy(members = members) }
                }
        }

        // Observe expenses
        viewModelScope.launch {
            repository.getExpensesStream(tripId)
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collect { expenses ->
                    _uiState.update { it.copy(expenses = expenses) }
                }
        }
    }

    fun addMember(
        name: String,
        memberType: String = "OFFLINE_TAGGED",
        email: String? = null,
        phoneNumber: String? = null,
        bankName: String? = null,
        accountHolderName: String? = null,
        bankAccountNumber: String? = null,
        eWalletName: String? = null,
        eWalletHandle: String? = null
    ) {
        viewModelScope.launch {
            try {
                repository.addMember(
                    tripId = tripId,
                    member = MemberData(
                        id = UUID.randomUUID().toString(),
                        tripId = tripId,
                        name = name,
                        memberType = memberType,
                        email = email,
                        phoneNumber = phoneNumber,
                        bankName = bankName,
                        accountHolderName = accountHolderName,
                        bankAccountNumber = bankAccountNumber,
                        eWalletName = eWalletName,
                        eWalletHandle = eWalletHandle
                    )
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to add member: ${e.message}") }
            }
        }
    }

    fun updateMember(member: MemberData) {
        viewModelScope.launch {
            try {
                repository.updateMember(tripId, member)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to update member: ${e.message}") }
            }
        }
    }

    fun removeMember(memberId: String) {
        viewModelScope.launch {
            try {
                repository.removeMember(tripId, memberId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to remove member: ${e.message}") }
            }
        }
    }

    fun recordSettlement(payerMemberId: String, receiverMemberId: String, amountCents: Long) {
        viewModelScope.launch {
            try {
                val members = _uiState.value.members
                val payer = members.firstOrNull { it.id == payerMemberId }?.name ?: "Payer"
                val receiver = members.firstOrNull { it.id == receiverMemberId }?.name ?: "Receiver"
                val expenseId = UUID.randomUUID().toString()

                repository.addExpense(
                    tripId = tripId,
                    expense = ExpenseData(
                        id = expenseId,
                        tripId = tripId,
                        title = "Payment: $payer \u27a1 $receiver",
                        totalAmountCents = amountCents,
                        currency = _uiState.value.trip?.currency ?: "USD",
                        categoryName = "SETTLEMENT",
                        paidByMemberId = payerMemberId,
                        paidByMemberName = payer,
                        splitType = "EQUAL",
                        isSettlement = true,
                        createdBy = currentUserId,
                        participants = listOf(
                            ParticipantData(
                                memberId = receiverMemberId,
                                memberName = receiver,
                                amountCents = amountCents
                            )
                        )
                    )
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to record settlement: ${e.message}") }
            }
        }
    }

    fun finishTrip() {
        viewModelScope.launch {
            try {
                repository.updateTrip(tripId, isFinished = true)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to finish trip: ${e.message}") }
            }
        }
    }

    fun deleteTrip(onDeleted: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteTrip(tripId)
                onDeleted()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to delete trip: ${e.message}") }
            }
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            try {
                repository.deleteExpense(tripId, expenseId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to delete expense: ${e.message}") }
            }
        }
    }

    fun refreshInviteCode() {
        viewModelScope.launch {
            try {
                val code = repository.generateInviteCode(tripId)
                _uiState.update { it.copy(inviteCode = code) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to generate invite code: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
