package com.pwm.personalwealthmanager.presentation.budget.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwm.personalwealthmanager.core.utils.IndianCurrencyFormatter
import com.pwm.personalwealthmanager.presentation.budget.IncomeEntryUi
import com.pwm.personalwealthmanager.presentation.budget.WizardUiState

@Composable
fun IncomeScreen(
    state: WizardUiState,
    onAmountChange: (categoryId: String, amount: Double) -> Unit,
    onContinue: () -> Unit
) {
    val incomeEntries = state.incomeEntries
    val isFirstTime = state.isFirstTimeUser

    Scaffold(
        containerColor = Color(0xFFF5F7FA),
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total income",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6B7280)
                    )
                    Text(
                        text = IndianCurrencyFormatter.format(state.totalIncomeTarget),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onContinue,
                    enabled = state.canAdvanceFromIncome,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A7C5C))
                ) {
                    Text("Continue", modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "What's your income this month?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "We'll pre-fill from your last 3 months. Edit anything that changed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7280)
                )
                Spacer(Modifier.height(20.dp))
            }

            // Group by type label
            val groups = incomeEntries.groupBy { it.incomeTypeLabel }
            val orderedGroups = listOfNotNull(
                "primary".takeIf { groups.containsKey("primary") },
                "passive".takeIf { groups.containsKey("passive") },
                "one_time".takeIf { groups.containsKey("one_time") }
            ) + groups.keys.filter { it !in listOf("primary", "passive", "one_time") }

            for (groupKey in orderedGroups) {
                val entries = groups[groupKey] ?: continue
                item {
                    val label = when (groupKey) {
                        "primary" -> "Primary Income"
                        "passive" -> "Passive Income"
                        "one_time" -> "One-time Income"
                        else -> groupKey.replaceFirstChar { it.uppercase() }
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF9CA3AF),
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                items(entries, key = { it.categoryId }) { entry ->
                    IncomeCategoryRow(
                        entry = entry,
                        isFirstTime = isFirstTime && entry.incomeTypeLabel == "primary",
                        onAmountChange = { onAmountChange(entry.categoryId, it) }
                    )
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun IncomeCategoryRow(
    entry: IncomeEntryUi,
    isFirstTime: Boolean,
    onAmountChange: (Double) -> Unit
) {
    var text by remember(entry.categoryId) {
        mutableStateOf(if (entry.amount > 0) entry.amount.toLong().toString() else "")
    }
    val isEmpty = text.isBlank() || text == "0"
    val showError = isFirstTime && isEmpty

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { raw ->
                        val cleaned = raw.filter { it.isDigit() }
                        text = cleaned
                        onAmountChange(cleaned.toDoubleOrNull() ?: 0.0)
                    },
                    prefix = { Text("₹", style = MaterialTheme.typography.bodyLarge) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = showError,
                    modifier = Modifier.width(140.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1A7C5C),
                        errorBorderColor = Color(0xFFEF4444)
                    )
                )
            }
            AnimatedVisibility(visible = showError) {
                Text(
                    text = "Required — enter your ${entry.name.lowercase()} income",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFEF4444),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
