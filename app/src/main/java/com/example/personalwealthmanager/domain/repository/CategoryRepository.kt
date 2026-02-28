package com.example.personalwealthmanager.domain.repository

import com.example.personalwealthmanager.domain.model.Category

interface CategoryRepository {
    suspend fun getIncomeCategories(): Result<List<Category>>

    suspend fun getExpenseCategories(): Result<List<Category>>

    suspend fun updateCategory(
        type: String,  // "income" or "expense"
        categoryId: String,
        name: String?,
        description: String?,
        icon: String?
    ): Result<Category>

    suspend fun deleteCategory(
        type: String,  // "income" or "expense"
        categoryId: String
    ): Result<Unit>
}
