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
import com.pwm.personalwealthmanager.data.remote.dto.IncomeTargetViewDto
import com.pwm.personalwealthmanager.data.remote.dto.UnbudgetedCategoryDto
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
    var sheetData by remember { mutableStateOf<Pair<BucketSummaryDto, List<CategoryBudgetViewDto>>?>(null) }
    var showIncomeSheet by remember { mutableStateOf(false) }
    var showUnbudgetedSheet by remember { mutableStateOf(false) }

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
        bottomBar = { BudgetBottomNavBar(activeTab = BudgetNavTab.MENU) },
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
                        },
                        onIncomeTap    = { showIncomeSheet = true },
                        onUnbudgetedTap = { showUnbudgetedSheet = true }
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

    // Income breakdown bottom sheet
    if (showIncomeSheet && uiState is DashboardUiState.Success) {
        ModalBottomSheet(
            onDismissRequest = { showIncomeSheet = false },
            containerColor = Color.White
        ) {
            IncomeDetailSheet(incomeTargets = uiState.data.incomeTargets)
        }
    }

    // Unbudgeted spend bottom sheet
    if (showUnbudgetedSheet && uiState is DashboardUiState.Success) {
        ModalBottomSheet(
            onDismissRequest = { showUnbudgetedSheet = false },
            containerColor = Color.White
        ) {
            UnbudgetedDetailSheet(
                unbudgetedActual    = uiState.data.totals.unbudgetedActual,
                unbudgetedBuffer    = uiState.data.totals.unbudgetedBuffer,
                unbudgetedCategories = uiState.data.unbudgetedCategories
            )
        }
    }
}

@Composable
private fun DashboardContent(
    plan: MonthlyPlanDto,
    isCurrentMonth: Boolean,
    onBucketTap: (BucketSummaryDto) -> Unit,
    onIncomeTap: () -> Unit,
    onUnbudgetedTap: () -> Unit
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

        // Income bar — tappable
        item {
            IncomeBar(
                actual = plan.totals.incomeActual,
                target = plan.totals.incomeTarget,
                isLocked = plan.isLocked,
                onClick = onIncomeTap
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

        // Unbudgeted — tappable
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onUnbudgetedTap),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Unbudgeted spend", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
                    Text(
                        IndianCurrencyFormatter.format(plan.totals.unbudgetedActual),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (plan.totals.unbudgetedActual > 0) Color(0xFFEF4444) else Color(0xFF374151)
                    )
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
private fun IncomeBar(actual: Double, target: Double, isLocked: Boolean, onClick: () -> Unit) {
    val pct = if (target > 0) (actual / target).coerceIn(0.0, 1.0) else 0.0
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
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
    val color = bucketColor(bucket.type)
    val hasPlanned = bucket.planned > 0.0
    // If no plan set, treat planned=1 so actual/1=actual → bar fills to 100% when there's spend
    val effectivePct = if (hasPlanned) (bucket.pctUsed / 100.0).toFloat()
                       else if (bucket.actual > 0.0) 1f else 0f
    val pct = (bucket.pctUsed / 100.0).toFloat()
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
                progress = { effectivePct.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(5.dp),
                color = if (hasPlanned && pct > 1f) Color(0xFFEF4444) else color,
                trackColor = Color(0xFFE5E7EB)
            )
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(IndianCurrencyFormatter.compact(bucket.actual),
                    style = MaterialTheme.typography.labelSmall, color = color)
                Text(
                    if (hasPlanned) "of ${IndianCurrencyFormatter.compact(bucket.planned)}"
                    else "Unplanned",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF9CA3AF)
                )
            }
        }
    }
}

