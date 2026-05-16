package com.pwm.personalwealthmanager.presentation.budget.screens

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.pwm.personalwealthmanager.core.utils.IndianCurrencyFormatter
import com.pwm.personalwealthmanager.presentation.budget.BudgetBottomNavBar
import com.pwm.personalwealthmanager.presentation.budget.BudgetNavTab
import com.pwm.personalwealthmanager.data.remote.dto.AdherencePointDto
import com.pwm.personalwealthmanager.data.remote.dto.CalendarMonthDto
import com.pwm.personalwealthmanager.data.remote.dto.CategoryStreakDto
import com.pwm.personalwealthmanager.data.remote.dto.ReportsDto
import com.pwm.personalwealthmanager.data.remote.dto.SavingsCumPointDto
import com.pwm.personalwealthmanager.presentation.budget.ReportsUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val TealGreen = Color(0xFF1A7C5C)

@Composable
fun ReportsScreen(
    uiState: ReportsUiState,
    onCalendarMonthTap: (month: String) -> Unit,
    onRetry: () -> Unit
) {
    Scaffold(
        containerColor = Color(0xFFF5F7FA),
        bottomBar = { BudgetBottomNavBar(activeTab = BudgetNavTab.BUDGET) },
        topBar = {
            Surface(shadowElevation = 2.dp, color = Color.White) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        "Budget Reports",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { padding ->
        when (uiState) {
            is ReportsUiState.Idle, ReportsUiState.Loading ->
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TealGreen)
                }

            is ReportsUiState.Error ->
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.message, textAlign = TextAlign.Center,
                            color = Color(0xFF6B7280))
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = onRetry) { Text("Retry") }
                    }
                }

            is ReportsUiState.Success ->
                ReportsContent(
                    data = uiState.data,
                    modifier = Modifier.padding(padding),
                    onCalendarMonthTap = onCalendarMonthTap
                )
        }
    }
}

@Composable
private fun ReportsContent(
    data: ReportsDto,
    modifier: Modifier,
    onCalendarMonthTap: (String) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        // KPI tiles
        item { KpiRow(data) }

        // Calendar grid
        item {
            Text("Monthly history", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold, color = Color(0xFF374151))
        }
        item {
            CalendarGrid(months = data.calendar, onTap = onCalendarMonthTap)
        }

        // Framework adherence chart
        if (data.frameworkAdherenceTrend.size >= 2) {
            item {
                Text("Framework adherence", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, color = Color(0xFF374151))
            }
            item {
                AdherenceChart(points = data.frameworkAdherenceTrend)
            }
        }

        // Category streaks
        if (data.categoryStreaks.isNotEmpty()) {
            item {
                Text("Category streaks", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, color = Color(0xFF374151))
            }
            items(data.categoryStreaks.take(10)) { streak ->
                StreakRow(streak)
            }
        }

        // Savings cumulative chart
        if (data.savingsCumulative.size >= 2) {
            item {
                Text("Savings progress", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, color = Color(0xFF374151))
            }
            item {
                SavingsCumulativeChart(points = data.savingsCumulative)
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun KpiRow(data: ReportsDto) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        KpiCard(
            label = "On track",
            value = "${data.kpi.monthsOnTrack}/${data.kpi.totalMonthsWithPlan}",
            subtitle = "${data.kpi.hitRatePct}%",
            modifier = Modifier.weight(1f)
        )
        KpiCard(
            label = "Streak",
            value = "${data.kpi.currentStreakMonths}",
            subtitle = "months",
            modifier = Modifier.weight(1f)
        )
        KpiCard(
            label = "Avg stars",
            value = "${data.kpi.avgStars}⭐",
            subtitle = "last ${data.kpi.totalMonthsWithPlan}mo",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun KpiCard(label: String, value: String, subtitle: String, modifier: Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color(0xFF6B7280))
        }
    }
}

@Composable
private fun CalendarGrid(months: List<CalendarMonthDto>, onTap: (String) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Render in rows of 4
            val rows = months.chunked(4)
            rows.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { month ->
                        CalendarCell(
                            month = month,
                            modifier = Modifier.weight(1f),
                            onTap = { onTap(month.month) }
                        )
                    }
                    // Fill remaining cells with empty weights if row is incomplete
                    repeat(4 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun CalendarCell(month: CalendarMonthDto, modifier: Modifier, onTap: () -> Unit) {
    val stars = month.overallStars ?: 0
    val bg = when {
        !month.isLocked -> Color(0xFFF3F4F6)
        stars >= 4      -> Color(0xFFD1FAE5)
        stars >= 3      -> Color(0xFFFEF3C7)
        stars >= 1      -> Color(0xFFFEE2E2)
        else            -> Color(0xFFF3F4F6)
    }
    val label = try {
        LocalDate.parse(month.month, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            .format(DateTimeFormatter.ofPattern("MMM"))
    } catch (_: Exception) { "—" }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
            if (month.isLocked && stars > 0) {
                Text("${"⭐".repeat(stars.coerceIn(0, 5))}", fontSize = 8.sp)
            } else if (!month.isLocked) {
                Box(
                    Modifier.size(6.dp).clip(CircleShape)
                        .background(TealGreen.copy(alpha = 0.5f))
                )
            }
        }
    }
}

@Composable
private fun AdherenceChart(points: List<AdherencePointDto>) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)) {
        AndroidView(
            factory = { ctx ->
                LineChart(ctx).apply {
                    description.isEnabled = false
                    legend.isEnabled = false
                    setTouchEnabled(false)
                    xAxis.position = XAxis.XAxisPosition.BOTTOM
                    xAxis.setDrawGridLines(false)
                    xAxis.granularity = 1f
                    axisLeft.axisMinimum = 0f
                    axisLeft.axisMaximum = 100f
                    axisRight.isEnabled = false
                    setPadding(8, 16, 8, 8)
                }
            },
            update = { chart ->
                val entries = points.mapIndexed { i, p ->
                    Entry(i.toFloat(), (p.adherencePct ?: 0.0).toFloat())
                }
                val labels = points.map { p ->
                    try {
                        LocalDate.parse(p.month, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                            .format(DateTimeFormatter.ofPattern("MMM"))
                    } catch (_: Exception) { p.month.takeLast(5) }
                }
                chart.xAxis.valueFormatter =
                    com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels)

                val ds = LineDataSet(entries, "Adherence %").apply {
                    color = AndroidColor.rgb(26, 124, 92)
                    setCircleColor(AndroidColor.rgb(26, 124, 92))
                    circleRadius = 3f
                    lineWidth = 2f
                    setDrawValues(false)
                    setDrawFilled(true)
                    fillColor = AndroidColor.argb(40, 26, 124, 92)
                }
                chart.data = LineData(ds)
                chart.invalidate()
            },
            modifier = Modifier.fillMaxWidth().height(160.dp).padding(8.dp)
        )
    }
}

