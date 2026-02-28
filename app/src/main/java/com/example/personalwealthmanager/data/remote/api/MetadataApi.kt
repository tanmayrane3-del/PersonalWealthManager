package com.example.personalwealthmanager.data.remote.api

import com.example.personalwealthmanager.data.remote.dto.ApiResponse
import com.example.personalwealthmanager.data.remote.dto.CategoryDto
import com.example.personalwealthmanager.data.remote.dto.CreateCategoryRequest
import com.example.personalwealthmanager.data.remote.dto.CreateRecipientRequest
import com.example.personalwealthmanager.data.remote.dto.CreateSourceRequest
import com.example.personalwealthmanager.data.remote.dto.RecipientDto
import com.example.personalwealthmanager.data.remote.dto.SourceDto
import com.example.personalwealthmanager.data.remote.dto.UpdateCategoryRequest
import com.example.personalwealthmanager.data.remote.dto.UpdateRecipientRequest
import com.example.personalwealthmanager.data.remote.dto.UpdateSourceRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface MetadataApi {

    @GET("api/categories/income")
    suspend fun getIncomeCategories(
        @Header("x-session-token") sessionToken: String
    ): Response<ApiResponse<List<CategoryDto>>>

    @GET("api/categories/expense")
    suspend fun getExpenseCategories(
        @Header("x-session-token") sessionToken: String
    ): Response<ApiResponse<List<CategoryDto>>>

    @POST("api/categories/income")
    suspend fun createIncomeCategory(
        @Header("x-session-token") sessionToken: String,
        @Body request: CreateCategoryRequest
    ): Response<ApiResponse<CategoryDto>>

    @POST("api/categories/expense")
    suspend fun createExpenseCategory(
        @Header("x-session-token") sessionToken: String,
        @Body request: CreateCategoryRequest
    ): Response<ApiResponse<CategoryDto>>

    @PUT("api/categories/income/{id}")
    suspend fun updateIncomeCategory(
        @Header("x-session-token") sessionToken: String,
        @Path("id") categoryId: String,
        @Body request: UpdateCategoryRequest
    ): Response<ApiResponse<CategoryDto>>

    @PUT("api/categories/expense/{id}")
    suspend fun updateExpenseCategory(
        @Header("x-session-token") sessionToken: String,
        @Path("id") categoryId: String,
        @Body request: UpdateCategoryRequest
    ): Response<ApiResponse<CategoryDto>>

    @DELETE("api/categories/income/{id}")
    suspend fun deleteIncomeCategory(
        @Header("x-session-token") sessionToken: String,
        @Path("id") categoryId: String
    ): Response<ApiResponse<Unit>>

    @DELETE("api/categories/expense/{id}")
    suspend fun deleteExpenseCategory(
        @Header("x-session-token") sessionToken: String,
        @Path("id") categoryId: String
    ): Response<ApiResponse<Unit>>

    // Income Sources CRUD
    @GET("api/sources")
    suspend fun getSources(
        @Header("x-session-token") sessionToken: String
    ): Response<ApiResponse<List<SourceDto>>>

    @POST("api/sources")
    suspend fun createSource(
        @Header("x-session-token") sessionToken: String,
        @Body request: CreateSourceRequest
    ): Response<ApiResponse<SourceDto>>

    @PUT("api/sources/{id}")
    suspend fun updateSource(
        @Header("x-session-token") sessionToken: String,
        @Path("id") sourceId: String,
        @Body request: UpdateSourceRequest
    ): Response<ApiResponse<SourceDto>>

    @DELETE("api/sources/{id}")
    suspend fun deleteSource(
        @Header("x-session-token") sessionToken: String,
        @Path("id") sourceId: String
    ): Response<ApiResponse<Unit>>

    // Recipients CRUD
    @GET("api/recipients")
    suspend fun getRecipients(
        @Header("x-session-token") sessionToken: String
    ): Response<ApiResponse<List<RecipientDto>>>

    @POST("api/recipients")
    suspend fun createRecipient(
        @Header("x-session-token") sessionToken: String,
        @Body request: CreateRecipientRequest
    ): Response<ApiResponse<RecipientDto>>

    @PUT("api/recipients/{id}")
    suspend fun updateRecipient(
        @Header("x-session-token") sessionToken: String,
        @Path("id") recipientId: String,
        @Body request: UpdateRecipientRequest
    ): Response<ApiResponse<RecipientDto>>

    @DELETE("api/recipients/{id}")
    suspend fun deleteRecipient(
        @Header("x-session-token") sessionToken: String,
        @Path("id") recipientId: String
    ): Response<ApiResponse<Unit>>

    @GET("api/recipients/lookup")
    suspend fun lookupRecipientByPaymentIdentifier(
        @Header("x-session-token") sessionToken: String,
        @Query("payment_identifier") paymentIdentifier: String
    ): Response<ApiResponse<RecipientDto?>>
}