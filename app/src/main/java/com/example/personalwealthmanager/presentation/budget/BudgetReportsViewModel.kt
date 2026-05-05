package com.pwm.personalwealthmanager.presentation.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pwm.personalwealthmanager.core.utils.SessionManager
import com.pwm.personalwealthmanager.data.remote.dto.ReportsDto
import com.pwm.personalwealthmanager.domain.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ReportsUiState {
    object Idle    : ReportsUiState()
    object Loading : ReportsUiState()
    data class Success(val data: ReportsDto) : ReportsUiState()
    data class Error(val message: String)    : ReportsUiState()
}

@HiltViewModel
class BudgetReportsViewModel @Inject constructor(
    private val repository: BudgetRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow<ReportsUiState>(ReportsUiState.Idle)
    val state: StateFlow<ReportsUiState> = _state.asStateFlow()

    init { load() }

    fun load(months: Int = 12) {
        val token = sessionManager.getSessionToken() ?: return
        viewModelScope.launch {
            _state.value = ReportsUiState.Loading
            repository.getReports(token, months).fold(
                onSuccess = { _state.value = ReportsUiState.Success(it) },
                onFailure = { _state.value = ReportsUiState.Error(it.message ?: "Failed to load reports") }
            )
        }
    }
}
