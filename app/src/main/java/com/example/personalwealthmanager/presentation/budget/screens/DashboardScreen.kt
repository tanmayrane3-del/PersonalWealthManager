package com.pwm.personalwealthmanager.presentation.budget.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwm.personalwealthmanager.core.utils.IndianCurrencyFormatter
import com.pwm.personalwealthmanager.data.remote.dto.BucketSummaryDto
import com.pwm.personalwealthmanager.presentation.budget.BudgetBottomNavBar
import com.pwm.personalwealthmanager.presentation.budget.BudgetNavTab
import com.pwm.personalwealthmanager.data.remote.dto.CategoryBudgetViewDto
import com.pwm.personalwealthmanager.data.remote.dto.MonthlyPlanDto
import com.pwm.personalwealthmanager.data.remote.dto.SpendingType
import com.pwm.personalwealthmanager.presentation.budget.BudgetDashboardViewModel
import com.pwm.personalwealthmanager.presentation.budget.DashboardUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val TealGreen = Color(0xFF1A7C5C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: BudgetDashboardViewModel,
    selectedMonth: String,
    availableMonths: List<String>,
    uiState: DashboardUiState,
    onMonthSelect: (String) -> Unit,
    onAddExpense: () -> Unit,
    onOpenReports: () -> Unit,
    onEditPlan: () -> Unit,
    onStartWizard: () -> Unit,
    onRetry: () -> Unit
) {
    var monthMenuExpanded by remember { mutableStateOf(false) }
    var selectedBucket by remember { mutableStateOf<BucketSummaryDto?>(null) }
    var sheetData by remember { mutableStateOf<Pair<BucketSummaryDto, List<CategoryBudgetViewDto>>?>(null) }

    val isCurrentMonth = viewModel.isCurrentMonth()

    Scaffold(
        containerColor = Color(0xFFF5F7FA),
        topBar = {
            TopAppBar(
                title = {
                    // Month switcher dropdown
                    ExposedDropdownMenuBox(
                        expanded = monthMenuExpanded,
                        onExpandedChange = { monthMenuExpanded = !monthMenuExpanded }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.menuAnchor()
                        ) {
                            Text(
                                text = formatMonthLabel(selectedMonth),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        ExposedDropdownMenu(
                            expanded = monthMenuExpanded,
                            onDismissRequest = { monthMenuExpanded = false }
                        ) {
                            availableMonths.forEach { month ->
                                DropdownMenuItem(
                                    text = { Text(formatMonthLabel(month)) },
                                    onClick = {
                                        onMonthSelect(month)
                                        monthMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Show Edit only for unlocked current-month plans
                    val canEdit = isCurrentMonth
                            && uiState is DashboardUiState.Success
                            && !uiState.data.isLocked
                    if (canEdit) {
                        TextButton(onClick = onEditPlan) {
                            Text("Edit", color = Color(0xFF1A7C5C))
                        }
                    }
                    TextButton(onClick = onOpenReports) {
                        Text("Reports", color = Color(0xFF1A7C5C))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = { BudgetBottomNavBar(activeTab = BudgetNavTab.BUDGET) },
        floatingActionButton = {
            if (isCurrentMonth && uiState is DashboardUiState.Success) {
                FloatingActionButton(
                    onClick = onAddExpense,
                    containerColor = TealGreen,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add expense")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (uiState) {
                is DashboardUiState.Loading, DashboardUiState.Idle ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = TealGreen)

                is DashboardUiState.Empty ->
                    EmptyState(month = selectedMonth, onStartWizard = onStartWizard)

                is DashboardUiState.Error ->
                    ErrorState(message = uiState.message, onRetry = onRetry)

                is DashboardUiState.Success -> {
                    val plan = uiState.data
                    DashboardContent(
                        plan = plan,
                        isCurrentMonth = isCurrentMonth,
                        onBucketTap = { bucket ->
                            val cats = plan.categoryBudgets.filter { it.spendingType == bucket.type }
                            sheetData = bucket to cats
                        }
                    )
                }
            }
        }
    }

    // Bucket detail bottom sheet
    sheetData?.let { (bucket, cats) ->
        ModalBottomSheet(
            onDismissRequest = { sheetData = null },
            containerColor = Color.White
        ) {
            BucketDetailSheet(bucket = bucket, categories = cats)
        }
    }
}

@Composable
private fun DashboardContent(
    plan: MonthlyPlanDto,
    isCurrentMonth: Boolean,
    onBucketTap: (BucketSummaryDto) -> Unit
) {
    val expensePct = if (plan.totals.expenseBudget > 0)
        (plan.totals.expenseActual / plan.totals.expenseBudget).coerceIn(0.0, 1.0)
    else 0.0

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(12.dp)) }

        // Lock badge
        if (plan.isLocked) {
            item {
                Surface(
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "🔒  This month is locked — final results",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = Color(0xFF1E40AF)
                    )
                }
            }
        }

        // Hero ring card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BudgetRingChart(
                        pct = expensePct.toFloat(),
                        isLocked = plan.isLocked
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        HeroStat("Spent", IndianCurrencyFormatter.compact(plan.totals.expenseActual))
                        HeroStat("Budget", IndianCurrencyFormatter.compact(plan.totals.expenseBudget))
                        HeroStat("Buffer left", IndianCurrencyFormatter.compact(
                            (plan.totals.expenseBudget - plan.totals.expenseActual).coerceAtLeast(0.0)
                        ))
                    }
                    if (!plan.isLocked) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${plan.daysRemaining}d remaining · pace: ${plan.projection.expensePace.replace('_', ' ')}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
            }
        }

        // Income bar
        item {
            IncomeBar(
                actual = plan.totals.incomeActual,
                target = plan.totals.incomeTarget,
                isLocked = plan.isLocked
            )
        }

        // Bucket cards
        item {
            Text("Spending buckets", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold, color = Color(0xFF374151))
        }

        items(plan.buckets) { bucket ->
            BucketCard(bucket = bucket, onClick = { onBucketTap(bucket) })
        }

        // Unbudgeted
        if (plan.totals.unbudgetedActual > 0) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA))) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Unbudgeted spend", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
                        Text(IndianCurrencyFormatter.format(plan.totals.unbudgetedActual),
                            style = MaterialTheme.typography.bodySmall, color = Color(0xFF374151))
                    }
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) } // FAB clearance
    }
}

