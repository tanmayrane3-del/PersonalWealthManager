package com.pwm.personalwealthmanager.presentation.budget

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pwm.personalwealthmanager.data.remote.dto.Framework
import com.pwm.personalwealthmanager.presentation.budget.screens.AllocateScreen
import com.pwm.personalwealthmanager.presentation.budget.screens.FrameworkScreen
import com.pwm.personalwealthmanager.presentation.budget.screens.IncomeScreen
import com.pwm.personalwealthmanager.presentation.budget.screens.ReviewScreen
import dagger.hilt.android.AndroidEntryPoint

private const val ROUTE_INCOME    = "wizard/income"
private const val ROUTE_FRAMEWORK = "wizard/framework"
private const val ROUTE_ALLOCATE  = "wizard/allocate"
private const val ROUTE_REVIEW    = "wizard/review"

private val ROUTES = listOf(ROUTE_INCOME, ROUTE_FRAMEWORK, ROUTE_ALLOCATE, ROUTE_REVIEW)

@AndroidEntryPoint
class BudgetWizardActivity : ComponentActivity() {

    private val viewModel: BudgetWizardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                WizardHost(
                    viewModel = viewModel,
                    onFinish = {
                        Toast.makeText(this, "Budget plan saved!", Toast.LENGTH_SHORT).show()
                        finish()
                    },
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WizardHost(
    viewModel: BudgetWizardViewModel,
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    val navController = rememberNavController()
    val state by viewModel.state.collectAsState()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route ?: ROUTE_INCOME

    val stepIndex = ROUTES.indexOf(currentRoute).coerceAtLeast(0)
    val stepProgress = (stepIndex + 1).toFloat() / ROUTES.size

    val stepTitle = when (currentRoute) {
        ROUTE_INCOME    -> "Step 1 of 4 — Income"
        ROUTE_FRAMEWORK -> "Step 2 of 4 — Framework"
        ROUTE_ALLOCATE  -> "Step 3 of 4 — Allocate"
        ROUTE_REVIEW    -> "Step 4 of 4 — Review"
        else            -> "Budget Wizard"
    }

    // Load prefill once
    LaunchedEffect(Unit) {
        val month = viewModel.state.value.month.ifEmpty {
            // use repository helper via ViewModel state init
            val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Kolkata"))
            "${cal.get(java.util.Calendar.YEAR)}-${(cal.get(java.util.Calendar.MONTH) + 1).toString().padStart(2, '0')}-01"
        }
        viewModel.loadPrefill(month)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stepTitle, style = MaterialTheme.typography.titleSmall) },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (!navController.popBackStack()) onBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White
                    )
                )
                LinearProgressIndicator(
                    progress = { stepProgress },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF1A7C5C),
                    trackColor = Color(0xFFE5E7EB)
                )
            }
        },
        containerColor = Color(0xFFF5F7FA)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                WizardNavGraph(
                    navController = navController,
                    state = state,
                    viewModel = viewModel,
                    onFinish = onFinish
                )
            }
        }
    }
}

@Composable
private fun WizardNavGraph(
    navController: NavHostController,
    state: WizardUiState,
    viewModel: BudgetWizardViewModel,
    onFinish: () -> Unit
) {
    NavHost(navController = navController, startDestination = ROUTE_INCOME) {
        composable(ROUTE_INCOME) {
            IncomeScreen(
                state = state,
                onAmountChange = viewModel::updateIncome,
                onContinue = { navController.navigate(ROUTE_FRAMEWORK) }
            )
        }
        composable(ROUTE_FRAMEWORK) {
            FrameworkScreen(
                state = state,
                onFrameworkSelected = { fw ->
                    viewModel.selectFramework(fw)
                    if (fw != Framework.CUSTOM) viewModel.applyEqualSplitToAllocations()
                },
                onCustomPctChange = { n, w, s ->
                    viewModel.updateCustomPct(n, w, s)
                },
                onContinue = {
                    if (state.framework == Framework.CUSTOM) viewModel.applyEqualSplitToAllocations()
                    navController.navigate(ROUTE_ALLOCATE)
                }
            )
        }
        composable(ROUTE_ALLOCATE) {
            AllocateScreen(
                state = state,
                onAmountChange = viewModel::updateCategoryAmount,
                onTotalCapChange = viewModel::updateTotalCap,
                onMapLiability = viewModel::mapLiability,
                onContinue = { navController.navigate(ROUTE_REVIEW) }
            )
        }
        composable(ROUTE_REVIEW) {
            ReviewScreen(
                state = state,
                onSave = { viewModel.save(onSuccess = onFinish) },
                onRetry = { viewModel.clearSaveError() }
            )
        }
    }
}
