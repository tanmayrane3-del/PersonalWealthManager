package com.pwm.personalwealthmanager.presentation.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pwm.personalwealthmanager.core.utils.SessionManager
import com.pwm.personalwealthmanager.data.remote.dto.MonthlyPlanDto
import com.pwm.personalwealthmanager.domain.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

sealed class DashboardUiState {
    object Idle    : DashboardUiState()
    object Loading : DashboardUiState()
    data class Success(val data: MonthlyPlanDto) : DashboardUiState()
    object Empty   : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}

@HiltViewModel
class BudgetDashboardViewModel @Inject constructor(
    private val repository: BudgetRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _dashboardState = MutableStateFlow<DashboardUiState>(DashboardUiState.Idle)
    val dashboardState: StateFlow<DashboardUiState> = _dashboardState.asStateFlow()

    private val _selectedMonth = MutableStateFlow(repository.currentMonth())
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    /** Last 13 months (current + 12 past), newest first. */
    val availableMonths: List<String> = buildList {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        repeat(13) {
            val year  = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) + 1
            add("$year-${month.toString().padStart(2, '0')}-01")
            cal.add(Calendar.MONTH, -1)
        }
    }

    init {
        loadDashboard()
    }

    fun loadDashboard(month: String = _selectedMonth.value) {
        val token = sessionManager.getSessionToken() ?: return
        viewModelScope.launch {
            _dashboardState.value = DashboardUiState.Loading
            repository.getDashboard(token, month).fold(
                onSuccess = { dto ->
                    _dashboardState.value = DashboardUiState.Success(dto)
                },
                onFailure = { e ->
                    val msg = e.message ?: "Failed to load"
                    if (msg.contains("404") || msg.contains("No plan")) {
                        _dashboardState.value = DashboardUiState.Empty
                    } else {
                        _dashboardState.value = DashboardUiState.Error(msg)
                    }
                }
            )
        }
    }

    fun selectMonth(month: String) {
        _selectedMonth.update { month }
        loadDashboard(month)
    }

    fun refresh() = loadDashboard(_selectedMonth.value)

    fun isCurrentMonth(): Boolean = _selectedMonth.value == repository.currentMonth()
}
