package com.example.personalwealthmanager.presentation.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalwealthmanager.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryManagementViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CategoryManagementState())
    val state: StateFlow<CategoryManagementState> = _state.asStateFlow()

    init {
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            var incomeCategories = emptyList<com.example.personalwealthmanager.domain.model.Category>()
            var expenseCategories = emptyList<com.example.personalwealthmanager.domain.model.Category>()
            var hasError = false
            var errorMessage = ""

            // Load income categories
            categoryRepository.getIncomeCategories()
                .onSuccess { categories ->
                    incomeCategories = categories
                }
                .onFailure { error ->
                    hasError = true
                    errorMessage = error.message ?: "Failed to load income categories"
                }

            // Load expense categories
            categoryRepository.getExpenseCategories()
                .onSuccess { categories ->
                    expenseCategories = categories
                }
                .onFailure { error ->
                    hasError = true
                    errorMessage = error.message ?: "Failed to load expense categories"
                }

            _state.update {
                it.copy(
                    isLoading = false,
                    incomeCategories = incomeCategories,
                    expenseCategories = expenseCategories,
                    error = if (hasError) errorMessage else null
                )
            }
        }
    }

    fun toggleIncomeSection() {
        _state.update { it.copy(isIncomeExpanded = !it.isIncomeExpanded) }
    }

    fun toggleExpenseSection() {
        _state.update { it.copy(isExpenseExpanded = !it.isExpenseExpanded) }
    }

    fun updateCategory(
        type: String,
        categoryId: String,
        name: String?,
        description: String?,
        icon: String?
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isUpdating = true, error = null, updateSuccess = false) }

            categoryRepository.updateCategory(type, categoryId, name, description, icon)
                .onSuccess {
                    _state.update { it.copy(isUpdating = false, updateSuccess = true) }
                    loadCategories()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isUpdating = false,
                            error = error.message ?: "Failed to update category"
                        )
                    }
                }
        }
    }

    fun deleteCategory(type: String, categoryId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true, error = null, deleteSuccess = false) }

            categoryRepository.deleteCategory(type, categoryId)
                .onSuccess {
                    _state.update { it.copy(isDeleting = false, deleteSuccess = true) }
                    loadCategories()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isDeleting = false,
                            error = error.message ?: "Failed to delete category"
                        )
                    }
                }
        }
    }

    fun clearSuccessStates() {
        _state.update { it.copy(updateSuccess = false, deleteSuccess = false) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
