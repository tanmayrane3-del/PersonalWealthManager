package com.pwm.personalwealthmanager.data.repository

import com.pwm.personalwealthmanager.data.remote.api.BudgetApiService
import com.pwm.personalwealthmanager.data.remote.dto.LiabilityCategoryResponseDto
import com.pwm.personalwealthmanager.data.remote.dto.MonthlyPlanDto
import com.pwm.personalwealthmanager.data.remote.dto.ReportsDto
import com.pwm.personalwealthmanager.data.remote.dto.SetLiabilityCategoryRequest
import com.pwm.personalwealthmanager.data.remote.dto.WizardPrefillDto
import com.pwm.personalwealthmanager.data.remote.dto.WizardSaveRequest
import com.pwm.personalwealthmanager.domain.repository.BudgetRepository
import org.json.JSONObject
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

class BudgetRepositoryImpl @Inject constructor(
    private val api: BudgetApiService
) : BudgetRepository {

    // ── IST date helpers ──────────────────────────────────────────────────────

    private fun istCalendar(): Calendar =
        Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))

    override fun currentMonth(): String {
        val cal = istCalendar()
        val year  = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        return "$year-${month.toString().padStart(2, '0')}-01"
    }

    override fun daysElapsed(): Int {
        val cal = istCalendar()
        return maxOf(1, cal.get(Calendar.DAY_OF_MONTH))
    }

    override fun daysRemaining(): Int {
        val cal = istCalendar()
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH) - cal.get(Calendar.DAY_OF_MONTH)
    }

    // ── API calls ─────────────────────────────────────────────────────────────

    override suspend fun getWizardPrefill(
        sessionToken: String,
        month: String
    ): Result<WizardPrefillDto> = safeCall("fetch wizard prefill") {
        val response = api.getWizardPrefill(sessionToken, month)
        if (response.isSuccessful && response.body()?.status == "success") {
            response.body()?.data ?: throw Exception("No prefill data returned")
        } else {
            throw Exception(parseReason(response.errorBody()?.string(), "Failed to fetch prefill"))
        }
    }

    override suspend fun saveWizard(
        sessionToken: String,
        request: WizardSaveRequest
    ): Result<MonthlyPlanDto> = safeCall("save wizard plan") {
        val response = api.saveWizard(sessionToken, request)
        if (response.isSuccessful && response.body()?.status == "success") {
            response.body()?.data ?: throw Exception("No dashboard data returned")
        } else {
            val raw = response.errorBody()?.string()
            val reason = parseReason(raw, "Failed to save plan")
            // Surface HTTP 409 locked-month error distinctly
            if (response.code() == 409) throw LockedMonthException(reason)
            throw Exception(reason)
        }
    }

    override suspend fun getDashboard(
        sessionToken: String,
        month: String
    ): Result<MonthlyPlanDto> = safeCall("fetch dashboard") {
        val response = api.getDashboard(sessionToken, month)
        if (response.isSuccessful && response.body()?.status == "success") {
            response.body()?.data ?: throw Exception("No dashboard data returned")
        } else {
            throw Exception(parseReason(response.errorBody()?.string(), "Failed to fetch dashboard"))
        }
    }

    override suspend fun getReports(
        sessionToken: String,
        months: Int
    ): Result<ReportsDto> = safeCall("fetch reports") {
        val response = api.getReports(sessionToken, months)
        if (response.isSuccessful && response.body()?.status == "success") {
            response.body()?.data ?: throw Exception("No reports data returned")
        } else {
            throw Exception(parseReason(response.errorBody()?.string(), "Failed to fetch reports"))
        }
    }

    override suspend fun setLiabilityCategory(
        sessionToken: String,
        liabilityId: String,
        budgetCategoryId: String?
    ): Result<LiabilityCategoryResponseDto> = safeCall("set liability category") {
        val response = api.setLiabilityCategory(
            sessionToken,
            liabilityId,
            SetLiabilityCategoryRequest(budgetCategoryId)
        )
        if (response.isSuccessful && response.body()?.status == "success") {
            response.body()?.data ?: throw Exception("No data returned")
        } else {
            throw Exception(parseReason(response.errorBody()?.string(), "Failed to update liability category"))
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun <T> safeCall(tag: String, block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun parseReason(errorJson: String?, fallback: String): String {
        if (errorJson.isNullOrBlank()) return fallback
        return try {
            JSONObject(errorJson).optString("reason", fallback).ifBlank { fallback }
        } catch (_: Exception) {
            fallback
        }
    }
}

/** Thrown when the backend rejects a save because the month is already locked. */
class LockedMonthException(message: String) : Exception(message)
