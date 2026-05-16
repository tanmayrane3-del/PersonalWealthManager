package com.pwm.personalwealthmanager.presentation.budget

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import com.pwm.personalwealthmanager.presentation.budget.screens.DashboardScreen
import com.pwm.personalwealthmanager.presentation.transactions.EditTransactionActivity

@AndroidEntryPoint
class BudgetDashboardActivity : ComponentActivity() {

    private val viewModel: BudgetDashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // If launched from reports calendar tap, jump to that month
        intent.getStringExtra("month")?.let { viewModel.selectMonth(it) }
        setContent {
            MaterialTheme {
                val uiState    by viewModel.dashboardState.collectAsState()
                val month      by viewModel.selectedMonth.collectAsState()

                DashboardScreen(
                    viewModel       = viewModel,
                    selectedMonth   = month,
                    availableMonths = viewModel.availableMonths,
                    uiState         = uiState,
                    onMonthSelect   = viewModel::selectMonth,
                    onAddExpense    = {
                        startActivity(
                            Intent(this, EditTransactionActivity::class.java)
                                .putExtra("mode", "add")
                        )
                    },
                    onOpenReports   = {
                        startActivity(Intent(this, BudgetReportsActivity::class.java))
                    },
                    onEditPlan      = {
                        startActivity(Intent(this, BudgetWizardActivity::class.java))
                    },
                    onStartWizard   = {
                        startActivity(Intent(this, BudgetWizardActivity::class.java))
                    },
                    onRetry         = viewModel::refresh
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh when returning from wizard so dashboard reflects the saved plan
        viewModel.refresh()
    }
}
