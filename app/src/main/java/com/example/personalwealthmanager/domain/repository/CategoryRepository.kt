package com.pwm.personalwealthmanager.domain.repository

import com.pwm.personalwealthmanager.domain.model.Category

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
