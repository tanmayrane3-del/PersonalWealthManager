package com.example.personalwealthmanager.presentation.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalwealthmanager.domain.repository.CategoryRepository
import com.example.personalwealthmanager.domain.repository.SourceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SourceManagementViewModel @Inject constructor(
    private val sourceRepository: SourceRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SourceManagementState())
    val state: StateFlow<SourceManagementState> = _state.asStateFlow()

    init {
        loadSources()
        loadIncomeCategories()
    }

    fun loadSources() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            sourceRepository.getSources()
                .onSuccess { sources ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            sources = sources
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load income sources"
                        )
                    }
                }
        }
    }

    private fun loadIncomeCategories() {
        viewModelScope.launch {
            categoryRepository.getIncomeCategories()
                .onSuccess { categories ->
                    _state.update { it.copy(incomeCategories = categories) }
                }
                .onFailure { /* silently ignore — categories are optional for the UI */ }
        }
    }

    fun toggleGlobalSection() {
        _state.update { it.copy(isGlobalExpanded = !it.isGlobalExpanded) }
    }

    fun toggleUserSection() {
        _state.update { it.copy(isUserExpanded = !it.isUserExpanded) }
    }

    fun createSource(
        name: String,
        description: String?,
        type: String?,
        contactInfo: String?,
        sourceIdentifier: String?,
        defaultCategoryId: String? = null
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isCreating = true, error = null, createSuccess = false) }

            sourceRepository.createSource(name, description, type, contactInfo, sourceIdentifier, defaultCategoryId)
                .onSuccess {
                    _state.update { it.copy(isCreating = false, createSuccess = true) }
                    loadSources()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isCreating = false,
                            error = error.message ?: "Failed to create income source"
                        )
                    }
                }
        }
    }

    fun updateSource(
        sourceId: String,
        name: String?,
        description: String?,
        type: String?,
        contactInfo: String?,
        sourceIdentifier: String?,
        defaultCategoryId: String? = null
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isUpdating = true, error = null, updateSuccess = false) }

            sourceRepository.updateSource(sourceId, name, description, type, contactInfo, sourceIdentifier, defaultCategoryId)
                .onSuccess {
                    _state.update { it.copy(isUpdating = false, updateSuccess = true) }
                    loadSources()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isUpdating = false,
                            error = error.message ?: "Failed to update income source"
                        )
                    }
                }
        }
    }

    fun deleteSource(sourceId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true, error = null, deleteSuccess = false) }

            sourceRepository.deleteSource(sourceId)
                .onSuccess {
                    _state.update { it.copy(isDeleting = false, deleteSuccess = true) }
                    loadSources()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isDeleting = false,
                            error = error.message ?: "Failed to delete income source"
                        )
                    }
                }
        }
    }

    fun clearSuccessStates() {
        _state.update { it.copy(createSuccess = false, updateSuccess = false, deleteSuccess = false) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
