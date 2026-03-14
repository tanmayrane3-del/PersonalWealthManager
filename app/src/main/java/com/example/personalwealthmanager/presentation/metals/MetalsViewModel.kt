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
