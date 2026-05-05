package com.pwm.personalwealthmanager.presentation.budget.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pwm.personalwealthmanager.core.utils.IndianCurrencyFormatter
import com.pwm.personalwealthmanager.data.remote.dto.Framework
import com.pwm.personalwealthmanager.presentation.budget.WizardUiState

private val TealGreen = Color(0xFF1A7C5C)
private val CardBorder = Color(0xFFE5E7EB)
private val SelectedBorder = TealGreen

@Composable
fun FrameworkScreen(
    state: WizardUiState,
    onFrameworkSelected: (Framework) -> Unit,
    onCustomPctChange: (needs: Double?, wants: Double?, savings: Double?) -> Unit,
    onContinue: () -> Unit
) {
    val income = state.totalIncomeTarget
    val canContinue = state.framework != null &&
            (state.framework != Framework.CUSTOM || state.customPctSumValid)

    Scaffold(
        containerColor = Color(0xFFF5F7FA),
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onContinue,
                    enabled = canContinue,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TealGreen)
                ) {
                    Text("Continue", modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Choose a budget framework",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "How do you want to split your ₹${IndianCurrencyFormatter.format(income)} income?",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6B7280)
            )

            FrameworkCard(
                title = "Balanced  50/30/20",
                subtitle = "Standard split — half for needs, savings last",
                framework = Framework.FIFTY_30_20,
                needsPct = 50.0, wantsPct = 30.0, savingsPct = 20.0,
                income = income,
                selected = state.framework == Framework.FIFTY_30_20,
                onSelect = { onFrameworkSelected(Framework.FIFTY_30_20) }
            )

            FrameworkCard(
                title = "Aggressive Growth  40/30/30",
                subtitle = "Save more, spend less on wants",
                framework = Framework.FORTY_30_30,
                needsPct = 40.0, wantsPct = 30.0, savingsPct = 30.0,
                income = income,
                selected = state.framework == Framework.FORTY_30_30,
                onSelect = { onFrameworkSelected(Framework.FORTY_30_30) }
            )

            CustomFrameworkCard(
                state = state,
                income = income,
                onSelect = { onFrameworkSelected(Framework.CUSTOM) },
                onCustomPctChange = onCustomPctChange
            )
        }
    }
}

@Composable
private fun FrameworkCard(
    title: String,
    subtitle: String,
    framework: Framework,
    needsPct: Double,
    wantsPct: Double,
    savingsPct: Double,
    income: Double,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val border = if (selected) BorderStroke(2.dp, SelectedBorder) else BorderStroke(1.dp, CardBorder)
    OutlinedCard(
        onClick = onSelect,
        border = border,
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected) Color(0xFFF0FBF6) else Color.White
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selected,
                    onClick = onSelect,
                    colors = RadioButtonDefaults.colors(selectedColor = TealGreen)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
                }
            }
            Spacer(Modifier.height(12.dp))
            StackedBucketBar(needsPct, wantsPct, savingsPct)
            Spacer(Modifier.height(8.dp))
            BucketAmounts(needsPct, wantsPct, savingsPct, income)
        }
    }
}

@Composable
private fun CustomFrameworkCard(
    state: WizardUiState,
    income: Double,
    onSelect: () -> Unit,
    onCustomPctChange: (needs: Double?, wants: Double?, savings: Double?) -> Unit
) {
    val selected = state.framework == Framework.CUSTOM
    val border = if (selected) BorderStroke(2.dp, SelectedBorder) else BorderStroke(1.dp, CardBorder)
    val pctSum = state.needsPct + state.wantsPct + state.savingsPct
    val sumValid = Math.abs(pctSum - 100.0) < 0.01

    OutlinedCard(
        onClick = onSelect,
        border = border,
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected) Color(0xFFF0FBF6) else Color.White
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selected,
                    onClick = onSelect,
                    colors = RadioButtonDefaults.colors(selectedColor = TealGreen)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Custom", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("Set your own percentages", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
                }
            }

            AnimatedVisibility(
                visible = selected,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    PctInputRow("Needs", state.needsPct) { onCustomPctChange(it, null, null) }
                    PctInputRow("Wants", state.wantsPct) { onCustomPctChange(null, it, null) }
                    PctInputRow("Savings", state.savingsPct) { onCustomPctChange(null, null, it) }
                    Spacer(Modifier.height(8.dp))
                    val color = if (sumValid) Color(0xFF1A7C5C) else Color(0xFFEF4444)
                    Text(
                        text = "Sum: ${"%.1f".format(pctSum)}% ${if (!sumValid) "(must be 100%)" else "✓"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = color
                    )
                    if (sumValid) {
                        Spacer(Modifier.height(12.dp))
                        StackedBucketBar(state.needsPct, state.wantsPct, state.savingsPct)
                        Spacer(Modifier.height(8.dp))
                        BucketAmounts(state.needsPct, state.wantsPct, state.savingsPct, income)
                    }
                }
            }
        }
    }
}

@Composable
private fun PctInputRow(label: String, value: Double, onChange: (Double) -> Unit) {
    var text by remember(label) { mutableStateOf(value.toInt().toString()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        OutlinedTextField(
            value = text,
            onValueChange = { raw ->
                val cleaned = raw.filter { it.isDigit() || it == '.' }.take(5)
                text = cleaned
                onChange(cleaned.toDoubleOrNull() ?: 0.0)
            },
            suffix = { Text("%") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(90.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TealGreen)
        )
    }
}

@Composable
private fun StackedBucketBar(needsPct: Double, wantsPct: Double, savingsPct: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .background(Color(0xFFE5E7EB), RoundedCornerShape(5.dp))
    ) {
        val total = needsPct + wantsPct + savingsPct
        if (total > 0) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight((needsPct / total).toFloat())
                    .background(Color(0xFF1A7C5C), RoundedCornerShape(topStart = 5.dp, bottomStart = 5.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight((wantsPct / total).toFloat())
                    .background(Color(0xFF60A5FA))
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight((savingsPct / total).toFloat())
                    .background(Color(0xFFFBBF24), RoundedCornerShape(topEnd = 5.dp, bottomEnd = 5.dp))
            )
        }
    }
}

@Composable
private fun BucketAmounts(needsPct: Double, wantsPct: Double, savingsPct: Double, income: Double) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        BucketLabel("Needs", needsPct, income, Color(0xFF1A7C5C))
        BucketLabel("Wants", wantsPct, income, Color(0xFF3B82F6))
        BucketLabel("Savings", savingsPct, income, Color(0xFFF59E0B))
    }
}

@Composable
private fun BucketLabel(label: String, pct: Double, income: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = IndianCurrencyFormatter.compact(income * pct / 100), style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
    }
}
