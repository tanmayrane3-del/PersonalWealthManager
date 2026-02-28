package com.example.personalwealthmanager.presentation.sources

import com.example.personalwealthmanager.domain.model.Category
import com.example.personalwealthmanager.domain.model.Source

data class SourceManagementState(
    val isLoading: Boolean = false,
    val sources: List<Source> = emptyList(),
    val incomeCategories: List<Category> = emptyList(),
    val error: String? = null,
    val isGlobalExpanded: Boolean = true,
    val isUserExpanded: Boolean = true,
    val isCreating: Boolean = false,
    val isUpdating: Boolean = false,
    val isDeleting: Boolean = false,
    val createSuccess: Boolean = false,
    val updateSuccess: Boolean = false,
    val deleteSuccess: Boolean = false
)
