package com.pwm.personalwealthmanager.presentation.budget.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwm.personalwealthmanager.core.utils.IndianCurrencyFormatter
import com.pwm.personalwealthmanager.data.remote.dto.Framework
import com.pwm.personalwealthmanager.data.remote.dto.SpendingType
import com.pwm.personalwealthmanager.presentation.budget.WizardUiState

private val TealGreen = Color(0xFF1A7C5C)

@Composable
fun ReviewScreen(
    state: WizardUiState,
    onSave: () -> Unit,
    onRetry: () -> Unit
) {
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = Color(0xFFF5F7FA),
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (state.saveError != null) {
                    Surface(
                        color = Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                state.saveError,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF991B1B),
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onRetry) {
                                Text("Retry", color = Color(0xFF991B1B))
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Button(
                    onClick = onSave,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TealGreen)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Save plan", modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Review your plan",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                state.month,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6B7280)
            )

            // Hero numbers card
            Card(
                colors = CardDefaults.cardColors(containerColor = TealGreen),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        HeroStat("Income target", IndianCurrencyFormatter.compact(state.totalIncomeTarget), Color.White)
                        HeroStat("Budget cap", IndianCurrencyFormatter.compact(state.totalExpenseBudget), Color.White)
                        HeroStat("Buffer", IndianCurrencyFormatter.compact(state.unbudgetedBuffer), Color(0xFFD1EBE7))
                    }
                }
            }

            // Stacked bar for buckets
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Budget breakdown", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    ReviewStackedBar(state)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BucketSummaryChip("Needs", state.bucketSum(SpendingType.NEED), Color(0xFF1A7C5C))
                        BucketSummaryChip("Wants", state.bucketSum(SpendingType.WANT), Color(0xFF3B82F6))
                        BucketSummaryChip("Savings", state.bucketSum(SpendingType.SAVINGS_INVESTMENT), Color(0xFFF59E0B))
                        BucketSummaryChip("Buffer", state.unbudgetedBuffer, Color(0xFF9CA3AF))
                    }
                }
            }

            // Framework + category count summary
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryChip(
                    label = "Framework",
                    value = frameworkLabel(state.framework),
                    modifier = Modifier.weight(1f)
                )
                val catCount = state.bucketAllocations.values.flatten().count { it.amount > 0 }
                SummaryChip(
                    label = "Categories",
                    value = "$catCount budgeted",
                    modifier = Modifier.weight(1f)
                )
            }

            // Collapsible scoring guide
            ScoringGuideCard()
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.8f))
    }
}

@Composable
private fun ReviewStackedBar(state: WizardUiState) {
    val needsSum = state.bucketSum(SpendingType.NEED)
    val wantsSum = state.bucketSum(SpendingType.WANT)
    val savingsSum = state.bucketSum(SpendingType.SAVINGS_INVESTMENT)
    val buffer = state.unbudgetedBuffer
    val total = (needsSum + wantsSum + savingsSum + buffer).coerceAtLeast(1.0)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        if (needsSum > 0) Box(Modifier.fillMaxHeight().weight((needsSum / total).toFloat()).background(Color(0xFF1A7C5C)))
        if (wantsSum > 0) Box(Modifier.fillMaxHeight().weight((wantsSum / total).toFloat()).background(Color(0xFF3B82F6)))
        if (savingsSum > 0) Box(Modifier.fillMaxHeight().weight((savingsSum / total).toFloat()).background(Color(0xFFF59E0B)))
        if (buffer > 0) Box(Modifier.fillMaxHeight().weight((buffer / total).toFloat()).background(Color(0xFFE5E7EB)))
    }
}

@Composable
private fun BucketSummaryChip(label: String, amount: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(IndianCurrencyFormatter.compact(amount), style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
    }
}

@Composable
private fun SummaryChip(label: String, value: String, modifier: Modifier = Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp), modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ScoringGuideCard() {
    var expanded by remember { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("How discipline scoring works", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null)
                }
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    StarRow("⭐⭐⭐⭐⭐  5 stars", "Spent ≤ 90% of budget")
                    StarRow("⭐⭐⭐⭐  4 stars",   "Spent ≤ 100% (on target)")
                    StarRow("⭐⭐⭐  3 stars",     "Spent ≤ 110%")
                    StarRow("⭐⭐  2 stars",       "Spent ≤ 125%")
                    StarRow("⭐  1 star",          "Spent > 125%")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "At month-end, the app scores each category and gives an overall rating. " +
                                "4+ stars for a month counts as 'on track'.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280)
                    )
                }
            }
        }
    }
}

@Composable
private fun StarRow(stars: String, description: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stars, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(130.dp))
        Text(description, style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
    }
}

private fun frameworkLabel(fw: Framework?) = when (fw) {
    Framework.FIFTY_30_20 -> "50/30/20"
    Framework.FORTY_30_30 -> "40/30/30"
    Framework.CUSTOM -> "Custom"
    null -> "—"
}