@Composable
fun BucketDetailSheet(bucket: BucketSummaryDto, categories: List<CategoryBudgetViewDto>) {
    Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
        Text(bucketLabel(bucket.type), style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold)
        Text(
            if (bucket.planned > 0)
                "${IndianCurrencyFormatter.compact(bucket.actual)} of ${IndianCurrencyFormatter.compact(bucket.planned)}"
            else
                "${IndianCurrencyFormatter.compact(bucket.actual)} spent · Unplanned",
            style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280)
        )
        Spacer(Modifier.height(16.dp))
        categories.forEach { cat ->
            val catHasPlanned = cat.planned > 0
            // If no plan, treat planned=1 so bar fills to 100% when there's any spend
            val catPct = when {
                catHasPlanned    -> (cat.actual / cat.planned).coerceIn(0.0, 1.0)
                cat.actual > 0.0 -> 1.0
                else             -> 0.0
            }
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(cat.categoryName, style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f))
                    Text(
                        if (catHasPlanned)
                            "${IndianCurrencyFormatter.compact(cat.actual)} / ${IndianCurrencyFormatter.compact(cat.planned)}"
                        else
                            "${IndianCurrencyFormatter.compact(cat.actual)} spent",
                        style = MaterialTheme.typography.labelSmall, color = Color(0xFF6B7280)
                    )
                }
                Spacer(Modifier.height(3.dp))
                LinearProgressIndicator(
                    progress = { catPct.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = if (catHasPlanned && catPct >= 1.0) Color(0xFFEF4444)
                            else bucketColor(bucket.type),
                    trackColor = Color(0xFFE5E7EB)
                )
            }
        }
    }
}

// ── Income detail bottom sheet ────────────────────────────────────────────────
@Composable
fun IncomeDetailSheet(incomeTargets: List<IncomeTargetViewDto>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            "Income breakdown",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Target vs received this month",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF6B7280)
        )
        Spacer(Modifier.height(16.dp))

        if (incomeTargets.isEmpty()) {
            Text(
                "No income targets set for this month.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF9CA3AF)
            )
        } else {
            incomeTargets.forEach { it ->
                val pct = if (it.target > 0) (it.actual / it.target).coerceIn(0.0, 1.0) else 0.0
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(it.categoryName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(
                                it.incomeType?.name?.lowercase()?.replaceFirstChar { c -> c.uppercase() } ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF9CA3AF)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                IndianCurrencyFormatter.compact(it.actual),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF10B981)
                            )
                            Text(
                                "of ${IndianCurrencyFormatter.compact(it.target)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF9CA3AF)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { pct.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(5.dp),
                        color = Color(0xFF10B981),
                        trackColor = Color(0xFFE5E7EB)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${it.pctReceived.toInt()}% received${it.starsProjection?.let { s -> " · $s⭐" } ?: ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF9CA3AF)
                    )
                }
                HorizontalDivider(color = Color(0xFFF3F4F6))
            }
        }
    }
}

// ── Unbudgeted spend detail bottom sheet ──────────────────────────────────────
@Composable
fun UnbudgetedDetailSheet(
    unbudgetedActual: Double,
    unbudgetedBuffer: Double,
    unbudgetedCategories: List<UnbudgetedCategoryDto>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            "Unbudgeted spend",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "${IndianCurrencyFormatter.compact(unbudgetedActual)} across ${unbudgetedCategories.size} categories",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF6B7280)
        )
        Spacer(Modifier.height(16.dp))

        if (unbudgetedCategories.isEmpty()) {
            Text(
                "No unbudgeted spend this month.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF9CA3AF)
            )
        } else {
            unbudgetedCategories.forEach { cat ->
                // Progress bar = proportion of this category vs total unbudgeted
                val pct = if (unbudgetedActual > 0)
                    (cat.actual / unbudgetedActual).toFloat().coerceIn(0f, 1f)
                else 0f

                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icon + name
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (!cat.icon.isNullOrBlank()) {
                                Text(cat.icon, fontSize = 16.sp)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                cat.categoryName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            IndianCurrencyFormatter.compact(cat.actual),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF374151),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    LinearProgressIndicator(
                        progress = { pct },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = Color(0xFFF97316),
                        trackColor = Color(0xFFE5E7EB)
                    )
                }
                HorizontalDivider(color = Color(0xFFF3F4F6))
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
