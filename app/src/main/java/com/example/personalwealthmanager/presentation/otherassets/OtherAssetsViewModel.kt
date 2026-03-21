package com.example.personalwealthmanager.presentation.otherassets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalwealthmanager.core.utils.SessionManager
import com.example.personalwealthmanager.data.remote.dto.CreatePhysicalAssetRequest
import com.example.personalwealthmanager.data.remote.dto.UpdatePhysicalAssetRequest
import com.example.personalwealthmanager.domain.repository.PhysicalAssetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OtherAssetsViewModel @Inject constructor(
    private val repository: PhysicalAssetRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<OtherAssetsUiState>(OtherAssetsUiState.Idle)
    val uiState: StateFlow<OtherAssetsUiState> = _uiState

    private val _actionState = MutableStateFlow<OtherAssetsActionState>(OtherAssetsActionState.Idle)
    val actionState: StateFlow<OtherAssetsActionState> = _actionState

    fun fetchSummary() {
        viewModelScope.launch {
            _uiState.value = OtherAssetsUiState.Loading
            val token = sessionManager.getSessionToken() ?: run {
                _uiState.value = OtherAssetsUiState.Error("Not logged in")
                return@launch
            }
            repository.getSummary(token).fold(
                onSuccess = { summary ->
                    _uiState.value = OtherAssetsUiState.Success(summary)
                },
                onFailure = { e ->
                    _uiState.value = OtherAssetsUiState.Error(e.message ?: "Failed to load assets")
                }
            )
        }
    }

    fun createAsset(request: CreatePhysicalAssetRequest) {
        viewModelScope.launch {
            _actionState.value = OtherAssetsActionState.Saving
            val token = sessionManager.getSessionToken() ?: run {
                _actionState.value = OtherAssetsActionState.Error("Not logged in")
                return@launch
            }
            repository.createAsset(token, request).fold(
                onSuccess = {
                    _actionState.value = OtherAssetsActionState.Saved
                    fetchSummary()
                },
                onFailure = { e ->
                    _actionState.value = OtherAssetsActionState.Error(e.message ?: "Failed to create asset")
                }
            )
        }
    }

    fun updateAsset(id: String, request: UpdatePhysicalAssetRequest) {
        viewModelScope.launch {
            _actionState.value = OtherAssetsActionState.Saving
            val token = sessionManager.getSessionToken() ?: run {
                _actionState.value = OtherAssetsActionState.Error("Not logged in")
                return@launch
            }
            repository.updateAsset(token, id, request).fold(
                onSuccess = {
                    _actionState.value = OtherAssetsActionState.Saved
                    fetchSummary()
                },
                onFailure = { e ->
                    _actionState.value = OtherAssetsActionState.Error(e.message ?: "Failed to update asset")
                }
            )
        }
    }

    fun deleteAsset(id: String) {
        viewModelScope.launch {
            _actionState.value = OtherAssetsActionState.Deleting
            val token = sessionManager.getSessionToken() ?: run {
                _actionState.value = OtherAssetsActionState.Error("Not logged in")
                return@launch
            }
            repository.deleteAsset(token, id).fold(
                onSuccess = {
                    _actionState.value = OtherAssetsActionState.Saved
                    fetchSummary()
                },
                onFailure = { e ->
                    _actionState.value = OtherAssetsActionState.Error(e.message ?: "Failed to delete asset")
                }
            )
        }
    }

    fun resetActionState() {
        _actionState.value = OtherAssetsActionState.Idle
    }
}
