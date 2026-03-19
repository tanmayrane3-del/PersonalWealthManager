package com.example.personalwealthmanager.presentation.mutualfunds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalwealthmanager.core.utils.SessionManager
import com.example.personalwealthmanager.data.remote.dto.AddLotRequest
import com.example.personalwealthmanager.domain.model.SchemeLookupResult
import com.example.personalwealthmanager.domain.repository.MutualFundRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MutualFundsViewModel @Inject constructor(
    private val repository: MutualFundRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<MutualFundsUiState>(MutualFundsUiState.Idle)
    val uiState: StateFlow<MutualFundsUiState> = _uiState

    private val _cagrState = MutableStateFlow<MutualFundsCagrState>(MutualFundsCagrState.Idle)
    val cagrState: StateFlow<MutualFundsCagrState> = _cagrState

    private val _expandedIsins = MutableStateFlow<Set<String>>(emptySet())
    val expandedIsins: StateFlow<Set<String>> = _expandedIsins

    private val _lookupState = MutableStateFlow<LookupState>(LookupState.Idle)
    val lookupState: StateFlow<LookupState> = _lookupState

    fun fetchAll() {
        viewModelScope.launch {
            _uiState.value = MutualFundsUiState.Loading
            val token = sessionManager.getSessionToken() ?: run {
                _uiState.value = MutualFundsUiState.Error("Not logged in")
                return@launch
            }
            repository.getHoldings(token).fold(
                onSuccess = { (funds, summary) ->
                    _uiState.value = MutualFundsUiState.Success(funds, summary)
                },
                onFailure = { e ->
                    _uiState.value = MutualFundsUiState.Error(e.message ?: "Failed to load holdings")
                }
            )
        }
    }

    fun fetchSummary() {
        viewModelScope.launch {
            val token = sessionManager.getSessionToken() ?: return@launch
            repository.getSummary(token).onSuccess { dto ->
                if (dto.hasCagr && dto.projected1y > dto.currentValue) {
                    _cagrState.value = MutualFundsCagrState.Available(
                        totalValue  = dto.currentValue,
                        projected1y = dto.projected1y,
                        projected3y = dto.projected3y,
                        projected5y = dto.projected5y
                    )
                }
            }
        }
    }

    fun syncCagrAndRefresh() {
        viewModelScope.launch {
            val token = sessionManager.getSessionToken() ?: return@launch
            _cagrState.value = MutualFundsCagrState.Syncing

            repository.syncCagr(token)

            repository.getSummary(token).onSuccess { dto ->
                _cagrState.value = if (dto.hasCagr && dto.projected1y > dto.currentValue) {
                    MutualFundsCagrState.Available(
                        totalValue  = dto.currentValue,
                        projected1y = dto.projected1y,
                        projected3y = dto.projected3y,
                        projected5y = dto.projected5y
                    )
                } else {
                    MutualFundsCagrState.Idle
                }
            }.onFailure {
                _cagrState.value = MutualFundsCagrState.Idle
            }
        }
    }

    fun addLot(request: AddLotRequest) {
        viewModelScope.launch {
            val token = sessionManager.getSessionToken() ?: return@launch
            repository.addLot(token, request).fold(
                onSuccess = { fetchAll() },
                onFailure = { e ->
                    _uiState.value = MutualFundsUiState.Error(e.message ?: "Failed to add holding")
                }
            )
        }
    }

    fun deleteLot(id: String) {
        viewModelScope.launch {
            val token = sessionManager.getSessionToken() ?: return@launch
            repository.deleteLot(token, id).fold(
                onSuccess = { fetchAll() },
                onFailure = { e ->
                    _uiState.value = MutualFundsUiState.Error(e.message ?: "Failed to delete holding")
                }
            )
        }
    }

    fun toggleExpanded(isin: String) {
        val current = _expandedIsins.value.toMutableSet()
        if (isin in current) current.remove(isin) else current.add(isin)
        _expandedIsins.value = current
    }

    fun lookupScheme(isin: String) {
        viewModelScope.launch {
            _lookupState.value = LookupState.Loading
            val token = sessionManager.getSessionToken() ?: run {
                _lookupState.value = LookupState.Error("Not logged in")
                return@launch
            }
            repository.lookupScheme(token, isin).fold(
                onSuccess = { _lookupState.value = LookupState.Found(it) },
                onFailure = { _lookupState.value = LookupState.Error(it.message ?: "Not found") }
            )
        }
    }

    fun resetLookup() { _lookupState.value = LookupState.Idle }
}

sealed class LookupState {
    object Idle    : LookupState()
    object Loading : LookupState()
    data class Found(val result: SchemeLookupResult) : LookupState()
    data class Error(val message: String) : LookupState()
}
