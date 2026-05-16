package com.pwm.personalwealthmanager.data.remote.dto

import com.google.gson.annotations.SerializedName

// ── Enums ─────────────────────────────────────────────────────────────────────

enum class Framework {
    @SerializedName("50_30_20") FIFTY_30_20,
    @SerializedName("40_30_30") FORTY_30_30,
    @SerializedName("custom")   CUSTOM;

    fun toApiValue(): String = when (this) {
        FIFTY_30_20 -> "50_30_20"
        FORTY_30_30 -> "40_30_30"
        CUSTOM      -> "custom"
    }
}

enum class SpendingType {
    @SerializedName("need")                 NEED,
    @SerializedName("want")                 WANT,
    @SerializedName("savings_investment")   SAVINGS_INVESTMENT
}

enum class IncomeType {
    @SerializedName("primary")  PRIMARY,
    @SerializedName("passive")  PASSIVE,
    @SerializedName("one_time") ONE_TIME
}

// ── Wizard Prefill ────────────────────────────────────────────────────────────

data class WizardPrefillDto(
    val month: String,
    @SerializedName("is_first_time_user")         val isFirstTimeUser: Boolean,
    @SerializedName("income_categories")           val incomeCategories: List<IncomeCategoryPrefillDto>,
    @SerializedName("expense_categories_by_bucket") val expenseCategoriesByBucket: ExpenseBucketMapDto,
    @SerializedName("active_liabilities")          val activeLiabilities: List<LiabilityForMappingDto>,
    @SerializedName("current_month_plan")          val currentMonthPlan: PreviousMonthPlanDto?,
    @SerializedName("previous_month_plan")         val previousMonthPlan: PreviousMonthPlanDto?
)

data class IncomeCategoryPrefillDto(
    val id: String,
    val name: String,
    @SerializedName("income_type")      val incomeType: IncomeType?,
    @SerializedName("suggested_amount") val suggestedAmount: Double,
    val source: String
)

data class ExpenseBucketMapDto(
    val need: List<ExpenseCategoryPrefillDto>,
    val want: List<ExpenseCategoryPrefillDto>,
    @SerializedName("savings_investment") val savingsInvestment: List<ExpenseCategoryPrefillDto>
)

data class ExpenseCategoryPrefillDto(
    val id: String,
    val name: String,
    @SerializedName("recurring_prefill")   val recurringPrefill: Double,
    @SerializedName("emi_prefill")         val emiPrefill: Double,
    @SerializedName("default_equal_split") val defaultEqualSplit: Double,
    @SerializedName("existing_planned")    val existingPlanned: Double?,
    val source: String?
)

data class LiabilityForMappingDto(
    val id: String,
    @SerializedName("loan_type")             val loanType: String,
    @SerializedName("lender_name")           val lenderName: String,
    @SerializedName("emi_amount")            val emiAmount: Double,
    @SerializedName("mapped_category_id")    val mappedCategoryId: String?,
    @SerializedName("mapped_category_name")  val mappedCategoryName: String?
)

data class PreviousMonthPlanDto(
    val exists: Boolean,
    val framework: Framework?,
    @SerializedName("needs_pct")             val needsPct: Double?,
    @SerializedName("wants_pct")             val wantsPct: Double?,
    @SerializedName("savings_pct")           val savingsPct: Double?,
    @SerializedName("total_income_target")   val totalIncomeTarget: Double?,
    @SerializedName("total_expense_budget")  val totalExpenseBudget: Double?
)

// ── Wizard Save ───────────────────────────────────────────────────────────────

data class WizardSaveRequest(
    val month: String,
    val framework: String,
    @SerializedName("needs_pct")            val needsPct: Double,
    @SerializedName("wants_pct")            val wantsPct: Double,
    @SerializedName("savings_pct")          val savingsPct: Double,
    @SerializedName("total_income_target")  val totalIncomeTarget: Double,
    @SerializedName("total_expense_budget") val totalExpenseBudget: Double,
    @SerializedName("income_targets")       val incomeTargets: List<IncomeTargetItem>,
    val budgets: List<BudgetItem>
)

data class IncomeTargetItem(
    @SerializedName("category_id")    val categoryId: String,
    @SerializedName("target_amount")  val targetAmount: Double
)

data class BudgetItem(
    @SerializedName("category_id")    val categoryId: String,
    @SerializedName("planned_amount") val plannedAmount: Double
)

// ── Dashboard ─────────────────────────────────────────────────────────────────

