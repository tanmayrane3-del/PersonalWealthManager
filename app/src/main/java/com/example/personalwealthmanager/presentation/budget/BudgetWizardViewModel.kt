package com.pwm.personalwealthmanager.presentation.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pwm.personalwealthmanager.core.utils.SessionManager
import com.pwm.personalwealthmanager.data.remote.dto.BudgetItem
import com.pwm.personalwealthmanager.data.remote.dto.Framework
import com.pwm.personalwealthmanager.data.remote.dto.IncomeTargetItem
import com.pwm.personalwealthmanager.data.remote.dto.SpendingType
import com.pwm.personalwealthmanager.data.remote.dto.WizardSaveRequest
import com.pwm.personalwealthmanager.domain.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BudgetWizardViewModel @Inject constructor(
    private val repository: BudgetRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(WizardUiState())
    val state: StateFlow<WizardUiState> = _state.asStateFlow()

    fun loadPrefill(month: String) {
        val token = sessionManager.getSessionToken() ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadError = null, month = month) }
            repository.getWizardPrefill(token, month).fold(
                onSuccess = { prefill ->
                    val incomeEntries = prefill.incomeCategories
                        .sortedWith(compareBy({ incomeTypeSortKey(it.incomeType?.name) }, { it.name }))
                        .map { cat ->
                            IncomeEntryUi(
                                categoryId = cat.id,
                                name = cat.name,
                                incomeTypeLabel = cat.incomeType?.name?.lowercase() ?: "other",
                                amount = cat.suggestedAmount,
                                source = cat.source
                            )
                        }

                    val bucketAllocations = buildMap {
                        put(SpendingType.NEED, prefill.expenseCategoriesByBucket.need.map { cat ->
                            BucketCategoryUi(
                                categoryId = cat.id,
                                name = cat.name,
                                spendingType = SpendingType.NEED,
                                amount = cat.existingPlanned ?: (cat.recurringPrefill + cat.emiPrefill),
                                recurringPrefill = cat.recurringPrefill,
                                emiPrefill = cat.emiPrefill
                            )
                        })
                        put(SpendingType.WANT, prefill.expenseCategoriesByBucket.want.map { cat ->
                            BucketCategoryUi(
                                categoryId = cat.id,
                                name = cat.name,
                                spendingType = SpendingType.WANT,
                                amount = cat.existingPlanned ?: (cat.recurringPrefill + cat.emiPrefill),
                                recurringPrefill = cat.recurringPrefill,
                                emiPrefill = cat.emiPrefill
                            )
                        })
                        put(SpendingType.SAVINGS_INVESTMENT, prefill.expenseCategoriesByBucket.savingsInvestment.map { cat ->
                            BucketCategoryUi(
                                categoryId = cat.id,
                                name = cat.name,
                                spendingType = SpendingType.SAVINGS_INVESTMENT,
                                amount = cat.existingPlanned ?: (cat.recurringPrefill + cat.emiPrefill),
                                recurringPrefill = cat.recurringPrefill,
                                emiPrefill = cat.emiPrefill
                            )
                        })
                    }

                    val prevFramework = prefill.previousMonthPlan?.framework
                    val prevNeeds = 50.0
                    val prevWants = 30.0
                    val prevSavings = 20.0

                    _state.update {
                        it.copy(
                            isLoading = false,
                            isFirstTimeUser = prefill.isFirstTimeUser,
                            incomeEntries = incomeEntries,
                            bucketAllocations = bucketAllocations,
                            allLiabilities = prefill.activeLiabilities,
                            unmappedLiabilities = prefill.activeLiabilities.filter { l -> l.mappedCategoryId == null },
                            framework = prevFramework,
                            needsPct = prevNeeds,
                            wantsPct = prevWants,
                            savingsPct = prevSavings
                        )
                    }
                },
                onFailure = { e ->
                    _state.update { it.copy(isLoading = false, loadError = e.message ?: "Failed to load") }
                }
            )
        }
    }

    fun updateIncome(categoryId: String, amount: Double) {
        _state.update { s ->
            s.copy(incomeEntries = s.incomeEntries.map {
                if (it.categoryId == categoryId) it.copy(amount = amount) else it
            })
        }
    }

    fun selectFramework(framework: Framework) {
        val newNeeds = when (framework) {
            Framework.FIFTY_30_20 -> 50.0
            Framework.FORTY_30_30 -> 40.0
            Framework.CUSTOM -> _state.value.needsPct
        }
        val newWants = when (framework) {
            Framework.FIFTY_30_20 -> 30.0
            Framework.FORTY_30_30 -> 30.0
            Framework.CUSTOM -> _state.value.wantsPct
        }
        val newSavings = when (framework) {
            Framework.FIFTY_30_20 -> 20.0
            Framework.FORTY_30_30 -> 30.0
            Framework.CUSTOM -> _state.value.savingsPct
        }
        _state.update {
            it.copy(framework = framework, needsPct = newNeeds, wantsPct = newWants, savingsPct = newSavings)
        }
        if (framework != Framework.CUSTOM) applyEqualSplitToAllocations()
    }

    fun updateCustomPct(needsPct: Double? = null, wantsPct: Double? = null, savingsPct: Double? = null) {
        _state.update { s ->
            s.copy(
                needsPct = needsPct ?: s.needsPct,
                wantsPct = wantsPct ?: s.wantsPct,
                savingsPct = savingsPct ?: s.savingsPct
            )
        }
    }

    fun applyEqualSplitToAllocations() {
        val s = _state.value
        if (s.totalIncomeTarget <= 0) return
        val updated = s.bucketAllocations.mapValues { (type, cats) ->
            val cap = s.bucketCap(type)
            val rulePrefillSum = cats.filter { it.hasPrefill }.sumOf { it.recurringPrefill + it.emiPrefill }
            val remainingCats = cats.filter { !it.hasPrefill }
            val equalShare = if (remainingCats.isNotEmpty()) {
                ((cap - rulePrefillSum) / remainingCats.size).coerceAtLeast(0.0)
            } else 0.0
            cats.map { cat ->
                if (cat.hasPrefill) {
                    cat.copy(amount = cat.recurringPrefill + cat.emiPrefill)
                } else {
                    cat.copy(amount = equalShare)
                }
            }
        }
        val totalBudgeted = updated.values.flatten().sumOf { it.amount }
        val defaultCap = (totalBudgeted * 1.05).coerceAtLeast(totalBudgeted + 1.0)
        _state.update { it.copy(bucketAllocations = updated, totalExpenseBudget = defaultCap) }
    }

    fun updateCategoryAmount(categoryId: String, amount: Double) {
        _state.update { s ->
            s.copy(bucketAllocations = s.bucketAllocations.mapValues { (_, cats) ->
                cats.map { if (it.categoryId == categoryId) it.copy(amount = amount) else it }
            })
        }
    }

    fun updateTotalCap(amount: Double) {
        _state.update { it.copy(totalExpenseBudget = amount) }
    }

    fun mapLiability(liabilityId: String, categoryId: String?) {
        val token = sessionManager.getSessionToken() ?: return
        viewModelScope.launch {
            repository.setLiabilityCategory(token, liabilityId, categoryId).fold(
                onSuccess = { updated ->
                    _state.update { s ->
                        val updatedLiabilities = s.allLiabilities.map { l ->
                            if (l.id == liabilityId) l.copy(mappedCategoryId = updated.budgetCategoryId) else l
                        }
                        s.copy(
                            allLiabilities = updatedLiabilities,
                            unmappedLiabilities = updatedLiabilities.filter { it.mappedCategoryId == null }
                        )
                    }
                    // Reload prefill to pick up new EMI mapping
                    loadPrefill(_state.value.month)
                },
                onFailure = { /* silently ignore — user can retry */ }
            )
        }
    }

    fun save(onSuccess: () -> Unit) {
        val s = _state.value
        val token = sessionManager.getSessionToken() ?: return
        val fw = s.framework ?: return

        val budgetItems = s.bucketAllocations.values.flatten()
            .filter { it.amount > 0 }
            .map { BudgetItem(categoryId = it.categoryId, plannedAmount = it.amount) }

        val incomeItems = s.incomeEntries
            .filter { it.amount > 0 }
            .map { IncomeTargetItem(categoryId = it.categoryId, targetAmount = it.amount) }

        val request = WizardSaveRequest(
            month = s.month,
            framework = fw.toApiValue(),
            needsPct = s.needsPct,
            wantsPct = s.wantsPct,
            savingsPct = s.savingsPct,
            totalIncomeTarget = s.totalIncomeTarget,
            totalExpenseBudget = s.totalExpenseBudget,
            incomeTargets = incomeItems,
            budgets = budgetItems
        )

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, saveError = null) }
            repository.saveWizard(token, request).fold(
                onSuccess = {
                    _state.update { it.copy(isSaving = false, saveSuccess = true) }
                    onSuccess()
                },
                onFailure = { e ->
                    _state.update { it.copy(isSaving = false, saveError = e.message ?: "Save failed") }
                }
            )
        }
    }

    fun clearSaveError() {
        _state.update { it.copy(saveError = null) }
    }

    private fun incomeTypeSortKey(typeName: String?): Int = when (typeName?.lowercase()) {
        "primary" -> 0
        "passive" -> 1
        "one_time" -> 2
        else -> 3
    }
}
