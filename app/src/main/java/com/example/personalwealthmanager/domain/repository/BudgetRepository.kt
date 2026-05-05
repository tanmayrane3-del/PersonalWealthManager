package com.pwm.personalwealthmanager.domain.repository

import com.pwm.personalwealthmanager.data.remote.dto.LiabilityCategoryResponseDto
import com.pwm.personalwealthmanager.data.remote.dto.MonthlyPlanDto
import com.pwm.personalwealthmanager.data.remote.dto.ReportsDto
import com.pwm.personalwealthmanager.data.remote.dto.WizardPrefillDto
import com.pwm.personalwealthmanager.data.remote.dto.WizardSaveRequest

interface BudgetRepository {

    /** Current month as YYYY-MM-01, computed in Asia/Kolkata. */
    fun currentMonth(): String

    /** Days already elapsed in the current IST month (min 1). */
    fun daysElapsed(): Int

    /** Days remaining in the current IST month. */
    fun daysRemaining(): Int

    suspend fun getWizardPrefill(
        sessionToken: String,
        month: String
    ): Result<WizardPrefillDto>

    suspend fun saveWizard(
        sessionToken: String,
        request: WizardSaveRequest
    ): Result<MonthlyPlanDto>

    suspend fun getDashboard(
        sessionToken: String,
        month: String
    ): Result<MonthlyPlanDto>

    suspend fun getReports(
        sessionToken: String,
        months: Int = 12
    ): Result<ReportsDto>

    suspend fun setLiabilityCategory(
        sessionToken: String,
        liabilityId: String,
        budgetCategoryId: String?
    ): Result<LiabilityCategoryResponseDto>
}