data class MonthlyPlanDto(
    val month: String,
    @SerializedName("is_locked")      val isLocked: Boolean,
    @SerializedName("days_remaining") val daysRemaining: Int,
    @SerializedName("days_elapsed")   val daysElapsed: Int,
    val framework: Framework?,
    @SerializedName("needs_pct")      val needsPct: Double,
    @SerializedName("wants_pct")      val wantsPct: Double,
    @SerializedName("savings_pct")    val savingsPct: Double,
    val totals: TotalsDto,
    val buckets: List<BucketSummaryDto>,
    @SerializedName("category_budgets") val categoryBudgets: List<CategoryBudgetViewDto>,
    @SerializedName("income_targets")   val incomeTargets: List<IncomeTargetViewDto>,
    val projection: ProjectionDto
)

data class TotalsDto(
    @SerializedName("income_target")        val incomeTarget: Double,
    @SerializedName("income_actual")        val incomeActual: Double,
    @SerializedName("expense_budget")       val expenseBudget: Double,
    @SerializedName("expense_actual")       val expenseActual: Double,
    @SerializedName("savings_target")       val savingsTarget: Double,
    @SerializedName("savings_projected")    val savingsProjected: Double,
    @SerializedName("unbudgeted_buffer")    val unbudgetedBuffer: Double,
    @SerializedName("unbudgeted_actual")    val unbudgetedActual: Double
)

data class BucketSummaryDto(
    val type: SpendingType?,
    val planned: Double,
    val actual: Double,
    @SerializedName("pct_used")          val pctUsed: Double,
    @SerializedName("stars_projection")  val starsProjection: Int?
)

data class CategoryBudgetViewDto(
    @SerializedName("category_id")       val categoryId: String,
    @SerializedName("category_name")     val categoryName: String,
    @SerializedName("spending_type")     val spendingType: SpendingType?,
    val planned: Double,
    val actual: Double,
    @SerializedName("pct_used")          val pctUsed: Double,
    @SerializedName("stars_projection")  val starsProjection: Int?,
    @SerializedName("is_locked")         val isLocked: Boolean,
    @SerializedName("stars_locked")      val starsLocked: Int?
)

data class IncomeTargetViewDto(
    @SerializedName("category_id")      val categoryId: String,
    @SerializedName("category_name")    val categoryName: String,
    @SerializedName("income_type")      val incomeType: IncomeType?,
    val target: Double,
    val actual: Double,
    @SerializedName("pct_received")     val pctReceived: Double,
    @SerializedName("stars_projection") val starsProjection: Int?
)

data class ProjectionDto(
    @SerializedName("expense_pace")                    val expensePace: String,
    @SerializedName("projected_end_of_month_expense")  val projectedEndOfMonthExpense: Double,
    @SerializedName("projected_overall_stars")         val projectedOverallStars: Int?
)

// ── Reports ───────────────────────────────────────────────────────────────────

data class ReportsDto(
    val kpi: KpiDto,
    val calendar: List<CalendarMonthDto>,
    @SerializedName("framework_adherence_trend") val frameworkAdherenceTrend: List<AdherencePointDto>,
    @SerializedName("category_streaks")          val categoryStreaks: List<CategoryStreakDto>,
    @SerializedName("savings_cumulative")        val savingsCumulative: List<SavingsCumPointDto>
)

data class KpiDto(
    @SerializedName("months_on_track")       val monthsOnTrack: Int,
    @SerializedName("total_months_with_plan") val totalMonthsWithPlan: Int,
    @SerializedName("hit_rate_pct")          val hitRatePct: Int,
    @SerializedName("current_streak_months") val currentStreakMonths: Int,
    @SerializedName("avg_stars")             val avgStars: Double
)

data class CalendarMonthDto(
    val month: String,
    @SerializedName("is_locked")                val isLocked: Boolean,
    @SerializedName("overall_stars")            val overallStars: Int?,
    val spent: Double?,
    val cap: Double?,
    @SerializedName("framework_adherence_pct")  val frameworkAdherencePct: Double?
)

data class AdherencePointDto(
    val month: String,
    @SerializedName("adherence_pct") val adherencePct: Double?
)

data class CategoryStreakDto(
    @SerializedName("category_id")           val categoryId: String,
    @SerializedName("category_name")         val categoryName: String,
    @SerializedName("active_streak_months")  val activeStreakMonths: Int,
    @SerializedName("is_active")             val isActive: Boolean
)

data class SavingsCumPointDto(
    val month: String,
    @SerializedName("planned_cum") val plannedCum: Double,
    @SerializedName("actual_cum")  val actualCum: Double
)

// ── Liability category mapping ────────────────────────────────────────────────

data class SetLiabilityCategoryRequest(
    @SerializedName("budget_category_id") val budgetCategoryId: String?
)

data class LiabilityCategoryResponseDto(
    val id: String,
    @SerializedName("loan_type")            val loanType: String,
    @SerializedName("lender_name")          val lenderName: String,
    @SerializedName("emi_amount")           val emiAmount: Double,
    @SerializedName("budget_category_id")   val budgetCategoryId: String?,
    val status: String,
    @SerializedName("updated_at")           val updatedAt: String?
)
