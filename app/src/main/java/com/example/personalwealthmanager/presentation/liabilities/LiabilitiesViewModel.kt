package com.example.personalwealthmanager.presentation.liabilities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalwealthmanager.core.utils.SessionManager
import com.example.personalwealthmanager.data.remote.dto.CreateLiabilityRequest
import com.example.personalwealthmanager.data.remote.dto.UpdateLiabilityRequest
import com.example.personalwealthmanager.domain.repository.LiabilityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LiabilitiesViewModel @Inject constructor(
    private val repository: LiabilityRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<LiabilitiesUiState>(LiabilitiesUiState.Idle)
    val uiState: StateFlow<LiabilitiesUiState> = _uiState

    private val _actionState = MutableStateFlow<LiabilitiesActionState>(LiabilitiesActionState.Idle)
    val actionState: StateFlow<LiabilitiesActionState> = _actionState

    fun fetchSummary() {
        viewModelScope.launch {
            _uiState.value = LiabilitiesUiState.Loading
            val token = sessionManager.getSessionToken() ?: run {
                _uiState.value = LiabilitiesUiState.Error("Not logged in")
                return@launch
            }
            repository.getSummary(token).fold(
                onSuccess = { summary ->
                    _uiState.value = LiabilitiesUiState.Success(summary)
                },
                onFailure = { e ->
                    _uiState.value = LiabilitiesUiState.Error(e.message ?: "Failed to load liabilities")
                }
            )
        }
    }

    fun createLiability(request: CreateLiabilityRequest) {
        viewModelScope.launch {
            _actionState.value = LiabilitiesActionState.Saving
            val token = sessionManager.getSessionToken() ?: run {
                _actionState.value = LiabilitiesActionState.Error("Not logged in")
                return@launch
            }
            repository.createLiability(token, request).fold(
                onSuccess = {
                    _actionState.value = LiabilitiesActionState.Saved
                    fetchSummary()
                },
                onFailure = { e ->
                    _actionState.value = LiabilitiesActionState.Error(e.message ?: "Failed to create liability")
                }
            )
        }
    }

    fun updateLiability(id: String, request: UpdateLiabilityRequest) {
        viewModelScope.launch {
            _actionState.value = LiabilitiesActionState.Saving
            val token = sessionManager.getSessionToken() ?: run {
                _actionState.value = LiabilitiesActionState.Error("Not logged in")
                return@launch
            }
            repository.updateLiability(token, id, request).fold(
                onSuccess = {
                    _actionState.value = LiabilitiesActionState.Saved
                    fetchSummary()
                },
                onFailure = { e ->
                    _actionState.value = LiabilitiesActionState.Error(e.message ?: "Failed to update liability")
                }
            )
        }
    }

    fun deleteLiability(id: String) {
        viewModelScope.launch {
            _actionState.value = LiabilitiesActionState.Deleting
            val token = sessionManager.getSessionToken() ?: run {
                _actionState.value = LiabilitiesActionState.Error("Not logged in")
                return@launch
            }
            repository.deleteLiability(token, id).fold(
                onSuccess = {
                    _actionState.value = LiabilitiesActionState.Saved
                    fetchSummary()
                },
                onFailure = { e ->
                    _actionState.value = LiabilitiesActionState.Error(e.message ?: "Failed to delete liability")
                }
            )
        }
    }

    fun resetActionState() {
        _actionState.value = LiabilitiesActionState.Idle
    }
}
