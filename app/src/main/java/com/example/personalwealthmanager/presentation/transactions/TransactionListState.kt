package com.example.personalwealthmanager.presentation.transactions

import com.example.personalwealthmanager.domain.model.Category
import com.example.personalwealthmanager.domain.model.Recipient
import com.example.personalwealthmanager.domain.model.Source
import com.example.personalwealthmanager.domain.model.Transaction

data class TransactionListState(
    val isLoading: Boolean = false,
    val transactions: List<Transaction> = emptyList(),
    val filter: FilterState = FilterState(),
    val metadata: MetadataState = MetadataState(),
    val error: String? = null
)

data class FilterState(
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val type: String = "both", // "both", "income", "expense"
    val categoryId: String? = null,
    val sourceId: String? = null,
    val recipientId: String? = null,
    val minAmount: String? = null,
    val maxAmount: String? = null
)

data class MetadataState(
    val incomeCategories: List<Category> = emptyList(),
    val expenseCategories: List<Category> = emptyList(),
    val sources: List<Source> = emptyList(),
    val recipients: List<Recipient> = emptyList()
)