@Composable
private fun SavingsCumulativeChart(points: List<SavingsCumPointDto>) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)) {
        AndroidView(
            factory = { ctx ->
                LineChart(ctx).apply {
                    description.isEnabled = false
                    legend.isEnabled = true
                    setTouchEnabled(false)
                    xAxis.position = XAxis.XAxisPosition.BOTTOM
                    xAxis.setDrawGridLines(false)
                    xAxis.granularity = 1f
                    axisRight.isEnabled = false
                    setPadding(8, 16, 8, 8)
                }
            },
            update = { chart ->
                val labels = points.map { p ->
                    try {
                        LocalDate.parse(p.month, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                            .format(DateTimeFormatter.ofPattern("MMM"))
                    } catch (_: Exception) { p.month.takeLast(5) }
                }
                chart.xAxis.valueFormatter =
                    com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels)

                val plannedEntries = points.mapIndexed { i, p -> Entry(i.toFloat(), p.plannedCum.toFloat()) }
                val actualEntries  = points.mapIndexed { i, p -> Entry(i.toFloat(), p.actualCum.toFloat()) }

                val plannedDs = LineDataSet(plannedEntries, "Target").apply {
                    color = AndroidColor.rgb(156, 163, 175)
                    lineWidth = 1.5f
                    setDrawCircles(false)
                    setDrawValues(false)
                    enableDashedLine(10f, 5f, 0f)
                }
                val actualDs = LineDataSet(actualEntries, "Actual").apply {
                    color = AndroidColor.rgb(26, 124, 92)
                    setCircleColor(AndroidColor.rgb(26, 124, 92))
                    circleRadius = 3f
                    lineWidth = 2f
                    setDrawValues(false)
                    setDrawFilled(true)
                    fillColor = AndroidColor.argb(40, 26, 124, 92)
                }
                chart.data = LineData(plannedDs, actualDs)
                chart.invalidate()
            },
            modifier = Modifier.fillMaxWidth().height(180.dp).padding(8.dp)
        )
    }
}

@Composable
private fun StreakRow(streak: CategoryStreakDto) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(streak.categoryName, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium)
                if (streak.isActive) {
                    Text("Active streak", style = MaterialTheme.typography.labelSmall,
                        color = TealGreen)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${streak.activeStreakMonths}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (streak.isActive) TealGreen else Color(0xFF9CA3AF)
                )
                Text(" mo", style = MaterialTheme.typography.bodySmall, color = Color(0xFF9CA3AF))
            }
        }
    }
}
