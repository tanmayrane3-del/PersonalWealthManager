package com.example.personalwealthmanager.data.repository

import com.example.personalwealthmanager.core.utils.SessionManager
import com.example.personalwealthmanager.data.remote.api.MetadataApi
import com.example.personalwealthmanager.data.remote.dto.CategoryDto
import com.example.personalwealthmanager.data.remote.dto.UpdateCategoryRequest
import com.example.personalwealthmanager.domain.model.Category
import com.example.personalwealthmanager.domain.repository.CategoryRepository
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val metadataApi: MetadataApi,
    private val sessionManager: SessionManager
) : CategoryRepository {

    override suspend fun getIncomeCategories(): Result<List<Category>> {
        return try {
            val sessionToken = sessionManager.getSessionToken()
                ?: return Result.failure(Exception("No session token"))

            val response = metadataApi.getIncomeCategories(sessionToken)

            if (response.isSuccessful && response.body()?.status == "success") {
                val categories = response.body()?.data?.map { it.toDomain() } ?: emptyList()
                Result.success(categories)
            } else {
                val errorMessage = response.body()?.reason ?: "Failed to fetch income categories"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getExpenseCategories(): Result<List<Category>> {
        return try {
            val sessionToken = sessionManager.getSessionToken()
                ?: return Result.failure(Exception("No session token"))

            val response = metadataApi.getExpenseCategories(sessionToken)

            if (response.isSuccessful && response.body()?.status == "success") {
                val categories = response.body()?.data?.map { it.toDomain() } ?: emptyList()
                Result.success(categories)
            } else {
                val errorMessage = response.body()?.reason ?: "Failed to fetch expense categories"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateCategory(
        type: String,
        categoryId: String,
        name: String?,
        description: String?,
        icon: String?
    ): Result<Category> {
        return try {
            val sessionToken = sessionManager.getSessionToken()
                ?: return Result.failure(Exception("No session token"))

            val request = UpdateCategoryRequest(
                name = name,
                description = description,
                icon = icon
            )

            val response = if (type == "income") {
                metadataApi.updateIncomeCategory(sessionToken, categoryId, request)
            } else {
                metadataApi.updateExpenseCategory(sessionToken, categoryId, request)
            }

            if (response.isSuccessful && response.body()?.status == "success") {
                val categoryDto = response.body()?.data
                if (categoryDto != null) {
                    Result.success(categoryDto.toDomain())
                } else {
                    Result.failure(Exception("Failed to update category"))
                }
            } else {
                val errorMessage = response.body()?.reason ?: "Failed to update category"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteCategory(type: String, categoryId: String): Result<Unit> {
        return try {
            val sessionToken = sessionManager.getSessionToken()
                ?: return Result.failure(Exception("No session token"))

            val response = if (type == "income") {
                metadataApi.deleteIncomeCategory(sessionToken, categoryId)
            } else {
                metadataApi.deleteExpenseCategory(sessionToken, categoryId)
            }

            if (response.isSuccessful && response.body()?.status == "success") {
                Result.success(Unit)
            } else {
                val errorMessage = response.body()?.reason ?: "Failed to delete category"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun CategoryDto.toDomain(): Category {
        return Category(
            id = this.id,
            name = this.name,
            description = this.description,
            icon = this.icon,
            color = this.color,
            type = this.type,
            isGlobal = this.isGlobal,
            isUserSpecific = this.isUserSpecific,
            transactionCount = this.transactionCount
        )
    }
}
