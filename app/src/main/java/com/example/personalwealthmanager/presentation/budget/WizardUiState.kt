package com.pwm.personalwealthmanager.presentation.budget

import com.pwm.personalwealthmanager.data.remote.dto.Framework
import com.pwm.personalwealthmanager.data.remote.dto.IncomeCategoryPrefillDto
import com.pwm.personalwealthmanager.data.remote.dto.LiabilityForMappingDto
import com.pwm.personalwealthmanager.data.remote.dto.SpendingType

data class IncomeEntryUi(
    val categoryId: String,
    val name: String,
    val incomeTypeLabel: String,
    val amount: Double,
    val source: String
)

data class BucketCategoryUi(
    val categoryId: String,
    val name: String,
    val spendingType: SpendingType,
    val amount: Double,
    val recurringPrefill: Double,
    val emiPrefill: Double,
    val hasPrefill: Boolean = recurringPrefill > 0 || emiPrefill > 0
) {
    fun prefillCaption(): String? = when {
        emiPrefill > 0 && recurringPrefill > 0 -> "Auto-filled from recurring expenses & active loans"
        emiPrefill > 0 -> "Auto-filled from active loans"
        recurringPrefill > 0 -> "Auto-filled from recurring expenses"
        else -> null
    }
}

data class WizardUiState(
    val month: String = "",
    val isFirstTimeUser: Boolean = false,
    val isMidMonth: Boolean = false,
    val isLoading: Boolean = false,
    val loadError: String? = null,
    val incomeEntries: List<IncomeEntryUi> = emptyList(),
    val framework: Framework? = null,
    val needsPct: Double = 50.0,
    val wantsPct: Double = 30.0,
    val savingsPct: Double = 20.0,
    val bucketAllocations: Map<SpendingType, List<BucketCategoryUi>> = emptyMap(),
    val totalExpenseBudget: Double = 0.0,
    val unmappedLiabilities: List<LiabilityForMappingDto> = emptyList(),
    val allLiabilities: List<LiabilityForMappingDto> = emptyList(),
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val saveSuccess: Boolean = false
) {
    val totalIncomeTarget: Double get() = incomeEntries.sumOf { it.amount }

    val sumOfCategoryBudgets: Double get() =
        bucketAllocations.values.flatten().sumOf { it.amount }

    val unbudgetedBuffer: Double get() =
        (totalExpenseBudget - sumOfCategoryBudgets).coerceAtLeast(0.0)

    val isCapValid: Boolean get() = totalExpenseBudget > sumOfCategoryBudgets

    fun bucketCap(type: SpendingType): Double = when (type) {
        SpendingType.NEED -> totalIncomeTarget * (needsPct / 100)
        SpendingType.WANT -> totalIncomeTarget * (wantsPct / 100)
        SpendingType.SAVINGS_INVESTMENT -> totalIncomeTarget * (savingsPct / 100)
    }

    fun bucketSum(type: SpendingType): Double =
        bucketAllocations[type].orEmpty().sumOf { it.amount }

    fun bucketIsOver(type: SpendingType): Boolean =
        totalIncomeTarget > 0 && bucketSum(type) > bucketCap(type)

    val customPctSumValid: Boolean get() =
        Math.abs(needsPct + wantsPct + savingsPct - 100.0) < 0.01

    val canAdvanceFromIncome: Boolean get() {
        if (totalIncomeTarget <= 0) return false
        if (isFirstTimeUser) {
            return incomeEntries
                .filter { it.incomeTypeLabel == "primary" }
                .any { it.amount > 0 }
        }
        return true
    }
}
