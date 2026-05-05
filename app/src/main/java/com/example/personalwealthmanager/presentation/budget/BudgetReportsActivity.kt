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
import com.pwm.personalwealthmanager.presentation.budget.screens.ReportsScreen

@AndroidEntryPoint
class BudgetReportsActivity : ComponentActivity() {

    private val viewModel: BudgetReportsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val uiState by viewModel.state.collectAsState()

                ReportsScreen(
                    uiState = uiState,
                    onCalendarMonthTap = { month ->
                        // Navigate to dashboard for the tapped month
                        val intent = Intent(this, BudgetDashboardActivity::class.java)
                            .putExtra("month", month)
                        startActivity(intent)
                    },
                    onRetry = viewModel::load
                )
            }
        }
    }
}
