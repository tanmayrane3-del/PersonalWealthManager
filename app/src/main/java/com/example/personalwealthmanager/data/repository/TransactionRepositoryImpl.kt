package com.example.personalwealthmanager.data.repository

import com.example.personalwealthmanager.core.utils.SessionManager
import com.example.personalwealthmanager.data.remote.api.MetadataApi
import com.example.personalwealthmanager.data.remote.api.TransactionApi
import com.example.personalwealthmanager.data.remote.dto.CreateCategoryRequest
import com.example.personalwealthmanager.data.remote.dto.CreateTransactionRequest
import com.example.personalwealthmanager.data.remote.dto.TransactionDto
import com.example.personalwealthmanager.data.remote.dto.UpdateTransactionRequest
import com.example.personalwealthmanager.domain.model.Category
import com.example.personalwealthmanager.domain.model.Recipient
import com.example.personalwealthmanager.domain.model.Source
import com.example.personalwealthmanager.domain.model.Transaction
import com.example.personalwealthmanager.domain.model.TransactionMetadata
import com.example.personalwealthmanager.domain.repository.TransactionRepository
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val transactionApi: TransactionApi,
    private val metadataApi: MetadataApi,
    private val sessionManager: SessionManager
) : TransactionRepository {

    override suspend fun getTransactions(
        dateFrom: String?,
        dateTo: String?
    ): Result<List<Transaction>> {
        return try {
            val sessionToken = sessionManager.getSessionToken()
                ?: return Result.failure(Exception("No session token"))

            // Fetch both income and expenses
            val incomeResponse = transactionApi.getIncome(sessionToken, dateFrom, dateTo)
            val expensesResponse = transactionApi.getExpenses(sessionToken, dateFrom, dateTo)

            val transactions = mutableListOf<Transaction>()

            // Process income
            if (incomeResponse.isSuccessful && incomeResponse.body()?.status == "success") {
                val incomeData = incomeResponse.body()?.data ?: emptyList()
                transactions.addAll(incomeData.map { it.toDomain("income") })
            }

            // Process expenses
            if (expensesResponse.isSuccessful && expensesResponse.body()?.status == "success") {
                val expensesData = expensesResponse.body()?.data ?: emptyList()
                transactions.addAll(expensesData.map { it.toDomain("expense") })
            }

            // Sort by date descending
            transactions.sortByDescending { it.date }

            Result.success(transactions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTransactionById(id: String, isIncome: Boolean): Result<Transaction> {
        return try {
            val sessionToken = sessionManager.getSessionToken()
                ?: return Result.failure(Exception("No session token"))

            val response = if (isIncome) {
                transactionApi.getIncomeById(sessionToken, id)
            } else {
                transactionApi.getExpenseById(sessionToken, id)
            }

            if (response.isSuccessful && response.body()?.status == "success") {
                val data = response.body()?.data
                if (data != null) {
                    val type = if (isIncome) "income" else "expense"
                    Result.success(data.toDomain(type))
                } else {
                    Result.failure(Exception("Transaction not found"))
                }
            } else {
                val errorMessage = response.body()?.reason ?: "Failed to fetch transaction"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createTransaction(
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
    ): Result<Unit> {
        return try {
            val sessionToken = sessionManager.getSessionToken()
                ?: return Result.failure(Exception("No session token"))

            val request = CreateTransactionRequest(
                date = date,
                time = time,
                amount = amount,
                currency = "INR",
                categoryId = categoryId,
                sourceId = if (type == "income") sourceRecipientId else null,
                recipientId = if (type == "expense") sourceRecipientId else null,
                paymentMethod = paymentMethod,
                transactionReference = transactionReference,
                tags = tags,
                notes = notes
            )

            val response = if (type == "income") {
                transactionApi.createIncome(sessionToken, request)
            } else {
                transactionApi.createExpense(sessionToken, request)
            }

            if (response.isSuccessful && response.body()?.status == "success") {
                Result.success(Unit)
            } else {
                val errorMessage = response.body()?.reason ?: "Failed to create transaction"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTransaction(
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
    ): Result<Unit> {
        return try {
            val sessionToken = sessionManager.getSessionToken()
                ?: return Result.failure(Exception("No session token"))

            val request = UpdateTransactionRequest(
                date = date,
                time = time,
                amount = amount,
                currency = "INR",
                categoryId = categoryId,
                sourceId = if (type == "income") sourceRecipientId else null,
                recipientId = if (type == "expense") sourceRecipientId else null,
                paymentMethod = paymentMethod,
                transactionReference = transactionReference,
                tags = tags,
                notes = notes
            )

            val response = if (type == "income") {
                transactionApi.updateIncome(sessionToken, id, request)
            } else {
                transactionApi.updateExpense(sessionToken, id, request)
            }

            if (response.isSuccessful && response.body()?.status == "success") {
                Result.success(Unit)
            } else {
                val errorMessage = response.body()?.reason ?: "Failed to update transaction"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTransaction(id: String, isIncome: Boolean): Result<Unit> {
        return try {
            val sessionToken = sessionManager.getSessionToken()
                ?: return Result.failure(Exception("No session token"))

            val response = if (isIncome) {
                transactionApi.deleteIncome(sessionToken, id)
            } else {
                transactionApi.deleteExpense(sessionToken, id)
            }

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMessage = response.body()?.reason ?: "Failed to delete transaction"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMetadata(): Result<TransactionMetadata> {
        return try {
            val sessionToken = sessionManager.getSessionToken()
                ?: return Result.failure(Exception("No session token"))

            val incomeCategoriesResponse = metadataApi.getIncomeCategories(sessionToken)
            val expenseCategoriesResponse = metadataApi.getExpenseCategories(sessionToken)
            val sourcesResponse = metadataApi.getSources(sessionToken)
            val recipientsResponse = metadataApi.getRecipients(sessionToken)

            val incomeCategories = if (incomeCategoriesResponse.isSuccessful) {
                incomeCategoriesResponse.body()?.data?.map { it.toDomain() } ?: emptyList()
            } else {
                emptyList()
            }

            val expenseCategories = if (expenseCategoriesResponse.isSuccessful) {
                expenseCategoriesResponse.body()?.data?.map { it.toDomain() } ?: emptyList()
            } else {
                emptyList()
            }

            val sources = if (sourcesResponse.isSuccessful) {
                sourcesResponse.body()?.data?.map { it.toDomain() } ?: emptyList()
            } else {
                emptyList()
            }

            val recipients = if (recipientsResponse.isSuccessful) {
                recipientsResponse.body()?.data?.map { it.toDomain() } ?: emptyList()
            } else {
                emptyList()
            }

            Result.success(
                TransactionMetadata(
                    incomeCategories = incomeCategories,
                    expenseCategories = expenseCategories,
                    sources = sources,
                    recipients = recipients
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createCategory(
        type: String,
        name: String,
        description: String?,
        icon: String?
    ): Result<Category> {
        return try {
            val sessionToken = sessionManager.getSessionToken()
                ?: return Result.failure(Exception("No session token"))

            val request = CreateCategoryRequest(
                name = name,
                description = description,
                icon = icon,
                color = "#FF5722"  // Fixed color as per requirement
            )

            val response = if (type == "income") {
                metadataApi.createIncomeCategory(sessionToken, request)
            } else {
                metadataApi.createExpenseCategory(sessionToken, request)
            }

            if (response.isSuccessful && response.body()?.status == "success") {
                val categoryDto = response.body()?.data
                if (categoryDto != null) {
                    Result.success(categoryDto.toDomain())
                } else {
                    Result.failure(Exception("Failed to create category"))
                }
            } else {
                val errorMessage = response.body()?.reason ?: "Failed to create category"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Extension function to convert DTO to Domain model
    private fun TransactionDto.toDomain(type: String): Transaction {
        return Transaction(
            transactionId = this.incomeId ?: this.expenseId ?: "",
            amount = this.amount,
            date = this.date,
            time = this.time,
            type = type,
            categoryName = this.categoryName,
            categoryIcon = this.categoryIcon,
            sourceName = this.sourceName,
            recipientName = this.recipientName,
            paymentMethod = this.paymentMethod,
            transactionReference = this.transactionReference,
            tags = this.tags ?: emptyList(),
            notes = this.notes
        )
    }

    private fun com.example.personalwealthmanager.data.remote.dto.CategoryDto.toDomain(): Category {
        return Category(
            id = this.id,
            name = this.name,
            description = this.description,
            icon = this.icon,
            color = this.color,
            type = this.type
        )
    }

    private fun com.example.personalwealthmanager.data.remote.dto.SourceDto.toDomain(): Source {
        return Source(
            id = this.id,
            name = this.name
        )
    }

    private fun com.example.personalwealthmanager.data.remote.dto.RecipientDto.toDomain(): Recipient {
        return Recipient(
            id = this.id,
            name = this.name
        )
    }
}