@Composable
private fun BudgetRingChart(pct: Float, isLocked: Boolean) {
    val animatedPct by animateFloatAsState(
        targetValue = pct,
        animationSpec = tween(durationMillis = 800),
        label = "ring"
    )
    val ringColor = when {
        pct < 0.70f -> TealGreen
        pct < 0.90f -> Color(0xFFD97706)
        pct < 1.00f -> Color(0xFFF59E0B)
        else        -> Color(0xFFEF4444)
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 18.dp.toPx()
            val inset = stroke / 2
            drawArc(
                color = Color(0xFFE5E7EB),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedPct,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(pct * 100).toInt()}%",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = ringColor
            )
            Text(
                text = if (isLocked) "final" else "of budget",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF9CA3AF)
            )
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
    }
}

@Composable
private fun IncomeBar(actual: Double, target: Double, isLocked: Boolean) {
    val pct = if (target > 0) (actual / target).coerceIn(0.0, 1.0) else 0.0
    Card(colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Income received", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    "${IndianCurrencyFormatter.compact(actual)} / ${IndianCurrencyFormatter.compact(target)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280)
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { pct.toFloat() },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = Color(0xFF10B981),
                trackColor = Color(0xFFE5E7EB)
            )
        }
    }
}

@Composable
private fun BucketCard(bucket: BucketSummaryDto, onClick: () -> Unit) {
    val pct = (bucket.pctUsed / 100.0).toFloat()
    val color = bucketColor(bucket.type)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
                    Spacer(Modifier.width(8.dp))
                    Text(bucketLabel(bucket.type), style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium)
                }
                Text(
                    "${bucket.starsProjection ?: "—"}⭐",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { pct.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(5.dp),
                color = if (pct > 1f) Color(0xFFEF4444) else color,
                trackColor = Color(0xFFE5E7EB)
            )
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(IndianCurrencyFormatter.compact(bucket.actual),
                    style = MaterialTheme.typography.labelSmall, color = color)
                Text("of ${IndianCurrencyFormatter.compact(bucket.planned)}",
                    style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
            }
        }
    }
}

@Composable
fun BucketDetailSheet(bucket: BucketSummaryDto, categories: List<CategoryBudgetViewDto>) {
    Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
        Text(bucketLabel(bucket.type), style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold)
        Text("${IndianCurrencyFormatter.compact(bucket.actual)} of ${IndianCurrencyFormatter.compact(bucket.planned)}",
            style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
        Spacer(Modifier.height(16.dp))
        categories.forEach { cat ->
            val pct = if (cat.planned > 0) (cat.actual / cat.planned).coerceIn(0.0, 1.0) else 0.0
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(cat.categoryName, style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f))
                    Text("${IndianCurrencyFormatter.compact(cat.actual)} / ${IndianCurrencyFormatter.compact(cat.planned)}",
                        style = MaterialTheme.typography.labelSmall, color = Color(0xFF6B7280))
                }
                Spacer(Modifier.height(3.dp))
                LinearProgressIndicator(
                    progress = { pct.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = if (pct >= 1.0) Color(0xFFEF4444) else bucketColor(bucket.type),
                    trackColor = Color(0xFFE5E7EB)
                )
            }
        }
    }
}

@Composable
private fun EmptyState(month: String, onStartWizard: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text("📋", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "No plan yet for ${formatMonthLabel(month)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Set one up in 4 quick steps.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onStartWizard,
                colors = ButtonDefaults.buttonColors(containerColor = TealGreen)
            ) {
                Text("Start setup")
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text("Something went wrong", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280),
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onRetry) { Text("Retry") }
        }
    }
}

private fun formatMonthLabel(yearMonth: String): String = try {
    val d = LocalDate.parse(yearMonth, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    d.format(DateTimeFormatter.ofPattern("MMM yyyy"))
} catch (_: Exception) { yearMonth }

private fun bucketLabel(type: SpendingType?): String = when (type) {
    SpendingType.NEED               -> "Needs"
    SpendingType.WANT               -> "Wants"
    SpendingType.SAVINGS_INVESTMENT -> "Savings & Investments"
    null                            -> "Other"
}

private fun bucketColor(type: SpendingType?): Color = when (type) {
    SpendingType.NEED               -> Color(0xFF1A7C5C)
    SpendingType.WANT               -> Color(0xFF3B82F6)
    SpendingType.SAVINGS_INVESTMENT -> Color(0xFFF59E0B)
    null                            -> Color(0xFF9CA3AF)
}
