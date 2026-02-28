package com.example.personalwealthmanager.data.remote.api

import com.example.personalwealthmanager.data.remote.dto.ApiResponse
import com.example.personalwealthmanager.data.remote.dto.CreateTransactionRequest
import com.example.personalwealthmanager.data.remote.dto.TransactionDto
import com.example.personalwealthmanager.data.remote.dto.TransactionIdResponse
import com.example.personalwealthmanager.data.remote.dto.UpdateTransactionRequest
import retrofit2.Response
import retrofit2.http.*

interface TransactionApi {

    // ========== INCOME ENDPOINTS ==========

    @GET("api/income")
    suspend fun getIncome(
        @Header("x-session-token") sessionToken: String,
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null
    ): Response<ApiResponse<List<TransactionDto>>>

    @GET("api/income/{income_id}")
    suspend fun getIncomeById(
        @Header("x-session-token") sessionToken: String,
        @Path("income_id") id: String
    ): Response<ApiResponse<TransactionDto>>

    @POST("api/income")
    suspend fun createIncome(
        @Header("x-session-token") sessionToken: String,
        @Body request: CreateTransactionRequest
    ): Response<ApiResponse<TransactionIdResponse>>

    @PUT("api/income/{income_id}")
    suspend fun updateIncome(
        @Header("x-session-token") sessionToken: String,
        @Path("income_id") id: String,
        @Body request: UpdateTransactionRequest
    ): Response<ApiResponse<TransactionIdResponse>>

    @DELETE("api/income/{income_id}")
    suspend fun deleteIncome(
        @Header("x-session-token") sessionToken: String,
        @Path("income_id") id: String
    ): Response<ApiResponse<Unit>>

    // ========== EXPENSE ENDPOINTS ==========

    @GET("api/expenses")
    suspend fun getExpenses(
        @Header("x-session-token") sessionToken: String,
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null
    ): Response<ApiResponse<List<TransactionDto>>>

    @GET("api/expenses/{expense_id}")
    suspend fun getExpenseById(
        @Header("x-session-token") sessionToken: String,
        @Path("expense_id") id: String
    ): Response<ApiResponse<TransactionDto>>

    @POST("api/expenses")
    suspend fun createExpense(
        @Header("x-session-token") sessionToken: String,
        @Body request: CreateTransactionRequest
    ): Response<ApiResponse<TransactionIdResponse>>

    @PUT("api/expenses/{expense_id}")
    suspend fun updateExpense(
        @Header("x-session-token") sessionToken: String,
        @Path("expense_id") id: String,
        @Body request: UpdateTransactionRequest
    ): Response<ApiResponse<TransactionIdResponse>>

    @DELETE("api/expenses/{expense_id}")
    suspend fun deleteExpense(
        @Header("x-session-token") sessionToken: String,
        @Path("expense_id") id: String
    ): Response<ApiResponse<Unit>>
}