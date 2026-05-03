package com.pwm.personalwealthmanager.presentation.recipients

import com.pwm.personalwealthmanager.domain.model.Category
import com.pwm.personalwealthmanager.domain.model.Recipient

data class RecipientManagementState(
    val isLoading: Boolean = false,
    val recipients: List<Recipient> = emptyList(),
    val error: String? = null,
    val isGlobalExpanded: Boolean = true,
    val isUserExpanded: Boolean = true,
    val isCreating: Boolean = false,
    val isUpdating: Boolean = false,
    val isDeleting: Boolean = false,
    val createSuccess: Boolean = false,
    val updateSuccess: Boolean = false,
    val deleteSuccess: Boolean = false,
    val expenseCategories: List<Category> = emptyList(),
    val isCategoriesLoading: Boolean = false
)
