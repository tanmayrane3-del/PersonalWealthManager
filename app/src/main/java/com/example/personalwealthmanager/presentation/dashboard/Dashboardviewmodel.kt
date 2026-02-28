package com.example.personalwealthmanager.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalwealthmanager.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    fun loadDashboardData(dateFrom: String? = null, dateTo: String? = null) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            transactionRepository.getTransactions(dateFrom, dateTo)
                .onSuccess { transactions ->
                    val totalIncome = transactions
                        .filter { it.type == "income" }
                        .sumOf { it.amount.toDoubleOrNull() ?: 0.0 }

                    val totalExpenses = transactions
                        .filter { it.type == "expense" }
                        .sumOf { it.amount.toDoubleOrNull() ?: 0.0 }

                    val balance = totalIncome - totalExpenses

                    _state.update {
                        it.copy(
                            isLoading = false,
                            transactions = transactions,
                            totalIncome = String.format("%.2f", totalIncome),
                            totalExpenses = String.format("%.2f", totalExpenses),
                            balance = String.format("%.2f", balance)
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load data"
                        )
                    }
                }
        }
    }
}