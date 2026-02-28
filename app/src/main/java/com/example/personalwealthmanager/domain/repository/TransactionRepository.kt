package com.example.personalwealthmanager.domain.repository

import com.example.personalwealthmanager.domain.model.Category
import com.example.personalwealthmanager.domain.model.Transaction
import com.example.personalwealthmanager.domain.model.TransactionMetadata

interface TransactionRepository {
    suspend fun getTransactions(
        dateFrom: String? = null,
        dateTo: String? = null
    ): Result<List<Transaction>>

    suspend fun getTransactionById(id: String, isIncome: Boolean): Result<Transaction>

    suspend fun createTransaction(
        type: String,
        date: String,
        time: String,
        amount: String,
        categoryId: String,
        sourceRecipientId: String,
        paymentMethod: String,
        transactionReference: String?,
        tags: List<String>?,
        notes: String?
    ): Result<Unit>

    suspend fun updateTransaction(
        id: String,
        type: String,
        date: String,
        time: String,
        amount: String,
        categoryId: String,
        sourceRecipientId: String,
        paymentMethod: String,
        transactionReference: String?,
        tags: List<String>?,
        notes: String?
    ): Result<Unit>

    suspend fun deleteTransaction(id: String, isIncome: Boolean): Result<Unit>

    suspend fun getMetadata(): Result<TransactionMetadata>

    suspend fun createCategory(
        type: String,  // "income" or "expense"
        name: String,
        description: String?,
        icon: String?
    ): Result<Category>
}
