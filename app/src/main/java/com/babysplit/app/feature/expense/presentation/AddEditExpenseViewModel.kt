package com.babysplit.app.feature.expense.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babysplit.app.core.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AddEditExpenseUiState(
    val members: List<MemberData> = emptyList(),
    val existingExpense: ExpenseData? = null,
    val currency: String = "USD",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedSuccessfully: Boolean = false
)

class AddEditExpenseViewModel(
    private val tripId: String,
    private val expenseId: String?,
    private val repository: TripRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditExpenseUiState())
    val uiState: StateFlow<AddEditExpenseUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        // Observe members
        viewModelScope.launch {
            repository.getMembersStream(tripId)
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collect { members ->
                    _uiState.update { it.copy(members = members) }
                }
        }

        // Load trip currency
        viewModelScope.launch {
            repository.getTripStream(tripId)
                .catch { /* ignore */ }
                .collect { trip ->
                    if (trip != null) {
                        _uiState.update { it.copy(currency = trip.currency) }
                    }
                }
        }

        // Load existing expense if editing
        if (!expenseId.isNullOrBlank()) {
            viewModelScope.launch {
                try {
                    val expense = repository.getExpenseById(tripId, expenseId)
                    _uiState.update { it.copy(existingExpense = expense, isLoading = false) }
                } catch (e: Exception) {
                    _uiState.update { it.copy(errorMessage = e.message, isLoading = false) }
                }
            }
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun saveExpense(expense: ExpenseData, onSaved: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                if (expenseId.isNullOrBlank()) {
                    repository.addExpense(tripId, expense)
                } else {
                    repository.updateExpense(tripId, expense)
                }
                _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
                onSaved()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Failed to save: ${e.message}") }
            }
        }
    }

    fun deleteExpense(expenseId: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteExpense(tripId, expenseId)
                onDeleted()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to delete: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
