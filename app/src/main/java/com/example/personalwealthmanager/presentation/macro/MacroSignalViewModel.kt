package com.example.personalwealthmanager.presentation.macro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalwealthmanager.core.utils.SessionManager
import com.example.personalwealthmanager.data.remote.dto.MacroAccuracyDto
import com.example.personalwealthmanager.data.remote.dto.MacroBacktestResponseDto
import com.example.personalwealthmanager.data.remote.dto.MacroHistoryItemDto
import com.example.personalwealthmanager.data.remote.dto.MacroSignalDto
import com.example.personalwealthmanager.domain.repository.MacroRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class MacroUiState {
    object Loading : MacroUiState()
    data class Success(
        val signal: MacroSignalDto?,
        val history: List<MacroHistoryItemDto>,
        val accuracy: List<MacroAccuracyDto>,
        val backtest: MacroBacktestResponseDto?
    ) : MacroUiState()
    data class Error(val message: String) : MacroUiState()
}

@HiltViewModel
class MacroSignalViewModel @Inject constructor(
    private val macroRepository: MacroRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<MacroUiState>(MacroUiState.Loading)
    val uiState: StateFlow<MacroUiState> = _uiState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = MacroUiState.Loading
            val token = sessionManager.getSessionToken() ?: run {
                _uiState.value = MacroUiState.Error("Not logged in")
                return@launch
            }
            val signalDeferred   = async { macroRepository.getSignal(token) }
            val historyDeferred  = async { macroRepository.getHistory(token) }
            val accuracyDeferred = async { macroRepository.getAccuracy(token) }
            val backtestDeferred = async { macroRepository.getBacktest(token) }

            val signalResult   = signalDeferred.await()
            val historyResult  = historyDeferred.await()
            val accuracyResult = accuracyDeferred.await()
            val backtestResult = backtestDeferred.await()

            if (signalResult.isFailure) {
                _uiState.value = MacroUiState.Error(
                    signalResult.exceptionOrNull()?.message ?: "Failed to load signal"
                )
                return@launch
            }
            _uiState.value = MacroUiState.Success(
                signal   = signalResult.getOrNull(),
                history  = historyResult.getOrDefault(emptyList()),
                accuracy = accuracyResult.getOrDefault(emptyList()),
                backtest = backtestResult.getOrNull()
            )
        }
    }
}
