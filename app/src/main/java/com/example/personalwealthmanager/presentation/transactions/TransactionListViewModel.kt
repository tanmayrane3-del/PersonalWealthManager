package com.example.personalwealthmanager.presentation.transactions

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
class TransactionListViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TransactionListState())
    val state: StateFlow<TransactionListState> = _state.asStateFlow()

    init {
        loadMetadata()
    }

    fun loadTransactions() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val filter = _state.value.filter

            transactionRepository.getTransactions(
                dateFrom = filter.dateFrom,
                dateTo = filter.dateTo
            ).onSuccess { allTransactions ->
                // Apply local filters
                var filtered = allTransactions

                // Filter by type
                if (filter.type != "both") {
                    filtered = filtered.filter { it.type == filter.type }
                }

                // Filter by amount
                filter.minAmount?.toDoubleOrNull()?.let { min ->
                    filtered = filtered.filter { (it.amount.toDoubleOrNull() ?: 0.0) >= min }
                }
                filter.maxAmount?.toDoubleOrNull()?.let { max ->
                    filtered = filtered.filter { (it.amount.toDoubleOrNull() ?: 0.0) <= max }
                }

                _state.update {
                    it.copy(
                        isLoading = false,
                        transactions = filtered
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load transactions"
                    )
                }
            }
        }
    }

    private fun loadMetadata() {
        viewModelScope.launch {
            transactionRepository.getMetadata()
                .onSuccess { metadata ->
                    _state.update {
                        it.copy(
                            metadata = MetadataState(
                                incomeCategories = metadata.incomeCategories,
                                expenseCategories = metadata.expenseCategories,
                                sources = metadata.sources,
                                recipients = metadata.recipients
                            )
                        )
                    }
                }
        }
    }

    fun updateFilter(filter: FilterState) {
        _state.update { it.copy(filter = filter) }
    }

    fun resetFilters() {
        _state.update { it.copy(filter = FilterState()) }
    }

    fun deleteTransaction(transactionId: String, isIncome: Boolean) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transactionId, isIncome)
                .onSuccess {
                    loadTransactions()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(error = error.message ?: "Failed to delete transaction")
                    }
                }
        }
    }
}
