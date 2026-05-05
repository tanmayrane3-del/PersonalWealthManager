package com.pwm.personalwealthmanager.presentation.budget.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pwm.personalwealthmanager.core.utils.IndianCurrencyFormatter
import com.pwm.personalwealthmanager.data.remote.dto.LiabilityForMappingDto
import com.pwm.personalwealthmanager.data.remote.dto.SpendingType
import com.pwm.personalwealthmanager.presentation.budget.BucketCategoryUi
import com.pwm.personalwealthmanager.presentation.budget.WizardUiState

private val TealGreen = Color(0xFF1A7C5C)

@Composable
fun AllocateScreen(
    state: WizardUiState,
    onAmountChange: (categoryId: String, amount: Double) -> Unit,
    onTotalCapChange: (Double) -> Unit,
    onMapLiability: (liabilityId: String, categoryId: String?) -> Unit,
    onContinue: () -> Unit
) {
    val bucketTypes = listOf(
        SpendingType.NEED to "Needs",
        SpendingType.WANT to "Wants",
        SpendingType.SAVINGS_INVESTMENT to "Savings & Investments"
    )

    Scaffold(
        containerColor = Color(0xFFF5F7FA),
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                TotalCapInput(state = state, onTotalCapChange = onTotalCapChange)
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onContinue,
                    enabled = state.isCapValid,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TealGreen)
                ) {
                    Text("Review plan", modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Allocate your budget",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Set a monthly amount for each category. We've pre-filled what we know.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7280)
                )
                Spacer(Modifier.height(12.dp))
            }

            for ((type, label) in bucketTypes) {
                item {
                    BucketSection(
                        label = label,
                        type = type,
                        state = state,
                        onAmountChange = onAmountChange
                    )
                }
            }

            // Unmapped liabilities card
            if (state.unmappedLiabilities.isNotEmpty()) {
                item {
                    UnmappedLiabilitiesCard(
                        liabilities = state.unmappedLiabilities,
                        allCategories = state.bucketAllocations.values.flatten(),
                        onMap = onMapLiability
                    )
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun BucketSection(
    label: String,
    type: SpendingType,
    state: WizardUiState,
    onAmountChange: (String, Double) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    val categories = state.bucketAllocations[type] ?: emptyList()
    val cap = state.bucketCap(type)
    val sum = state.bucketSum(type)
    val isOver = state.bucketIsOver(type)
    val pctUsed = if (cap > 0) (sum / cap).coerceIn(0.0, 1.0) else 0.0

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Section header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Cap: ${IndianCurrencyFormatter.format(cap)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF9CA3AF)
                    )
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }

            // Mini progress bar
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { pctUsed.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = if (isOver) Color(0xFFEF4444) else TealGreen,
                trackColor = Color(0xFFE5E7EB)
            )

            // Soft warning if bucket over cap
            AnimatedVisibility(visible = isOver) {
                val overBy = sum - cap
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    color = Color(0xFFFFFBEB),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "You're ${IndianCurrencyFormatter.format(overBy)} over your $label framework. " +
                                "That's okay — your savings will absorb the difference. " +
                                "We'll show this in your monthly report.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF92400E),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // Category rows
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    for (cat in categories) {
                        CategoryRow(cat = cat, onAmountChange = { onAmountChange(cat.categoryId, it) })
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    cat: BucketCategoryUi,
    onAmountChange: (Double) -> Unit
) {
    var text by remember(cat.categoryId) {
        mutableStateOf(if (cat.amount > 0) cat.amount.toLong().toString() else "")
    }

    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = cat.name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Medium
            )
            OutlinedTextField(
                value = text,
                onValueChange = { raw ->
                    val cleaned = raw.filter { it.isDigit() }
                    text = cleaned
                    onAmountChange(cleaned.toDoubleOrNull() ?: 0.0)
                },
                prefix = { Text("₹") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(130.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TealGreen)
            )
        }
        cat.prefillCaption()?.let { caption ->
            Text(
                text = caption,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun TotalCapInput(state: WizardUiState, onTotalCapChange: (Double) -> Unit) {
    var text by remember { mutableStateOf(
        if (state.totalExpenseBudget > 0) state.totalExpenseBudget.toLong().toString() else ""
    ) }
    val sum = state.sumOfCategoryBudgets
    val isError = state.totalExpenseBudget > 0 && state.totalExpenseBudget <= sum

    Column {
        OutlinedTextField(
            value = text,
            onValueChange = { raw ->
                val cleaned = raw.filter { it.isDigit() }
                text = cleaned
                onTotalCapChange(cleaned.toDoubleOrNull() ?: 0.0)
            },
            label = { Text("Total monthly budget cap") },
            prefix = { Text("₹") },
            singleLine = true,
            isError = isError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TealGreen,
                errorBorderColor = Color(0xFFEF4444)
            )
        )
        if (isError) {
            Text(
                text = "Cap must be more than ${IndianCurrencyFormatter.format(sum)} (sum of categories)",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFEF4444)
            )
        } else if (state.totalExpenseBudget > sum) {
            Text(
                text = "Categories: ${IndianCurrencyFormatter.format(sum)}  ·  Buffer: ${IndianCurrencyFormatter.format(state.unbudgetedBuffer)}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF6B7280)
            )
        }
    }
}

@Composable
private fun UnmappedLiabilitiesCard(
    liabilities: List<LiabilityForMappingDto>,
    allCategories: List<BucketCategoryUi>,
    onMap: (String, String?) -> Unit
) {
    var showPickerFor by remember { mutableStateOf<String?>(null) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Active loans — map to a category",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF92400E)
            )
            Text(
                "Loan EMIs will be pre-filled once mapped.",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFB45309),
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )
            for (liability in liabilities) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(liability.lenderName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            "${liability.loanType.replaceFirstChar { it.uppercase() }} · EMI ${IndianCurrencyFormatter.format(liability.emiAmount)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF6B7280)
                        )
                    }
                    OutlinedButton(
                        onClick = { showPickerFor = liability.id },
                        colors = ButtonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF92400E),
                            disabledContainerColor = Color.White,
                            disabledContentColor = Color(0xFF9CA3AF)
                        )
                    ) {
                        Text("Map to category")
                    }
                }
            }
        }
    }

    // Category picker dialog
    if (showPickerFor != null) {
        CategoryPickerDialog(
            categories = allCategories,
            onSelect = { catId ->
                onMap(showPickerFor!!, catId)
                showPickerFor = null
            },
            onDismiss = { showPickerFor = null }
        )
    }
}

@Composable
private fun CategoryPickerDialog(
    categories: List<BucketCategoryUi>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select a budget category") },
        text = {
            Column {
                categories.forEach { cat ->
                    TextButton(
                        onClick = { onSelect(cat.categoryId) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "${cat.name} (${bucketLabel(cat.spendingType)})",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun bucketLabel(type: SpendingType) = when (type) {
    SpendingType.NEED -> "Need"
    SpendingType.WANT -> "Want"
    SpendingType.SAVINGS_INVESTMENT -> "Savings"
}
