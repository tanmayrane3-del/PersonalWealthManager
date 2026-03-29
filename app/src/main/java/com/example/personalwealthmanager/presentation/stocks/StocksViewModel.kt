package com.example.personalwealthmanager.presentation.stocks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalwealthmanager.core.utils.SessionManager
import com.example.personalwealthmanager.domain.model.StocksPortfolioSummary
import com.example.personalwealthmanager.domain.repository.HoldingsRepository
import com.example.personalwealthmanager.domain.repository.ZerodhaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StocksViewModel @Inject constructor(
    private val holdingsRepository: HoldingsRepository,
    private val zerodhaRepository: ZerodhaRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow<StocksState>(StocksState.Idle)
    val state: StateFlow<StocksState> = _state

    private val _authUrlState = MutableStateFlow<String?>(null)
    val authUrlState: StateFlow<String?> = _authUrlState

    private val _summary = MutableStateFlow<StocksPortfolioSummary?>(null)
    val summary: StateFlow<StocksPortfolioSummary?> = _summary

    private var pollingJob: Job? = null

    fun loadHoldings() {
        val token = sessionManager.getSessionToken() ?: return
        viewModelScope.launch {
            _state.value = StocksState.Loading
            val result = holdingsRepository.getHoldings(token)
            result.fold(
                onSuccess = { holdings -> _state.value = StocksState.Success(holdings) },
                onFailure = { e ->
                    val msg = e.message ?: ""
                    when {
                        msg.contains("not authenticated", ignoreCase = true) ||
                        msg.contains("session expired", ignoreCase = true) -> _state.value = StocksState.NotAuthenticated
                        msg.contains("credentials not found", ignoreCase = true) -> _state.value = StocksState.CredentialsNotFound
                        else -> _state.value = StocksState.Error(msg)
                    }
                }
            )
        }
        loadSummary()
    }

    fun loadSummary() {
        val token = sessionManager.getSessionToken() ?: return
        viewModelScope.launch {
            holdingsRepository.getSummary(token).onSuccess { _summary.value = it }
        }
    }

    fun syncHoldings() {
        val token = sessionManager.getSessionToken() ?: return
        viewModelScope.launch {
            _state.value = StocksState.Loading
            val result = holdingsRepository.syncHoldings(token)
            result.fold(
                onSuccess = { holdings ->
                    _state.value = StocksState.Success(holdings)
                    loadSummary()
                    // CAGR is computed in the background by the backend after sync.
                    // Yahoo Finance chart API: ~2 s/stock → ~34 s for 17 stocks.
                    // Poll every 10 s (up to 20 attempts = ~3.5 min) until CAGR values appear.
                    if (holdings.any { it.cagr1y == null }) {
                        launch {
                            repeat(20) { _ ->
                                delay(10_000L)
                                val updated = holdingsRepository.getHoldings(token).getOrNull()
                                    ?: return@repeat
                                _state.value = StocksState.Success(updated)
                                if (updated.all { it.cagr1y != null }) {
                                    loadSummary()
                                    return@launch
                                }
                            }
                        }
                    }
                },
                onFailure = { e ->
                    val msg = e.message ?: ""
                    when {
                        msg.contains("not authenticated", ignoreCase = true) ||
                        msg.contains("session expired", ignoreCase = true) -> _state.value = StocksState.NotAuthenticated
                        msg.contains("credentials not found", ignoreCase = true) -> _state.value = StocksState.CredentialsNotFound
                        else -> _state.value = StocksState.Error(msg)
                    }
                }
            )
        }
    }

    fun fetchAuthUrl() {
        val token = sessionManager.getSessionToken() ?: return
        viewModelScope.launch {
            val result = zerodhaRepository.getAuthUrl(token)
            result.fold(
                onSuccess = { url -> _authUrlState.value = url },
                onFailure = { _authUrlState.value = null }
            )
        }
    }

    fun exchangeAndSync(requestToken: String) {
        val token = sessionManager.getSessionToken() ?: return
        viewModelScope.launch {
            _state.value = StocksState.Loading
            val exchangeResult = zerodhaRepository.exchangeToken(token, requestToken)
            if (exchangeResult.isFailure) {
                _state.value = StocksState.Error(exchangeResult.exceptionOrNull()?.message ?: "Token exchange failed")
                return@launch
            }
            syncHoldings()
        }
    }

    fun clearAuthUrl() {
        _authUrlState.value = null
    }

    fun getSessionToken(): String? = sessionManager.getSessionToken()

    // Silently refreshes holdings and summary every 5 minutes while the screen is visible.
    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(5 * 60 * 1000L)
                val token = sessionManager.getSessionToken() ?: break
                holdingsRepository.getHoldings(token)
                    .onSuccess { holdings -> _state.value = StocksState.Success(holdings) }
                holdingsRepository.getSummary(token)
                    .onSuccess { _summary.value = it }
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }
}
