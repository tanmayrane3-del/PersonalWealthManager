package com.example.personalwealthmanager.presentation.mutualfunds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalwealthmanager.core.utils.SessionManager
import com.example.personalwealthmanager.data.remote.dto.CasPreviewData
import com.example.personalwealthmanager.data.remote.dto.CasPreviewFundDto
import com.example.personalwealthmanager.data.remote.dto.ConfirmImportRequest
import com.example.personalwealthmanager.data.remote.dto.ImportResultDto
import com.example.personalwealthmanager.domain.repository.MutualFundRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

sealed class CasImportUiState {
    object Idle         : CasImportUiState()
    object Uploading    : CasImportUiState()
    data class Preview(val data: CasPreviewData) : CasImportUiState()
    object Confirming   : CasImportUiState()
    data class Done(val result: ImportResultDto) : CasImportUiState()
    data class Error(val message: String) : CasImportUiState()
}

@HiltViewModel
class CasImportViewModel @Inject constructor(
    private val repository: MutualFundRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<CasImportUiState>(CasImportUiState.Idle)
    val uiState: StateFlow<CasImportUiState> = _uiState

    fun uploadCas(pdfBytes: ByteArray, fileName: String) {
        viewModelScope.launch {
            _uiState.value = CasImportUiState.Uploading
            val token = sessionManager.getSessionToken() ?: run {
                _uiState.value = CasImportUiState.Error("Not logged in")
                return@launch
            }
            val requestBody = pdfBytes.toRequestBody("application/pdf".toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("pdf", fileName, requestBody)

            repository.uploadCas(token, part).fold(
                onSuccess = { _uiState.value = CasImportUiState.Preview(it) },
                onFailure = { _uiState.value = CasImportUiState.Error(it.message ?: "Upload failed") }
            )
        }
    }

    fun confirmImport(selectedFunds: List<CasPreviewFundDto>) {
        viewModelScope.launch {
            _uiState.value = CasImportUiState.Confirming
            val token = sessionManager.getSessionToken() ?: run {
                _uiState.value = CasImportUiState.Error("Not logged in")
                return@launch
            }
            repository.confirmCasImport(token, ConfirmImportRequest(selectedFunds)).fold(
                onSuccess = { _uiState.value = CasImportUiState.Done(it) },
                onFailure = { _uiState.value = CasImportUiState.Error(it.message ?: "Import failed") }
            )
        }
    }

    fun resetState() { _uiState.value = CasImportUiState.Idle }
}
