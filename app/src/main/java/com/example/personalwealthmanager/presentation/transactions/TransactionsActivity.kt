package com.example.personalwealthmanager.presentation.transactions

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.personalwealthmanager.R
import com.example.personalwealthmanager.MainActivity
import com.example.personalwealthmanager.domain.model.Transaction
import com.example.personalwealthmanager.presentation.categories.CategoryManagementActivity
import com.example.personalwealthmanager.presentation.sources.SourceManagementActivity
import com.example.personalwealthmanager.presentation.recipients.RecipientManagementActivity
import com.example.personalwealthmanager.presentation.stocks.StocksActivity
import com.example.personalwealthmanager.presentation.zerodha.SetupZerodhaActivity
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
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class TransactionsActivity : AppCompatActivity() {

    private val viewModel: TransactionListViewModel by viewModels()

    private lateinit var rvTransactions: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var filterDrawer: LinearLayout
    private lateinit var fabAddTransaction: FloatingActionButton
    private lateinit var adapter: TransactionsAdapter

    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerSource: Spinner
    private lateinit var spinnerRecipient: Spinner

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transactions)

        initializeViews()
        setupRecyclerView()
        setupClickListeners()
        setupNavigationDrawer()
        setupFilterDrawer()
        observeState()

        loadInitialFilters()
        viewModel.loadTransactions()
    }

    private fun initializeViews() {
        rvTransactions = findViewById(R.id.rvTransactions)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        progressBar = findViewById(R.id.progressBar)
        drawerLayout = findViewById(R.id.drawerLayout)
        filterDrawer = findViewById(R.id.filterDrawer)
        fabAddTransaction = findViewById(R.id.fabAddTransaction)

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
            transactions = emptyList(),
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
        findViewById<ImageView>(R.id.btnMenu).setOnClickListener {
            if (isFilterDrawerOpen()) closeFilterDrawer()
            drawerLayout.openDrawer(GravityCompat.END)
        }

        findViewById<Button>(R.id.btnFilters).setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) drawerLayout.closeDrawer(GravityCompat.END)
            openFilterDrawer()
        }

        findViewById<Button>(R.id.btnView).setOnClickListener { anchor ->
            showViewPopupMenu(anchor)
        }

        fabAddTransaction.setOnClickListener {
            val intent = Intent(this, EditTransactionActivity::class.java)
            intent.putExtra("mode", "add")
            startActivity(intent)
        }

        findViewById<FrameLayout>(R.id.filterDrawerContainer).setOnClickListener {
            closeFilterDrawer()
        }

        filterDrawer.setOnClickListener { /* consume */ }

        findViewById<Button>(R.id.btnBackToList).setOnClickListener {
            backToListView()
        }
    }

    // ─── View mode popup ───────────────────────────────────────────────────────

    private fun showViewPopupMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, getString(R.string.weekly_view))
        popup.menu.add(0, 2, 0, getString(R.string.daily_view))
        popup.menu.add(0, 3, 0, getString(R.string.monthly_view))
        popup.menu.add(0, 4, 0, getString(R.string.source_recipient_view))
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> activateChartView(ViewMode.WEEKLY)
                2 -> activateChartView(ViewMode.DAILY)
                3 -> activateChartView(ViewMode.MONTHLY)
                4 -> activateChartView(ViewMode.SOURCE_RECIPIENT)
            }
            true
        }
        popup.show()
    }

    private fun activateChartView(mode: ViewMode) {
        currentViewMode = mode

        rvTransactions.visibility = View.GONE
        tvEmptyState.visibility = View.GONE
        chartContainer.visibility = View.VISIBLE
        fabAddTransaction.hide()

        // Title
        tvChartTitle.text = when (mode) {
            ViewMode.WEEKLY -> getString(R.string.weekly_view)
            ViewMode.DAILY -> getString(R.string.daily_view)
            ViewMode.MONTHLY -> getString(R.string.monthly_view)
            ViewMode.SOURCE_RECIPIENT -> getString(R.string.source_recipient_view)
            ViewMode.LIST -> ""
        }

        // Date range
        val filter = viewModel.state.value.filter
        tvChartDateRange.text = buildDateRangeText(filter)

        val transactions = viewModel.state.value.transactions

        when (mode) {
            ViewMode.WEEKLY -> {
                showLineChartSection()
                populateWeeklyChart(transactions)
            }
            ViewMode.DAILY -> {
                showLineChartSection()
                populateDailyChart(transactions)
            }
            ViewMode.MONTHLY -> {
                showLineChartSection()
                populateMonthlyChart(transactions)
            }
            ViewMode.SOURCE_RECIPIENT -> {
                showPieChartSection()
                populatePieCharts(transactions)
            }
            ViewMode.LIST -> {}
        }
    }

    private fun backToListView() {
        currentViewMode = ViewMode.LIST
        chartContainer.visibility = View.GONE
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

        // Sort by actual date
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

        // Extra bottom padding so vertical labels don't get clipped
        val extraBottom = when {
            labelRotation <= -80f -> 90f  // vertical (-90°)
            labelRotation <= -40f -> 50f  // diagonal (-45°)
            else -> 10f
        }

        lineChart.apply {
            data = LineData(incomeSet, expenseSet)
            description.isEnabled = false
            setBackgroundColor(Color.TRANSPARENT)
            setExtraBottomOffset(extraBottom)

            // Pinch zoom on Y-axis to adjust amount intervals; drag for horizontal scroll
            setPinchZoom(false)         // false = independent X/Y scaling
            isScaleXEnabled = false     // horizontal scroll via drag, not scale
            isScaleYEnabled = true      // pinch adjusts Y (amount intervals)
            isDragEnabled = true
            setDoubleTapToZoomEnabled(true)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.WHITE
                gridColor = Color.parseColor("#44FFFFFF")
                axisLineColor = Color.parseColor("#88FFFFFF")
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
                textColor = Color.WHITE
                gridColor = Color.parseColor("#44FFFFFF")
                axisLineColor = Color.parseColor("#88FFFFFF")
                axisMinimum = 0f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String =
                        if (value >= 1_000f) "₹${(value / 1000).toInt()}k" else "₹${value.toInt()}"
                }
            }

            axisRight.isEnabled = false

            legend.apply {
                textColor = Color.WHITE
                textSize = 12f
                form = Legend.LegendForm.LINE
                verticalAlignment = Legend.LegendVerticalAlignment.TOP
                horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
            }

            // Limit visible X range → enables horizontal drag-scroll when data > window
            if (labels.size > visibleXCount) {
                setVisibleXRangeMaximum(visibleXCount.toFloat())
                moveViewToX(data.entryCount.toFloat()) // start at the most recent end
            }

            // Animate: lines draw left-to-right, values rise from zero
            animateXY(900, 600, Easing.EaseInOutCubic, Easing.EaseOutQuad)
        }
    }

    // ─── Pie chart builders ────────────────────────────────────────────────────

    private fun populatePieCharts(transactions: List<Transaction>) {
        val income = transactions.filter { it.type == "income" }
        val expense = transactions.filter { it.type == "expense" }

        setupPieChart(
            pieIncomeByCategory,
            income.groupBy { it.categoryName }
                .mapValues { (_, list) -> list.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }.toFloat() }
        )
        setupPieChart(
            pieExpenseByCategory,
            expense.groupBy { it.categoryName }
                .mapValues { (_, list) -> list.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }.toFloat() }
        )
        setupPieChart(
            pieIncomeBySource,
            income.groupBy { it.sourceName ?: getString(R.string.no_data) }
                .mapValues { (_, list) -> list.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }.toFloat() }
        )
        setupPieChart(
            pieExpenseByRecipient,
            expense.groupBy { it.recipientName ?: getString(R.string.no_data) }
                .mapValues { (_, list) -> list.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }.toFloat() }
        )
    }

    private fun setupPieChart(chart: PieChart, data: Map<String, Float>) {
        // Helper to format raw amount for legend labels
        fun fmt(v: Float) = if (v >= 1000f) "₹${(v / 1000f).let { if (it == it.toLong().toFloat()) it.toLong().toString() else "%.1f".format(it) }}k" else "₹${v.toInt()}"

        val sorted = data.entries.sortedByDescending { it.value }
        // Embed ₹k amount in the PieEntry label → legend shows "Name  ₹Xk"
        // setDrawEntryLabels(false) keeps names off the slices themselves
        val entries = sorted.map { PieEntry(it.value, "${it.key}  ${fmt(it.value)}") }

        val colors = mutableListOf<Int>().apply {
            addAll(ColorTemplate.MATERIAL_COLORS.toList())
            addAll(ColorTemplate.COLORFUL_COLORS.toList())
            addAll(ColorTemplate.JOYFUL_COLORS.toList())
        }

        val dataSet = PieDataSet(entries, "").apply {
            setColors(colors)
            valueTextColor = Color.WHITE
            valueTextSize = 11f
            // setUsePercentValues(true) passes percentage to this formatter
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String =
                    "%.1f%%".format(value)
            }
        }

        chart.apply {
            this.data = if (entries.isEmpty()) null else PieData(dataSet)
            notifyDataSetChanged()      // sync legend entries before any draw
            description.isEnabled = false
            setUsePercentValues(true)   // slice labels show %; formatter receives 0–100
            setDrawEntryLabels(false)   // names stay off slices; legend shows them
            setHoleColor(Color.TRANSPARENT)
            holeRadius = 35f
            transparentCircleRadius = 40f
            setTransparentCircleColor(Color.parseColor("#22FFFFFF"))
            isRotationEnabled = true

            // Auto-generated legend follows PieDataSet entry order (already sorted descending)
            legend.apply {
                textColor = Color.WHITE
                textSize = 11f
                isWordWrapEnabled = true
                form = Legend.LegendForm.CIRCLE
            }

            if (entries.isEmpty()) {
                setNoDataText(getString(R.string.no_data_for_period))
                setNoDataTextColor(Color.WHITE)
                invalidate()
            } else {
                invalidate()            // stable first draw before animation starts
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

    // ─── Navigation drawer ─────────────────────────────────────────────────────

    private fun setupNavigationDrawer() {
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        val headerView = navigationView.getHeaderView(0)

        val sharedPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val userEmail = sharedPrefs.getString("user_email", "user@example.com")
        headerView.findViewById<TextView>(R.id.tvUserEmail)?.text = userEmail

        headerView.findViewById<Button>(R.id.btnDashboard)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        headerView.findViewById<Button>(R.id.btnTransactions)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        val btnManagement = headerView.findViewById<Button>(R.id.btnManagement)
        val ivManagementExpand = headerView.findViewById<ImageView>(R.id.ivManagementExpand)
        val managementChildItems = headerView.findViewById<LinearLayout>(R.id.managementChildItems)

        btnManagement?.setOnClickListener {
            if (managementChildItems?.visibility == View.VISIBLE) {
                managementChildItems.visibility = View.GONE
                ivManagementExpand?.setImageResource(R.drawable.ic_expand_more)
            } else {
                managementChildItems?.visibility = View.VISIBLE
                ivManagementExpand?.setImageResource(R.drawable.ic_expand_less)
            }
        }
        ivManagementExpand?.setOnClickListener { btnManagement?.performClick() }

        headerView.findViewById<Button>(R.id.btnCategoryManagement)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, CategoryManagementActivity::class.java))
        }

        headerView.findViewById<Button>(R.id.btnSourceManagement)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, SourceManagementActivity::class.java))
        }

        headerView.findViewById<Button>(R.id.btnRecipientManagement)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, RecipientManagementActivity::class.java))
        }

        val btnAssets = headerView.findViewById<Button>(R.id.btnAssets)
        val ivAssetsExpand = headerView.findViewById<ImageView>(R.id.ivAssetsExpand)
        val assetsChildItems = headerView.findViewById<LinearLayout>(R.id.assetsChildItems)

        btnAssets?.setOnClickListener {
            if (assetsChildItems?.visibility == View.VISIBLE) {
                assetsChildItems.visibility = View.GONE
                ivAssetsExpand?.setImageResource(R.drawable.ic_expand_more)
            } else {
                assetsChildItems?.visibility = View.VISIBLE
                ivAssetsExpand?.setImageResource(R.drawable.ic_expand_less)
            }
        }
        ivAssetsExpand?.setOnClickListener { btnAssets?.performClick() }

        headerView.findViewById<Button>(R.id.btnStocks)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, StocksActivity::class.java))
        }

        headerView.findViewById<Button>(R.id.btnMetals)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, com.example.personalwealthmanager.presentation.metals.MetalsActivity::class.java))
        }

        headerView.findViewById<Button>(R.id.btnMutualFunds)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, com.example.personalwealthmanager.presentation.mutualfunds.MutualFundsActivity::class.java))
        }

        headerView.findViewById<Button>(R.id.btnOtherAssets)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, com.example.personalwealthmanager.presentation.otherassets.OtherAssetsActivity::class.java))
        }

        headerView.findViewById<Button>(R.id.btnLiabilities)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, com.example.personalwealthmanager.presentation.liabilities.LiabilitiesActivity::class.java))
        }

        val btnSetupDemat = headerView.findViewById<Button>(R.id.btnSetupDemat)
        val ivSetupDematExpand = headerView.findViewById<ImageView>(R.id.ivSetupDematExpand)
        val setupDematChildItems = headerView.findViewById<LinearLayout>(R.id.setupDematChildItems)

        btnSetupDemat?.setOnClickListener {
            if (setupDematChildItems?.visibility == View.VISIBLE) {
                setupDematChildItems.visibility = View.GONE
                ivSetupDematExpand?.setImageResource(R.drawable.ic_expand_more)
            } else {
                setupDematChildItems?.visibility = View.VISIBLE
                ivSetupDematExpand?.setImageResource(R.drawable.ic_expand_less)
            }
        }
        ivSetupDematExpand?.setOnClickListener { btnSetupDemat?.performClick() }

        headerView.findViewById<Button>(R.id.btnConnectZerodha)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, SetupZerodhaActivity::class.java))
        }

        headerView.findViewById<Button>(R.id.btnSettings)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, com.example.personalwealthmanager.presentation.settings.SettingsActivity::class.java))
        }

        headerView.findViewById<Button>(R.id.btnLogout)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            sharedPrefs.edit().clear().apply()
            val intent = Intent(this, com.example.personalwealthmanager.presentation.auth.login.LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    // ─── Filter drawer ─────────────────────────────────────────────────────────

    private fun setupFilterDrawer() {
        val etDateFrom = filterDrawer.findViewById<EditText>(R.id.etDateFrom)
        val etDateTo = filterDrawer.findViewById<EditText>(R.id.etDateTo)
        val spinnerType = filterDrawer.findViewById<Spinner>(R.id.spinnerType)
        val etMinAmount = filterDrawer.findViewById<EditText>(R.id.etMinAmount)
        val etMaxAmount = filterDrawer.findViewById<EditText>(R.id.etMaxAmount)
        val btnReset = filterDrawer.findViewById<Button>(R.id.btnResetFilters)
        val btnApply = filterDrawer.findViewById<Button>(R.id.btnApplyFilters)
        val btnClose = filterDrawer.findViewById<ImageView>(R.id.btnCloseFilters)

        spinnerCategory = filterDrawer.findViewById(R.id.spinnerCategory)
        spinnerSource = filterDrawer.findViewById(R.id.spinnerSource)
        spinnerRecipient = filterDrawer.findViewById(R.id.spinnerRecipient)

        btnClose.setOnClickListener { closeFilterDrawer() }

        val typeOptions = listOf("Both", "Income", "Expense")
        spinnerType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, typeOptions).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

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
            spinnerType.setSelection(0)
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

            val type = when (spinnerType.selectedItemPosition) {
                1 -> "income"; 2 -> "expense"; else -> "both"
            }

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
                    dateFrom = dateFrom, dateTo = dateTo, type = type,
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
        filterDrawer.translationX = -filterDrawer.width.toFloat()
        filterDrawer.animate().translationX(0f).setDuration(300).start()
    }

    private fun closeFilterDrawer() {
        filterDrawer.animate().translationX(-filterDrawer.width.toFloat()).setDuration(300)
            .withEndAction { findViewById<FrameLayout>(R.id.filterDrawerContainer).visibility = View.GONE }
            .start()
    }

    private fun isFilterDrawerOpen() =
        findViewById<FrameLayout>(R.id.filterDrawerContainer).visibility == View.VISIBLE

    private fun showDeleteConfirmation(transaction: com.example.personalwealthmanager.domain.model.Transaction) {
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
                    // Refresh chart when filter changes while in chart view
                    tvChartDateRange.text = buildDateRangeText(state.filter)
                    when (currentViewMode) {
                        ViewMode.WEEKLY -> { showLineChartSection(); populateWeeklyChart(state.transactions) }
                        ViewMode.DAILY -> { showLineChartSection(); populateDailyChart(state.transactions) }
                        ViewMode.MONTHLY -> { showLineChartSection(); populateMonthlyChart(state.transactions) }
                        ViewMode.SOURCE_RECIPIENT -> { showPieChartSection(); populatePieCharts(state.transactions) }
                        ViewMode.LIST -> {}
                    }
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
