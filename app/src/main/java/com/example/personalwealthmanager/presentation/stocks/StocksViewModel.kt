package com.example.personalwealthmanager.presentation.stocks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalwealthmanager.core.utils.SessionManager
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
    }

    fun syncHoldings() {
        val token = sessionManager.getSessionToken() ?: return
        viewModelScope.launch {
            _state.value = StocksState.Loading
            val result = holdingsRepository.syncHoldings(token)
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

    // Silently refreshes holdings from DB every 5 minutes while the screen is visible.
    // Does not show a loading spinner — existing data stays on screen until new data arrives.
    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(5 * 60 * 1000L)
                val token = sessionManager.getSessionToken() ?: break
                val result = holdingsRepository.getHoldings(token)
                result.onSuccess { holdings -> _state.value = StocksState.Success(holdings) }
                // On failure: keep showing existing data — don't change state
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }
}
