package com.example.personalwealthmanager.presentation.metals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalwealthmanager.core.utils.SessionManager
import com.example.personalwealthmanager.data.remote.dto.MetalHoldingRequest
import com.example.personalwealthmanager.domain.repository.MetalsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MetalsViewModel @Inject constructor(
    private val metalsRepository: MetalsRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<MetalsUiState>(MetalsUiState.Idle)
    val uiState: StateFlow<MetalsUiState> = _uiState

    private val _cagrState = MutableStateFlow<MetalsCagrState>(MetalsCagrState.Idle)
    val cagrState: StateFlow<MetalsCagrState> = _cagrState

    /** Fetches CAGR summary from DB (no computation). Called on screen open. */
    fun fetchSummary() {
        viewModelScope.launch {
            val token = sessionManager.getSessionToken() ?: return@launch
            metalsRepository.getSummary(token).onSuccess { dto ->
                if (dto.hasCagr && dto.projected1y > dto.totalValue) {
                    _cagrState.value = MetalsCagrState.Available(
                        totalValue  = dto.totalValue,
                        projected1y = dto.projected1y,
                        projected3y = dto.projected3y,
                        projected5y = dto.projected5y
                    )
                }
                // else leave Idle — projections column stays hidden
            }
        }
    }

    /**
     * Smart sync: if CAGR missing in DB, computes it first (may take ~10s).
     * Then fetches and publishes the updated summary.
     */
    fun syncCagrAndRefresh() {
        viewModelScope.launch {
            val token = sessionManager.getSessionToken() ?: return@launch
            _cagrState.value = MetalsCagrState.Syncing

            val summary = metalsRepository.getSummary(token).getOrNull()
            if (summary != null && !summary.hasCagr) {
                metalsRepository.syncCagr(token)  // await computation
            }

            metalsRepository.getSummary(token).onSuccess { dto ->
                _cagrState.value = if (dto.hasCagr && dto.projected1y > dto.totalValue) {
                    MetalsCagrState.Available(
                        totalValue  = dto.totalValue,
                        projected1y = dto.projected1y,
                        projected3y = dto.projected3y,
                        projected5y = dto.projected5y
                    )
                } else {
                    MetalsCagrState.Idle
                }
            }.onFailure {
                _cagrState.value = MetalsCagrState.Idle
            }
        }
    }

    fun fetchAll() {
        viewModelScope.launch {
            _uiState.value = MetalsUiState.Loading
            val token = sessionManager.getSessionToken() ?: run {
                _uiState.value = MetalsUiState.Error("Not logged in")
                return@launch
            }
            metalsRepository.getHoldings(token).fold(
                onSuccess = { (holdings, rates, totalValue) ->
                    _uiState.value = MetalsUiState.Success(holdings, rates, totalValue)
                },
                onFailure = { e ->
                    _uiState.value = MetalsUiState.Error(e.message ?: "Failed to load holdings")
                }
            )
        }
    }

    fun addHolding(request: MetalHoldingRequest) {
        viewModelScope.launch {
            val token = sessionManager.getSessionToken() ?: return@launch
            metalsRepository.addHolding(token, request).fold(
                onSuccess = { fetchAll() },
                onFailure = { e ->
                    _uiState.value = MetalsUiState.Error(e.message ?: "Failed to add holding")
                }
            )
        }
    }

    fun updateHolding(id: String, request: MetalHoldingRequest) {
        viewModelScope.launch {
            val token = sessionManager.getSessionToken() ?: return@launch
            metalsRepository.updateHolding(token, id, request).fold(
                onSuccess = { fetchAll() },
                onFailure = { e ->
                    _uiState.value = MetalsUiState.Error(e.message ?: "Failed to update holding")
                }
            )
        }
    }

    fun deleteHolding(id: String) {
        viewModelScope.launch {
            val token = sessionManager.getSessionToken() ?: return@launch
            metalsRepository.deleteHolding(token, id).fold(
                onSuccess = { fetchAll() },
                onFailure = { e ->
                    _uiState.value = MetalsUiState.Error(e.message ?: "Failed to delete holding")
                }
            )
        }
    }
}
