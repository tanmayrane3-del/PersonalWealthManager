package com.example.personalwealthmanager.presentation.recipients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalwealthmanager.domain.repository.CategoryRepository
import com.example.personalwealthmanager.domain.repository.RecipientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecipientManagementViewModel @Inject constructor(
    private val recipientRepository: RecipientRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RecipientManagementState())
    val state: StateFlow<RecipientManagementState> = _state.asStateFlow()

    init {
        loadRecipients()
        loadExpenseCategories()
    }

    fun loadRecipients() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            recipientRepository.getRecipients()
                .onSuccess { recipients ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            recipients = recipients
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load recipients"
                        )
                    }
                }
        }
    }

    fun loadExpenseCategories() {
        viewModelScope.launch {
            _state.update { it.copy(isCategoriesLoading = true) }
            categoryRepository.getExpenseCategories()
                .onSuccess { categories ->
                    _state.update { it.copy(expenseCategories = categories, isCategoriesLoading = false) }
                }
                .onFailure {
                    _state.update { it.copy(isCategoriesLoading = false) }
                }
        }
    }

    fun toggleGlobalSection() {
        _state.update { it.copy(isGlobalExpanded = !it.isGlobalExpanded) }
    }

    fun toggleUserSection() {
        _state.update { it.copy(isUserExpanded = !it.isUserExpanded) }
    }

    fun createRecipient(
        name: String,
        type: String?,
        description: String?,
        contactInfo: String?,
        isFavorite: Boolean,
        paymentIdentifiers: List<String> = emptyList(),
        defaultCategoryId: String? = null
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isCreating = true, error = null, createSuccess = false) }

            recipientRepository.createRecipient(
                name, type, description, contactInfo, isFavorite,
                paymentIdentifiers, defaultCategoryId
            )
                .onSuccess {
                    _state.update { it.copy(isCreating = false, createSuccess = true) }
                    loadRecipients()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isCreating = false,
                            error = error.message ?: "Failed to create recipient"
                        )
                    }
                }
        }
    }

    fun updateRecipient(
        recipientId: String,
        name: String?,
        type: String?,
        description: String?,
        contactInfo: String?,
        isFavorite: Boolean?,
        paymentIdentifiers: List<String> = emptyList(),
        defaultCategoryId: String? = null
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isUpdating = true, error = null, updateSuccess = false) }

            recipientRepository.updateRecipient(
                recipientId, name, type, description, contactInfo, isFavorite,
                paymentIdentifiers, defaultCategoryId
            )
                .onSuccess {
                    _state.update { it.copy(isUpdating = false, updateSuccess = true) }
                    loadRecipients()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isUpdating = false,
                            error = error.message ?: "Failed to update recipient"
                        )
                    }
                }
        }
    }

    fun deleteRecipient(recipientId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true, error = null, deleteSuccess = false) }

            recipientRepository.deleteRecipient(recipientId)
                .onSuccess {
                    _state.update { it.copy(isDeleting = false, deleteSuccess = true) }
                    loadRecipients()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isDeleting = false,
                            error = error.message ?: "Failed to delete recipient"
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
