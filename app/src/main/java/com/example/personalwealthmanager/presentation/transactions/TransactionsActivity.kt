package com.example.personalwealthmanager.presentation.transactions

import android.app.ActivityOptions
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.viewModels
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.personalwealthmanager.R
import com.example.personalwealthmanager.domain.model.Transaction
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class TransactionsActivity : com.example.personalwealthmanager.presentation.base.BaseDrawerActivity() {

    override fun getActiveNavItem() = BottomNavItem.TRANSACTIONS
    override fun getSelfButtonId() = R.id.btnTransactions

    private val viewModel: TransactionListViewModel by viewModels()

    private lateinit var rvTransactions: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var filterDrawer: LinearLayout
    private lateinit var fabAddTransaction: FloatingActionButton
    private lateinit var adapter: TransactionsAdapter

    // Summary cards
    private lateinit var cardSummaryRow: LinearLayout
    private lateinit var tvCurrentLedgerBalance: TextView
    private lateinit var tvCurrentLedgerUpdated: TextView
    private lateinit var cardAnalysis: LinearLayout

    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerSource: Spinner
    private lateinit var spinnerRecipient: Spinner

    private var selectedTypeFilter = "both"

    // Chart views
    private lateinit var chartContainer: LinearLayout
    private lateinit var tvChartTitle: TextView
    private lateinit var tvChartDateRange: TextView
    private lateinit var lineChart: LineChart
    private lateinit var tvLineChartEmpty: TextView
    private lateinit var pieChartsContainer: LinearLayout
    private lateinit var pieIncomeByCategory: PieChart
    private lateinit var pieExpenseByCategory: PieChart
    private lateinit var pieIncomeBySource: PieChart
    private lateinit var pieExpenseByRecipient: PieChart

    private enum class ViewMode { LIST, WEEKLY, DAILY, MONTHLY, SOURCE_RECIPIENT }
    private var currentViewMode = ViewMode.LIST

    private val displayDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transactions)

        initializeViews()
        setupRecyclerView()
        setupClickListeners()
        setupDrawerMenu()
        setupBottomNav()
        setupFilterDrawer()
        observeState()

        loadInitialFilters()
        viewModel.loadTransactions()
    }

    private fun initializeViews() {
        rvTransactions = findViewById(R.id.rvTransactions)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        progressBar = findViewById(R.id.progressBar)
        filterDrawer = findViewById(R.id.filterDrawer)
        fabAddTransaction = findViewById(R.id.fabAddTransaction)

        cardSummaryRow = findViewById(R.id.cardSummaryRow)
        tvCurrentLedgerBalance = findViewById(R.id.tvCurrentLedgerBalance)
        tvCurrentLedgerUpdated = findViewById(R.id.tvCurrentLedgerUpdated)
        cardAnalysis = findViewById(R.id.cardAnalysis)

        chartContainer = findViewById(R.id.chartContainer)
        tvChartTitle = findViewById(R.id.tvChartTitle)
        tvChartDateRange = findViewById(R.id.tvChartDateRange)
        lineChart = findViewById(R.id.lineChart)
        tvLineChartEmpty = findViewById(R.id.tvLineChartEmpty)
        pieChartsContainer = findViewById(R.id.pieChartsContainer)
        pieIncomeByCategory = findViewById(R.id.pieIncomeByCategory)
        pieExpenseByCategory = findViewById(R.id.pieExpenseByCategory)
        pieIncomeBySource = findViewById(R.id.pieIncomeBySource)
        pieExpenseByRecipient = findViewById(R.id.pieExpenseByRecipient)
    }

    private fun setupRecyclerView() {
        adapter = TransactionsAdapter(
            onItemClick = { transaction ->
                val intent = Intent(this, EditTransactionActivity::class.java)
                intent.putExtra("transaction_id", transaction.transactionId)
                intent.putExtra("is_income", transaction.type == "income")
                intent.putExtra("mode", "edit")
                startActivity(intent)
            },
            onItemLongClick = { transaction ->
                showDeleteConfirmation(transaction)
            }
        )
        rvTransactions.layoutManager = LinearLayoutManager(this)
        rvTransactions.adapter = adapter
    }

    private fun setupClickListeners() {
        // Back button — if in chart mode go back to list, else finish
        findViewById<View>(R.id.btnBack).setOnClickListener {
            when {
                currentViewMode != ViewMode.LIST -> backToListView()
                isFilterDrawerOpen() -> closeFilterDrawer()
                else -> finish()
            }
        }

        // Filter button (icon + label) — open filter drawer
        findViewById<View>(R.id.btnFilters).setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) drawerLayout.closeDrawer(GravityCompat.END)
            openFilterDrawer()
        }

        // Analysis card — show chart mode popup (Weekly / Daily / Monthly / Source & Recipient)
        cardAnalysis.setOnClickListener { anchor ->
            showViewPopupMenu(anchor)
        }

        fabAddTransaction.setOnClickListener { view ->
            val intent = Intent(this, EditTransactionActivity::class.java)
            intent.putExtra("mode", "add")
            val options = ActivityOptions.makeScaleUpAnimation(
                view, 0, 0, view.width, view.height
            )
            startActivity(intent, options.toBundle())
        }

        findViewById<FrameLayout>(R.id.filterDrawerContainer).setOnClickListener {
            closeFilterDrawer()
        }

        filterDrawer.setOnClickListener { /* consume touches inside drawer */ }

        findViewById<Button>(R.id.btnBackToList).setOnClickListener {
            backToListView()
        }
    }

    // ─── View mode popup (custom white dropdown matching Figma) ──────────────

    private fun showViewPopupMenu(anchor: View) {
        val popupView = LayoutInflater.from(this)
            .inflate(R.layout.dropdown_chart_options, null)

        val widthPx = (200 * resources.displayMetrics.density).toInt()
        val popup = PopupWindow(
            popupView,
            widthPx,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 12f
            setBackgroundDrawable(ColorDrawable(Color.WHITE))
            isOutsideTouchable = true
        }

        popupView.findViewById<TextView>(R.id.optionWeekly).setOnClickListener {
            popup.dismiss(); activateChartView(ViewMode.WEEKLY)
        }
        popupView.findViewById<TextView>(R.id.optionDaily).setOnClickListener {
            popup.dismiss(); activateChartView(ViewMode.DAILY)
        }
        popupView.findViewById<TextView>(R.id.optionMonthly).setOnClickListener {
            popup.dismiss(); activateChartView(ViewMode.MONTHLY)
        }
        popupView.findViewById<TextView>(R.id.optionSourceRecipient).setOnClickListener {
            popup.dismiss(); activateChartView(ViewMode.SOURCE_RECIPIENT)
        }

        // Position below the anchor (Analysis card), aligned to its right edge
        popup.showAsDropDown(anchor, 0, 0, Gravity.END)
    }

    private fun activateChartView(mode: ViewMode) {
        currentViewMode = mode

        rvTransactions.visibility = View.GONE
        tvEmptyState.visibility = View.GONE
        cardSummaryRow.visibility = View.GONE
        chartContainer.visibility = View.VISIBLE
        fabAddTransaction.hide()

        tvChartTitle.text = when (mode) {
            ViewMode.WEEKLY -> getString(R.string.weekly_view)
            ViewMode.DAILY -> getString(R.string.daily_view)
            ViewMode.MONTHLY -> getString(R.string.monthly_view)
            ViewMode.SOURCE_RECIPIENT -> getString(R.string.source_recipient_view)
            ViewMode.LIST -> ""
        }

        val filter = viewModel.state.value.filter
        tvChartDateRange.text = buildDateRangeText(filter)

        val transactions = viewModel.state.value.transactions
        when (mode) {
            ViewMode.WEEKLY -> { showLineChartSection(); populateWeeklyChart(transactions) }
            ViewMode.DAILY -> { showLineChartSection(); populateDailyChart(transactions) }
            ViewMode.MONTHLY -> { showLineChartSection(); populateMonthlyChart(transactions) }
            ViewMode.SOURCE_RECIPIENT -> { showPieChartSection(); populatePieCharts(transactions) }
            ViewMode.LIST -> {}
        }
    }

    private fun backToListView() {
        currentViewMode = ViewMode.LIST
        chartContainer.visibility = View.GONE
        cardSummaryRow.visibility = View.VISIBLE
        fabAddTransaction.show()

        val state = viewModel.state.value
        if (state.transactions.isEmpty() && !state.isLoading) {
            tvEmptyState.visibility = View.VISIBLE
            rvTransactions.visibility = View.GONE
        } else {
            tvEmptyState.visibility = View.GONE
            rvTransactions.visibility = View.VISIBLE
        }
    }

    // ─── Chart section visibility helpers ─────────────────────────────────────

    private fun showLineChartSection() {
        lineChart.visibility = View.GONE
        tvLineChartEmpty.visibility = View.GONE
        pieChartsContainer.visibility = View.GONE
    }

    private fun showPieChartSection() {
        lineChart.visibility = View.GONE
        tvLineChartEmpty.visibility = View.GONE
        pieChartsContainer.visibility = View.VISIBLE
    }

    // ─── Line chart data builders ─────────────────────────────────────────────

    private fun populateWeeklyChart(transactions: List<Transaction>) {
        data class WeekKey(val year: Int, val week: Int)

        val cal = Calendar.getInstance()
        val incomeMap = mutableMapOf<WeekKey, Float>()
        val expenseMap = mutableMapOf<WeekKey, Float>()

        for (t in transactions) {
            val date = parseApiDate(t.date) ?: continue
            cal.time = date
            val key = WeekKey(cal.get(Calendar.YEAR), cal.get(Calendar.WEEK_OF_YEAR))
            val amount = t.amount.toFloatOrNull() ?: 0f
            if (t.type == "income") incomeMap[key] = (incomeMap[key] ?: 0f) + amount
            else expenseMap[key] = (expenseMap[key] ?: 0f) + amount
        }

        val allKeys = (incomeMap.keys + expenseMap.keys).distinct()
            .sortedWith(compareBy({ it.year }, { it.week }))

        if (allKeys.isEmpty()) { showNoLineData(); return }

        val labels = allKeys.map { "W${it.week}" }
        val incomeEntries = allKeys.mapIndexed { i, k -> Entry(i.toFloat(), incomeMap[k] ?: 0f) }
        val expenseEntries = allKeys.mapIndexed { i, k -> Entry(i.toFloat(), expenseMap[k] ?: 0f) }
        renderLineChart(incomeEntries, expenseEntries, labels, labelRotation = -45f, visibleXCount = 8)
    }

    private fun populateDailyChart(transactions: List<Transaction>) {
        val ddMMyyyy = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val incomeMap = mutableMapOf<String, Float>()
        val expenseMap = mutableMapOf<String, Float>()
        val dateOrder = mutableListOf<String>()

        for (t in transactions) {
            val date = parseApiDate(t.date) ?: continue
            val label = ddMMyyyy.format(date)
            if (label !in incomeMap) { dateOrder.add(label); incomeMap[label] = 0f; expenseMap[label] = 0f }
            val amount = t.amount.toFloatOrNull() ?: 0f
            if (t.type == "income") incomeMap[label] = (incomeMap[label] ?: 0f) + amount
            else expenseMap[label] = (expenseMap[label] ?: 0f) + amount
        }

        val sortedLabels = dateOrder.sortedWith(Comparator { a, b ->
            val da = runCatching { ddMMyyyy.parse(a)!! }.getOrNull()
            val db = runCatching { ddMMyyyy.parse(b)!! }.getOrNull()
            (da?.time ?: 0L).compareTo(db?.time ?: 0L)
        })

        if (sortedLabels.isEmpty()) { showNoLineData(); return }

        val incomeEntries = sortedLabels.mapIndexed { i, lbl -> Entry(i.toFloat(), incomeMap[lbl] ?: 0f) }
        val expenseEntries = sortedLabels.mapIndexed { i, lbl -> Entry(i.toFloat(), expenseMap[lbl] ?: 0f) }
        renderLineChart(incomeEntries, expenseEntries, sortedLabels, labelRotation = -90f, visibleXCount = 7)
    }

    private fun populateMonthlyChart(transactions: List<Transaction>) {
        val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

        data class MonthKey(val year: Int, val month: Int)

        val cal = Calendar.getInstance()
        val incomeMap = mutableMapOf<MonthKey, Float>()
        val expenseMap = mutableMapOf<MonthKey, Float>()

        for (t in transactions) {
            val date = parseApiDate(t.date) ?: continue
            cal.time = date
            val key = MonthKey(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
            val amount = t.amount.toFloatOrNull() ?: 0f
            if (t.type == "income") incomeMap[key] = (incomeMap[key] ?: 0f) + amount
            else expenseMap[key] = (expenseMap[key] ?: 0f) + amount
        }

        val allKeys = (incomeMap.keys + expenseMap.keys).distinct()
            .sortedWith(compareBy({ it.year }, { it.month }))

        if (allKeys.isEmpty()) { showNoLineData(); return }

        val labels = allKeys.map { "${monthNames[it.month]} ${it.year}" }
        val incomeEntries = allKeys.mapIndexed { i, k -> Entry(i.toFloat(), incomeMap[k] ?: 0f) }
        val expenseEntries = allKeys.mapIndexed { i, k -> Entry(i.toFloat(), expenseMap[k] ?: 0f) }
        renderLineChart(incomeEntries, expenseEntries, labels, labelRotation = -45f, visibleXCount = 6)
    }

    private fun showNoLineData() {
        lineChart.visibility = View.GONE
        tvLineChartEmpty.visibility = View.VISIBLE
    }

    private fun renderLineChart(
        incomeEntries: List<Entry>,
        expenseEntries: List<Entry>,
        labels: List<String>,
        labelRotation: Float = 0f,
        visibleXCount: Int = 7
    ) {
        tvLineChartEmpty.visibility = View.GONE
        lineChart.visibility = View.VISIBLE

        val chartTextColor = Color.parseColor("#333333")
        val chartGridColor = Color.parseColor("#DDDDDD")
        val chartAxisColor = Color.parseColor("#AAAAAA")

        val incomeSet = LineDataSet(incomeEntries, getString(R.string.income_line)).apply {
            color = Color.parseColor("#4CAF50")
            setCircleColor(Color.parseColor("#4CAF50"))
            lineWidth = 2.5f
            circleRadius = 4f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        val expenseSet = LineDataSet(expenseEntries, getString(R.string.expense_line)).apply {
            color = Color.parseColor("#F44336")
            setCircleColor(Color.parseColor("#F44336"))
            lineWidth = 2.5f
            circleRadius = 4f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        val extraBottom = when {
            labelRotation <= -80f -> 90f
            labelRotation <= -40f -> 50f
            else -> 10f
        }

        lineChart.apply {
            data = LineData(incomeSet, expenseSet)
            description.isEnabled = false
            setBackgroundColor(Color.TRANSPARENT)
            setExtraBottomOffset(extraBottom)

            setPinchZoom(false)
            isScaleXEnabled = false
            isScaleYEnabled = true
            isDragEnabled = true
            setDoubleTapToZoomEnabled(true)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = chartTextColor
                gridColor = chartGridColor
                axisLineColor = chartAxisColor
                granularity = 1f
                labelRotationAngle = labelRotation
                setLabelCount(minOf(labels.size, visibleXCount), false)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val idx = value.toInt().coerceIn(0, labels.size - 1)
                        return labels[idx]
                    }
                }
            }

            axisLeft.apply {
                textColor = chartTextColor
                gridColor = chartGridColor
                axisLineColor = chartAxisColor
                axisMinimum = 0f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String =
                        if (value >= 1_000f) "₹${(value / 1000).toInt()}k" else "₹${value.toInt()}"
                }
            }

            axisRight.isEnabled = false

            legend.apply {
                textColor = chartTextColor
                textSize = 12f
                form = Legend.LegendForm.LINE
                verticalAlignment = Legend.LegendVerticalAlignment.TOP
                horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
            }

            if (labels.size > visibleXCount) {
                setVisibleXRangeMaximum(visibleXCount.toFloat())
                moveViewToX(data.entryCount.toFloat())
            }

            animateXY(900, 600, Easing.EaseInOutCubic, Easing.EaseOutQuad)
        }
    }

    // ─── Pie chart builders ────────────────────────────────────────────────────

    private fun populatePieCharts(transactions: List<Transaction>) {
        val income = transactions.filter { it.type == "income" }
        val expense = transactions.filter { it.type == "expense" }

        setupPieChart(pieIncomeByCategory,
            income.groupBy { it.categoryName }
                .mapValues { (_, list) -> list.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }.toFloat() })
        setupPieChart(pieExpenseByCategory,
            expense.groupBy { it.categoryName }
                .mapValues { (_, list) -> list.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }.toFloat() })
        setupPieChart(pieIncomeBySource,
            income.groupBy { it.sourceName ?: getString(R.string.no_data) }
                .mapValues { (_, list) -> list.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }.toFloat() })
        setupPieChart(pieExpenseByRecipient,
            expense.groupBy { it.recipientName ?: getString(R.string.no_data) }
                .mapValues { (_, list) -> list.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }.toFloat() })
    }

    private fun setupPieChart(chart: PieChart, data: Map<String, Float>) {
        fun fmt(v: Float) = if (v >= 1000f) "₹${(v / 1000f).let {
            if (it == it.toLong().toFloat()) it.toLong().toString() else "%.1f".format(it)
        }}k" else "₹${v.toInt()}"

        val chartTextColor = Color.parseColor("#333333")
        val sorted = data.entries.sortedByDescending { it.value }
        val entries = sorted.map { PieEntry(it.value, "${it.key}  ${fmt(it.value)}") }

        val colors = mutableListOf<Int>().apply {
            addAll(ColorTemplate.MATERIAL_COLORS.toList())
            addAll(ColorTemplate.COLORFUL_COLORS.toList())
            addAll(ColorTemplate.JOYFUL_COLORS.toList())
        }

        val dataSet = PieDataSet(entries, "").apply {
            setColors(colors)
            valueTextColor = chartTextColor
            valueTextSize = 11f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float) = "%.1f%%".format(value)
            }
        }

        chart.apply {
            this.data = if (entries.isEmpty()) null else PieData(dataSet)
            notifyDataSetChanged()
            description.isEnabled = false
            setUsePercentValues(true)
            setDrawEntryLabels(false)
            setHoleColor(Color.TRANSPARENT)
            holeRadius = 35f
            transparentCircleRadius = 40f
            setTransparentCircleColor(Color.parseColor("#22000000"))
            isRotationEnabled = true

            legend.apply {
                textColor = chartTextColor
                textSize = 11f
                isWordWrapEnabled = true
                form = Legend.LegendForm.CIRCLE
            }

            if (entries.isEmpty()) {
                setNoDataText(getString(R.string.no_data_for_period))
                setNoDataTextColor(Color.parseColor("#666666"))
                invalidate()
            } else {
                invalidate()
                animateY(900, Easing.EaseInOutQuart)
            }
        }
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private fun parseApiDate(raw: String): Date? =
        runCatching { apiDateFormat.parse(raw) }.getOrNull()

    private fun buildDateRangeText(filter: FilterState): String {
        val from = filter.dateFrom?.let {
            runCatching { displayDateFormat.format(apiDateFormat.parse(it)!!) }.getOrElse { it }
        }
        val to = filter.dateTo?.let {
            runCatching { displayDateFormat.format(apiDateFormat.parse(it)!!) }.getOrElse { it }
        }
        return when {
            from != null && to != null -> "$from  →  $to"
            from != null -> "From $from"
            to != null -> "Until $to"
            else -> getString(R.string.all_transactions)
        }
    }

    private fun updateLedgerCard(transactions: List<Transaction>) {
        val income = transactions.filter { it.type == "income" }
            .sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
        val expense = transactions.filter { it.type == "expense" }
            .sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
        val balance = income - expense

        val prefix = if (balance < 0) "− " else ""
        tvCurrentLedgerBalance.text = prefix + currencyFormat.format(Math.abs(balance))

        val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        tvCurrentLedgerUpdated.text = "Updated at $timeStr"
    }

    // ─── Filter drawer ─────────────────────────────────────────────────────────

    private fun setupFilterDrawer() {
        val etDateFrom = filterDrawer.findViewById<EditText>(R.id.etDateFrom)
        val etDateTo = filterDrawer.findViewById<EditText>(R.id.etDateTo)
        val etMinAmount = filterDrawer.findViewById<EditText>(R.id.etMinAmount)
        val etMaxAmount = filterDrawer.findViewById<EditText>(R.id.etMaxAmount)
        val btnReset = filterDrawer.findViewById<Button>(R.id.btnResetFilters)
        val btnApply = filterDrawer.findViewById<Button>(R.id.btnApplyFilters)
        val btnClose = filterDrawer.findViewById<View>(R.id.btnCloseFilters)
        val btnTypeChipBoth = filterDrawer.findViewById<Button>(R.id.btnTypeChipBoth)
        val btnTypeChipIncome = filterDrawer.findViewById<Button>(R.id.btnTypeChipIncome)
        val btnTypeChipExpense = filterDrawer.findViewById<Button>(R.id.btnTypeChipExpense)

        // Clear Material tint on initial render so XML backgrounds show correctly
        listOf(btnTypeChipBoth, btnTypeChipIncome, btnTypeChipExpense)
            .forEach { it.backgroundTintList = null }

        spinnerCategory = filterDrawer.findViewById(R.id.spinnerCategory)
        spinnerSource = filterDrawer.findViewById(R.id.spinnerSource)
        spinnerRecipient = filterDrawer.findViewById(R.id.spinnerRecipient)

        btnClose.setOnClickListener { closeFilterDrawer() }

        fun updateTypeChips(selected: String) {
            selectedTypeFilter = selected
            listOf(
                btnTypeChipBoth to "both",
                btnTypeChipIncome to "income",
                btnTypeChipExpense to "expense"
            ).forEach { (btn, type) ->
                val isActive = selected == type
                btn.backgroundTintList = null  // clear Material primary tint
                btn.background = ContextCompat.getDrawable(
                    this,
                    if (isActive) R.drawable.bg_filter_chip_active else R.drawable.bg_filter_chip_inactive
                )
                btn.setTextColor(
                    if (isActive) Color.WHITE else getColor(R.color.tx_text_secondary)
                )
            }
        }

        btnTypeChipBoth.setOnClickListener { updateTypeChips("both") }
        btnTypeChipIncome.setOnClickListener { updateTypeChips("income") }
        btnTypeChipExpense.setOnClickListener { updateTypeChips("expense") }

        val allOnlyAdapter = {
            ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf("All")).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        }
        spinnerCategory.adapter = allOnlyAdapter()
        spinnerSource.adapter = allOnlyAdapter()
        spinnerRecipient.adapter = allOnlyAdapter()

        etDateFrom.setOnClickListener { showDatePicker { etDateFrom.setText(displayDateFormat.format(it)) } }
        etDateTo.setOnClickListener { showDatePicker { etDateTo.setText(displayDateFormat.format(it)) } }

        btnReset.setOnClickListener {
            etDateFrom.setText("")
            etDateTo.setText("")
            updateTypeChips("both")
            spinnerCategory.setSelection(0)
            spinnerSource.setSelection(0)
            spinnerRecipient.setSelection(0)
            etMinAmount.setText("")
            etMaxAmount.setText("")
            viewModel.resetFilters()
            closeFilterDrawer()
            viewModel.loadTransactions()
        }

        btnApply.setOnClickListener {
            val dateFrom = if (etDateFrom.text.isNotEmpty()) {
                runCatching { apiDateFormat.format(displayDateFormat.parse(etDateFrom.text.toString())!!) }.getOrNull()
            } else null

            val dateTo = if (etDateTo.text.isNotEmpty()) {
                runCatching { apiDateFormat.format(displayDateFormat.parse(etDateTo.text.toString())!!) }.getOrNull()
            } else null

            val metadata = viewModel.state.value.metadata
            val allCategories = metadata.incomeCategories + metadata.expenseCategories
            val categoryId = if (spinnerCategory.selectedItemPosition > 0)
                allCategories.getOrNull(spinnerCategory.selectedItemPosition - 1)?.id else null

            val sourceId = if (spinnerSource.selectedItemPosition > 0)
                metadata.sources.getOrNull(spinnerSource.selectedItemPosition - 1)?.id else null

            val recipientId = if (spinnerRecipient.selectedItemPosition > 0)
                metadata.recipients.getOrNull(spinnerRecipient.selectedItemPosition - 1)?.id else null

            viewModel.updateFilter(
                FilterState(
                    dateFrom = dateFrom, dateTo = dateTo, type = selectedTypeFilter,
                    categoryId = categoryId, sourceId = sourceId, recipientId = recipientId,
                    minAmount = etMinAmount.text.toString().ifEmpty { null },
                    maxAmount = etMaxAmount.text.toString().ifEmpty { null }
                )
            )
            closeFilterDrawer()
            viewModel.loadTransactions()
        }
    }

    private fun loadInitialFilters() {
        val filterType = intent.getStringExtra("filter_type")
        val dateFrom = intent.getStringExtra("date_from")
        val dateTo = intent.getStringExtra("date_to")
        if (filterType != null || dateFrom != null || dateTo != null) {
            viewModel.updateFilter(FilterState(type = filterType ?: "both", dateFrom = dateFrom, dateTo = dateTo))
        }
    }

    private fun showDatePicker(onDateSelected: (Date) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            calendar.set(year, month, day)
            onDateSelected(calendar.time)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun openFilterDrawer() {
        findViewById<FrameLayout>(R.id.filterDrawerContainer).visibility = View.VISIBLE
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            findViewById<FrameLayout>(R.id.mainContentWrapper).setRenderEffect(
                android.graphics.RenderEffect.createBlurEffect(18f, 18f, android.graphics.Shader.TileMode.CLAMP)
            )
        }
        filterDrawer.translationX = -filterDrawer.width.toFloat()
        filterDrawer.animate().translationX(0f).setDuration(300).start()
    }

    private fun closeFilterDrawer() {
        filterDrawer.animate().translationX(-filterDrawer.width.toFloat()).setDuration(300)
            .withEndAction {
                findViewById<FrameLayout>(R.id.filterDrawerContainer).visibility = View.GONE
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    findViewById<FrameLayout>(R.id.mainContentWrapper).setRenderEffect(null)
                }
            }
            .start()
    }

    private fun isFilterDrawerOpen() =
        findViewById<FrameLayout>(R.id.filterDrawerContainer).visibility == View.VISIBLE

    private fun showDeleteConfirmation(transaction: Transaction) {
        AlertDialog.Builder(this)
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteTransaction(transaction.transactionId, transaction.type == "income")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ─── State observation ────────────────────────────────────────────────────

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                if (currentViewMode == ViewMode.LIST) {
                    if (state.transactions.isEmpty() && !state.isLoading) {
                        tvEmptyState.visibility = View.VISIBLE
                        rvTransactions.visibility = View.GONE
                    } else {
                        tvEmptyState.visibility = View.GONE
                        rvTransactions.visibility = View.VISIBLE
                        adapter.updateTransactions(state.transactions)
                    }
                } else if (!state.isLoading) {
                    tvChartDateRange.text = buildDateRangeText(state.filter)
                    when (currentViewMode) {
                        ViewMode.WEEKLY -> { showLineChartSection(); populateWeeklyChart(state.transactions) }
                        ViewMode.DAILY -> { showLineChartSection(); populateDailyChart(state.transactions) }
                        ViewMode.MONTHLY -> { showLineChartSection(); populateMonthlyChart(state.transactions) }
                        ViewMode.SOURCE_RECIPIENT -> { showPieChartSection(); populatePieCharts(state.transactions) }
                        ViewMode.LIST -> {}
                    }
                }

                // Update Current Ledger balance card
                if (!state.isLoading) {
                    updateLedgerCard(state.transactions)
                }

                state.error?.let { error ->
                    Toast.makeText(this@TransactionsActivity, error, Toast.LENGTH_LONG).show()
                }

                // Populate filter spinners
                val allCategories = state.metadata.incomeCategories + state.metadata.expenseCategories
                if (spinnerCategory.adapter?.count != allCategories.size + 1) {
                    spinnerCategory.adapter = ArrayAdapter(this@TransactionsActivity,
                        android.R.layout.simple_spinner_item,
                        listOf("All") + allCategories.map { it.name }).apply {
                        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    }
                }

                if (spinnerSource.adapter?.count != state.metadata.sources.size + 1) {
                    spinnerSource.adapter = ArrayAdapter(this@TransactionsActivity,
                        android.R.layout.simple_spinner_item,
                        listOf("All") + state.metadata.sources.map { it.name }).apply {
                        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    }
                }

                if (spinnerRecipient.adapter?.count != state.metadata.recipients.size + 1) {
                    spinnerRecipient.adapter = ArrayAdapter(this@TransactionsActivity,
                        android.R.layout.simple_spinner_item,
                        listOf("All") + state.metadata.recipients.map { it.name }).apply {
                        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadTransactions()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            currentViewMode != ViewMode.LIST -> backToListView()
            isFilterDrawerOpen() -> closeFilterDrawer()
            drawerLayout.isDrawerOpen(GravityCompat.END) -> drawerLayout.closeDrawer(GravityCompat.END)
            else -> super.onBackPressed()
        }
    }
}
