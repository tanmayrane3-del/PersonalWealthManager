package com.example.personalwealthmanager.presentation.transactions

import com.example.personalwealthmanager.domain.model.Category
import com.example.personalwealthmanager.domain.model.Recipient
import com.example.personalwealthmanager.domain.model.Source
import com.example.personalwealthmanager.domain.model.Transaction

data class TransactionDetailState(
    val isLoading: Boolean = false,
    val transaction: Transaction? = null,
    val categories: List<Category> = emptyList(),
    val expenseCategories: List<Category> = emptyList(),
    val sources: List<Source> = emptyList(),
    val recipients: List<Recipient> = emptyList(),
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null,
    // Category creation state
    val isCreatingCategory: Boolean = false,
    val categoryCreated: Category? = null,
    val categoryCreationError: String? = null,
    // Source creation state
    val isCreatingSource: Boolean = false,
    val sourceCreated: Source? = null,
    val sourceCreationError: String? = null,
    // Recipient creation state
    val isCreatingRecipient: Boolean = false,
    val recipientCreated: Recipient? = null,
    val recipientCreationError: String? = null
)
