package com.example.personalwealthmanager.presentation.categories

import com.example.personalwealthmanager.domain.model.Category

data class CategoryManagementState(
    val isLoading: Boolean = false,
    val incomeCategories: List<Category> = emptyList(),
    val expenseCategories: List<Category> = emptyList(),
    val error: String? = null,
    val isIncomeExpanded: Boolean = true,
    val isExpenseExpanded: Boolean = true,
    val isUpdating: Boolean = false,
    val isDeleting: Boolean = false,
    val updateSuccess: Boolean = false,
    val deleteSuccess: Boolean = false
)
