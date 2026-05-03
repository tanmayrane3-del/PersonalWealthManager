package com.pwm.personalwealthmanager.presentation.sources

import com.pwm.personalwealthmanager.domain.model.Category
import com.pwm.personalwealthmanager.domain.model.Source

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
