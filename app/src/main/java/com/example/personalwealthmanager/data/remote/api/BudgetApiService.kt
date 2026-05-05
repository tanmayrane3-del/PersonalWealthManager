package com.pwm.personalwealthmanager.data.remote.api

import com.pwm.personalwealthmanager.data.remote.dto.ApiResponse
import com.pwm.personalwealthmanager.data.remote.dto.LiabilityCategoryResponseDto
import com.pwm.personalwealthmanager.data.remote.dto.MonthlyPlanDto
import com.pwm.personalwealthmanager.data.remote.dto.ReportsDto
import com.pwm.personalwealthmanager.data.remote.dto.SetLiabilityCategoryRequest
import com.pwm.personalwealthmanager.data.remote.dto.WizardPrefillDto
import com.pwm.personalwealthmanager.data.remote.dto.WizardSaveRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface BudgetApiService {

    @GET("api/budget/wizard/prefill")
    suspend fun getWizardPrefill(
        @Header("x-session-token") sessionToken: String,
        @Query("month") month: String
    ): Response<ApiResponse<WizardPrefillDto>>

    @POST("api/budget/wizard/save")
    suspend fun saveWizard(
        @Header("x-session-token") sessionToken: String,
        @Body request: WizardSaveRequest
    ): Response<ApiResponse<MonthlyPlanDto>>

    @GET("api/budget/dashboard")
    suspend fun getDashboard(
        @Header("x-session-token") sessionToken: String,
        @Query("month") month: String
    ): Response<ApiResponse<MonthlyPlanDto>>

    @GET("api/budget/reports")
    suspend fun getReports(
        @Header("x-session-token") sessionToken: String,
        @Query("months") months: Int = 12
    ): Response<ApiResponse<ReportsDto>>

    @PUT("api/liabilities/{id}/category")
    suspend fun setLiabilityCategory(
        @Header("x-session-token") sessionToken: String,
        @Path("id") liabilityId: String,
        @Body body: SetLiabilityCategoryRequest
    ): Response<ApiResponse<LiabilityCategoryResponseDto>>
}
