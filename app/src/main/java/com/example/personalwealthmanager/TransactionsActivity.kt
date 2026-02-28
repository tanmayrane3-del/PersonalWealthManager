package com.example.personalwealthmanager

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.personalwealthmanager.ApiConfig
import com.example.personalwealthmanager.presentation.categories.CategoryManagementActivity
import com.example.personalwealthmanager.presentation.sources.SourceManagementActivity
import com.example.personalwealthmanager.presentation.recipients.RecipientManagementActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class TransactionsActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_EDIT_TRANSACTION = 1001
    }

    private lateinit var rvTransactions: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var filterDrawer: LinearLayout
    private lateinit var adapter: TransactionsAdapter
    private lateinit var fabAddTransaction: com.google.android.material.floatingactionbutton.FloatingActionButton

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    // Filter variables
    private var filterDateFrom: String? = null
    private var filterDateTo: String? = null
    private var filterType: String = "both"
    private var filterCategoryId: String? = null
    private var filterSourceId: String? = null
    private var filterRecipientId: String? = null
    private var filterMinAmount: String? = null
    private var filterMaxAmount: String? = null

    // Filter data
    private val incomeCategories = mutableListOf<Pair<String, String>>()
    private val expenseCategories = mutableListOf<Pair<String, String>>()
    private val sources = mutableListOf<Pair<String, String>>()
    private val recipients = mutableListOf<Pair<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transactions)

        initializeViews()
        setupDrawerMenu()
        setupFilterDrawer()
        setupRecyclerView()
        setupClickListeners()

        // Load filter data first, then transactions
        loadFilterData()
    }

    private fun initializeViews() {
        rvTransactions = findViewById(R.id.rvTransactions)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        progressBar = findViewById(R.id.progressBar)
        drawerLayout = findViewById(R.id.drawerLayout)
        filterDrawer = findViewById(R.id.filterDrawer)
        fabAddTransaction = findViewById(R.id.fabAddTransaction)
    }

    private fun setupRecyclerView() {
        adapter = TransactionsAdapter(emptyList()) { transaction ->
            // Open edit screen
            val intent = Intent(this, EditTransactionActivity::class.java)
            intent.putExtra("transaction_id", transaction.transactionId)
            intent.putExtra("is_income", transaction.transactionType == "income")
            startActivityForResult(intent, REQUEST_EDIT_TRANSACTION)
        }
        rvTransactions.layoutManager = LinearLayoutManager(this)
        rvTransactions.adapter = adapter
    }

    private fun setupClickListeners() {
        findViewById<ImageView>(R.id.btnMenu).setOnClickListener {
            if (isFilterDrawerOpen()) {
                closeFilterDrawer()
            }
            drawerLayout.openDrawer(GravityCompat.END)
        }

        findViewById<Button>(R.id.btnFilters).setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawer(GravityCompat.END)
            }
            openFilterDrawer()
        }

        // FAB click listener
        fabAddTransaction.setOnClickListener {
            showAddTransactionDialog()
        }

        // Close filter drawer when clicking outside
        val filterContainer = findViewById<FrameLayout>(R.id.filterDrawerContainer)
        filterContainer.setOnClickListener {
            closeFilterDrawer()
        }

        // Prevent clicks from passing through to the filter container
        filterDrawer.setOnClickListener {
            // Do nothing - just consume the click
        }
    }

    private fun setupDrawerMenu() {
        val navigationView = findViewById<com.google.android.material.navigation.NavigationView>(R.id.navigationView)
        val headerView = navigationView.getHeaderView(0)

        val tvUserEmail = headerView.findViewById<TextView>(R.id.tvUserEmail)
        val userEmail = getUserEmail()
        tvUserEmail.text = userEmail ?: "User"

        val btnDashboard = headerView.findViewById<Button>(R.id.btnDashboard)
        btnDashboard.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            // Navigate to Dashboard (MainActivity)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        val btnTransactions = headerView.findViewById<Button>(R.id.btnTransactions)
        btnTransactions.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        // Management expandable menu
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

        ivManagementExpand?.setOnClickListener {
            btnManagement?.performClick()
        }

        // Category management button
        headerView.findViewById<Button>(R.id.btnCategoryManagement)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, CategoryManagementActivity::class.java))
        }

        // Source management button
        headerView.findViewById<Button>(R.id.btnSourceManagement)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, SourceManagementActivity::class.java))
        }

        // Recipient management button
        headerView.findViewById<Button>(R.id.btnRecipientManagement)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, RecipientManagementActivity::class.java))
        }

        val btnLogout = headerView.findViewById<Button>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            logout()
        }
    }

    private fun setupFilterDrawer() {
        val etDateFrom = filterDrawer.findViewById<EditText>(R.id.etDateFrom)
        val etDateTo = filterDrawer.findViewById<EditText>(R.id.etDateTo)
        val spinnerType = filterDrawer.findViewById<Spinner>(R.id.spinnerType)
        val spinnerCategory = filterDrawer.findViewById<Spinner>(R.id.spinnerCategory)
        val spinnerSource = filterDrawer.findViewById<Spinner>(R.id.spinnerSource)
        val spinnerRecipient = filterDrawer.findViewById<Spinner>(R.id.spinnerRecipient)
        val etMinAmount = filterDrawer.findViewById<EditText>(R.id.etMinAmount)
        val etMaxAmount = filterDrawer.findViewById<EditText>(R.id.etMaxAmount)
        val btnReset = filterDrawer.findViewById<Button>(R.id.btnResetFilters)
        val btnApply = filterDrawer.findViewById<Button>(R.id.btnApplyFilters)
        val btnClose = filterDrawer.findViewById<ImageView>(R.id.btnCloseFilters)

        // Close button
        btnClose.setOnClickListener {
            closeFilterDrawer()
        }

        // Setup Type Spinner
        val typeOptions = listOf("Both", "Income", "Expense")
        spinnerType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, typeOptions).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // Date picker listeners
        etDateFrom.setOnClickListener {
            showDatePicker { date ->
                etDateFrom.setText(displayDateFormat.format(date))
            }
        }

        etDateTo.setOnClickListener {
            showDatePicker { date ->
                etDateTo.setText(displayDateFormat.format(date))
            }
        }

        // Type spinner listener
        spinnerType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedType = when (position) {
                    1 -> "income"
                    2 -> "expense"
                    else -> "both"
                }
                updateCategorySpinner(spinnerCategory, selectedType)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Reset button
        btnReset.setOnClickListener {
            resetFilters()
            etDateFrom.setText("")
            etDateTo.setText("")
            spinnerType.setSelection(0)
            etMinAmount.setText("")
            etMaxAmount.setText("")
            closeFilterDrawer()
            loadTransactions()
        }

        // Apply button
        btnApply.setOnClickListener {
            applyFilters(etDateFrom, etDateTo, spinnerType, spinnerCategory, spinnerSource, spinnerRecipient, etMinAmount, etMaxAmount)
            closeFilterDrawer()
            loadTransactions()
        }
    }

    private fun openFilterDrawer() {
        val filterContainer = findViewById<FrameLayout>(R.id.filterDrawerContainer)
        filterContainer.visibility = View.VISIBLE

        // Animate slide in from LEFT
        filterDrawer.translationX = -filterDrawer.width.toFloat()  // Changed to negative
        filterDrawer.animate()
            .translationX(0f)
            .setDuration(300)
            .start()
    }

    private fun closeFilterDrawer() {
        val filterContainer = findViewById<FrameLayout>(R.id.filterDrawerContainer)

        // Animate slide out to LEFT
        filterDrawer.animate()
            .translationX(-filterDrawer.width.toFloat())  // Changed to negative
            .setDuration(300)
            .withEndAction {
                filterContainer.visibility = View.GONE
            }
            .start()
    }

    private fun isFilterDrawerOpen(): Boolean {
        val filterContainer = findViewById<FrameLayout>(R.id.filterDrawerContainer)
        return filterContainer.visibility == View.VISIBLE
    }

    private fun resetFilters() {
        filterDateFrom = null
        filterDateTo = null
        filterType = "both"
        filterCategoryId = null
        filterSourceId = null
        filterRecipientId = null
        filterMinAmount = null
        filterMaxAmount = null
    }

    private fun applyFilters(
        etDateFrom: EditText,
        etDateTo: EditText,
        spinnerType: Spinner,
        spinnerCategory: Spinner,
        spinnerSource: Spinner,
        spinnerRecipient: Spinner,
        etMinAmount: EditText,
        etMaxAmount: EditText
    ) {
        filterDateFrom = if (etDateFrom.text.toString().isNotEmpty()) {
            try {
                dateFormat.format(displayDateFormat.parse(etDateFrom.text.toString())!!)
            } catch (e: Exception) {
                null
            }
        } else null

        filterDateTo = if (etDateTo.text.toString().isNotEmpty()) {
            try {
                dateFormat.format(displayDateFormat.parse(etDateTo.text.toString())!!)
            } catch (e: Exception) {
                null
            }
        } else null

        filterType = when (spinnerType.selectedItemPosition) {
            1 -> "income"
            2 -> "expense"
            else -> "both"
        }

        val categories = if (filterType == "income") incomeCategories else expenseCategories
        filterCategoryId = if (spinnerCategory.selectedItemPosition > 0) {
            categories[spinnerCategory.selectedItemPosition].first
        } else null

        filterSourceId = if (spinnerSource.selectedItemPosition > 0) {
            sources[spinnerSource.selectedItemPosition].first
        } else null

        filterRecipientId = if (spinnerRecipient.selectedItemPosition > 0) {
            recipients[spinnerRecipient.selectedItemPosition].first
        } else null

        filterMinAmount = etMinAmount.text.toString().ifEmpty { null }
        filterMaxAmount = etMaxAmount.text.toString().ifEmpty { null }
    }

    private fun updateCategorySpinner(spinner: Spinner, type: String) {
        val categories = if (type == "income") incomeCategories else expenseCategories
        val categoryNames = categories.map { it.second }
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryNames).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun showDatePicker(onDateSelected: (Date) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day)
                onDateSelected(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun loadFilterData() {
        val sessionToken = getSessionToken()
        if (sessionToken == null) {
            navigateToLogin()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                loadIncomeCategories(sessionToken)
                loadExpenseCategories(sessionToken)
                loadSources(sessionToken)
                loadRecipients(sessionToken)

                withContext(Dispatchers.Main) {
                    // Initialize spinners after data is loaded
                    initializeSpinners()
                    loadTransactions()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@TransactionsActivity,
                        "Failed to load filter data: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadTransactions()
                }
            }
        }
    }

    private fun initializeSpinners() {
        val spinnerCategory = filterDrawer.findViewById<Spinner>(R.id.spinnerCategory)
        val spinnerSource = filterDrawer.findViewById<Spinner>(R.id.spinnerSource)
        val spinnerRecipient = filterDrawer.findViewById<Spinner>(R.id.spinnerRecipient)

        updateCategorySpinner(spinnerCategory, "both")

        val sourceNames = sources.map { it.second }
        spinnerSource.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sourceNames).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        val recipientNames = recipients.map { it.second }
        spinnerRecipient.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, recipientNames).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun loadIncomeCategories(sessionToken: String) {
        val url = URL(ApiConfig.BASE_URL + ApiConfig.Endpoints.GET_INCOME_CATEGORIES)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty(ApiConfig.HEADER_SESSION_TOKEN, sessionToken)

        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            val response = connection.inputStream.bufferedReader().readText()
            val jsonResponse = JSONObject(response)
            val dataArray = jsonResponse.getJSONArray("data")

            incomeCategories.clear()
            incomeCategories.add(Pair("", "All Categories"))
            for (i in 0 until dataArray.length()) {
                val item = dataArray.getJSONObject(i)
                incomeCategories.add(Pair(item.getString("id"), item.getString("name")))
            }
        }
    }

    private fun loadExpenseCategories(sessionToken: String) {
        val url = URL(ApiConfig.BASE_URL + ApiConfig.Endpoints.GET_EXPENSE_CATEGORIES)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty(ApiConfig.HEADER_SESSION_TOKEN, sessionToken)

        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            val response = connection.inputStream.bufferedReader().readText()
            val jsonResponse = JSONObject(response)
            val dataArray = jsonResponse.getJSONArray("data")

            expenseCategories.clear()
            expenseCategories.add(Pair("", "All Categories"))
            for (i in 0 until dataArray.length()) {
                val item = dataArray.getJSONObject(i)
                expenseCategories.add(Pair(item.getString("id"), item.getString("name")))
            }
        }
    }

    private fun loadSources(sessionToken: String) {
        val url = URL(ApiConfig.BASE_URL + ApiConfig.Endpoints.GET_INCOME_SOURCES)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty(ApiConfig.HEADER_SESSION_TOKEN, sessionToken)

        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            val response = connection.inputStream.bufferedReader().readText()
            val jsonResponse = JSONObject(response)
            val dataArray = jsonResponse.getJSONArray("data")

            sources.clear()
            sources.add(Pair("", "All Sources"))
            for (i in 0 until dataArray.length()) {
                val item = dataArray.getJSONObject(i)
                sources.add(Pair(item.getString("id"), item.getString("name")))
            }
        }
    }

    private fun loadRecipients(sessionToken: String) {
        val url = URL(ApiConfig.BASE_URL + ApiConfig.Endpoints.GET_RECIPIENTS)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty(ApiConfig.HEADER_SESSION_TOKEN, sessionToken)

        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            val response = connection.inputStream.bufferedReader().readText()
            val jsonResponse = JSONObject(response)
            val dataArray = jsonResponse.getJSONArray("data")

            recipients.clear()
            recipients.add(Pair("", "All Recipients"))
            for (i in 0 until dataArray.length()) {
                val item = dataArray.getJSONObject(i)
                recipients.add(Pair(item.getString("id"), item.getString("name")))
            }
        }
    }

    private fun loadTransactions() {
        val sessionToken = getSessionToken()
        if (sessionToken == null) {
            navigateToLogin()
            return
        }

        showLoading()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val urlBuilder = StringBuilder(ApiConfig.BASE_URL + ApiConfig.Endpoints.GET_TRANSACTIONS)
                urlBuilder.append("?type=$filterType")

                filterDateFrom?.let { urlBuilder.append("&start_date=$it") }
                filterDateTo?.let { urlBuilder.append("&end_date=$it") }
                filterCategoryId?.let { if (it.isNotEmpty()) urlBuilder.append("&category_id=$it") }
                filterSourceId?.let { if (it.isNotEmpty()) urlBuilder.append("&source_id=$it") }
                filterRecipientId?.let { if (it.isNotEmpty()) urlBuilder.append("&recipient_id=$it") }
                filterMinAmount?.let { if (it.isNotEmpty()) urlBuilder.append("&min_amount=$it") }
                filterMaxAmount?.let { if (it.isNotEmpty()) urlBuilder.append("&max_amount=$it") }

                val url = URL(urlBuilder.toString())
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty(ApiConfig.HEADER_SESSION_TOKEN, sessionToken)
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                val response = if (responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.bufferedReader().readText()
                } else {
                    connection.errorStream?.bufferedReader()?.readText() ?: ""
                }

                withContext(Dispatchers.Main) {
                    hideLoading()

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val jsonResponse = JSONObject(response)
                        if (jsonResponse.getString("status") == "success") {
                            val dataArray = jsonResponse.getJSONArray("data")
                            val transactions = mutableListOf<Transaction>()

                            for (i in 0 until dataArray.length()) {
                                val item = dataArray.getJSONObject(i)
                                transactions.add(
                                    Transaction(
                                        transactionId = item.getString("transaction_id"),
                                        transactionType = item.getString("transaction_type"),
                                        date = item.getString("date"),
                                        time = item.getString("time"),
                                        amount = item.getDouble("amount"),
                                        currency = item.getString("currency"),
                                        categoryName = item.optString("category_name"),
                                        categoryIcon = item.optString("category_icon"),
                                        categoryColor = item.optString("category_color"),
                                        sourceName = item.optString("source_name"),
                                        recipientName = item.optString("recipient_name"),
                                        paymentMethod = item.optString("payment_method"),
                                        notes = item.optString("notes"),
                                        tags = null
                                    )
                                )
                            }

                            if (transactions.isEmpty()) {
                                showEmptyState()
                            } else {
                                showTransactions(transactions)
                            }
                        } else {
                            showEmptyState()
                        }
                    } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                        Toast.makeText(
                            this@TransactionsActivity,
                            getString(R.string.session_expired),
                            Toast.LENGTH_SHORT
                        ).show()
                        navigateToLogin()
                    } else {
                        Toast.makeText(
                            this@TransactionsActivity,
                            getString(R.string.refresh_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                        showEmptyState()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hideLoading()
                    Toast.makeText(
                        this@TransactionsActivity,
                        getString(R.string.network_error, e.message),
                        Toast.LENGTH_SHORT
                    ).show()
                    showEmptyState()
                }
            }
        }
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        rvTransactions.visibility = View.GONE
        tvEmptyState.visibility = View.GONE
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
    }

    private fun showTransactions(transactions: List<Transaction>) {
        rvTransactions.visibility = View.VISIBLE
        tvEmptyState.visibility = View.GONE
        adapter.updateTransactions(transactions)
    }

    private fun showEmptyState() {
        rvTransactions.visibility = View.GONE
        tvEmptyState.visibility = View.VISIBLE
    }

    private fun logout() {
        val sessionToken = getSessionToken()
        if (sessionToken == null) {
            navigateToLogin()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(ApiConfig.BASE_URL + ApiConfig.Endpoints.LOGOUT)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("X-Session-Token", sessionToken)

                withContext(Dispatchers.Main) {
                    clearSession()
                    Toast.makeText(
                        this@TransactionsActivity,
                        getString(R.string.logout_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    navigateToLogin()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    clearSession()
                    navigateToLogin()
                }
            }
        }
    }

    private fun clearSession() {
        getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).edit {
            remove("session_token")
            remove("user_email")
        }
    }

    private fun getSessionToken(): String? {
        return getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            .getString("session_token", null)
    }

    private fun getUserEmail(): String? {
        return getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            .getString("user_email", null)
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showAddTransactionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_transaction, null)
        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Initialize form fields
        val btnCloseDialog = dialogView.findViewById<ImageView>(R.id.btnCloseDialog)
        val actvType = dialogView.findViewById<AutoCompleteTextView>(R.id.actvTransactionType)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val actvCategory = dialogView.findViewById<AutoCompleteTextView>(R.id.actvCategory)
        val actvSourceRecipient = dialogView.findViewById<AutoCompleteTextView>(R.id.actvSourceRecipient)
        val tilSourceRecipient = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilSourceRecipient)
        val tvSourceRecipientLabel = dialogView.findViewById<TextView>(R.id.tvSourceRecipientLabel)
        val tvSelectedDate = dialogView.findViewById<TextView>(R.id.tvSelectedDate)
        val tvSelectedTime = dialogView.findViewById<TextView>(R.id.tvSelectedTime)
        val datePickerLayout = dialogView.findViewById<LinearLayout>(R.id.datePickerLayout)
        val timePickerLayout = dialogView.findViewById<LinearLayout>(R.id.timePickerLayout)
        val actvPaymentMethod = dialogView.findViewById<AutoCompleteTextView>(R.id.actvPaymentMethod)
        val etTransactionReference = dialogView.findViewById<EditText>(R.id.etTransactionReference)
        val etTags = dialogView.findViewById<EditText>(R.id.etTags)
        val etNotes = dialogView.findViewById<EditText>(R.id.etNotes)
        val btnClear = dialogView.findViewById<Button>(R.id.btnClear)
        val btnAdd = dialogView.findViewById<Button>(R.id.btnAdd)

        // Close button listener
        btnCloseDialog.setOnClickListener {
            dialog.dismiss()
        }

        // Variables to store selected values
        var selectedDate: Date? = null
        var selectedTime: String? = null
        val selectedDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        // Setup Transaction Type AutoCompleteTextView
        val typeOptions = arrayOf("Income", "Expense")
        val typeAdapter = ArrayAdapter(this, R.layout.spinner_dropdown_item, typeOptions)
        actvType.setAdapter(typeAdapter)
        actvType.setText(typeOptions[0], false)  // Default to Income

        var selectedTypeIndex = 0

        // Payment Method AutoCompleteTextView
        val paymentMethods = arrayOf(
            "Cash", "Bank Transfer", "Credit Card", "Debit Card",
            "UPI", "Wallet", "Cheque", "Other"
        )
        val paymentAdapter = ArrayAdapter(this, R.layout.spinner_dropdown_item, paymentMethods)
        actvPaymentMethod.setAdapter(paymentAdapter)

        // Transaction type change listener
        actvType.setOnItemClickListener { parent, view, position, id ->
            selectedTypeIndex = position
            val isIncome = position == 0
            tvSourceRecipientLabel.text = if (isIncome) "Source *" else "Recipient *"
            tilSourceRecipient.hint = if (isIncome) "Select Source" else "Select Recipient"

            // Update category dropdown
            val categories = if (isIncome) incomeCategories else expenseCategories
            val categoryNames = categories.map { it.second }.toTypedArray()
            val categoryAdapter = ArrayAdapter(this, R.layout.spinner_dropdown_item, categoryNames)
            actvCategory.setAdapter(categoryAdapter)
            actvCategory.text.clear()

            // Update source/recipient dropdown
            val sourceRecipient = if (isIncome) sources else recipients
            val names = sourceRecipient.map { it.second }.toTypedArray()
            val srAdapter = ArrayAdapter(this, R.layout.spinner_dropdown_item, names)
            actvSourceRecipient.setAdapter(srAdapter)
            actvSourceRecipient.text.clear()
        }

        // Trigger initial setup for Income
        actvType.post {
            val categories = incomeCategories.map { it.second }.toTypedArray()
            val categoryAdapter = ArrayAdapter(this, R.layout.spinner_dropdown_item, categories)
            actvCategory.setAdapter(categoryAdapter)

            val sourceNames = sources.map { it.second }.toTypedArray()
            val sourceAdapter = ArrayAdapter(this, R.layout.spinner_dropdown_item, sourceNames)
            actvSourceRecipient.setAdapter(sourceAdapter)
        }

        // Date picker
        datePickerLayout.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    selectedDate = calendar.time
                    tvSelectedDate.text = displayDateFormat.format(selectedDate!!)
                    tvSelectedDate.setTextColor(resources.getColor(R.color.text_dark, null))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Time picker
        timePickerLayout.setOnClickListener {
            val calendar = Calendar.getInstance()
            android.app.TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    calendar.set(Calendar.MINUTE, minute)
                    calendar.set(Calendar.SECOND, 0)
                    selectedTime = timeFormat.format(calendar.time)

                    // Format time in 12-hour format with AM/PM
                    val hour12 = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
                    val amPm = if (hourOfDay < 12) "AM" else "PM"
                    tvSelectedTime.text = String.format("%02d:%02d %s", hour12, minute, amPm)
                    tvSelectedTime.setTextColor(resources.getColor(R.color.text_dark, null))
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false  // false = 12-hour format
            ).show()
        }

        // Clear button
        btnClear.setOnClickListener {
            etAmount.setText("")
            actvType.setText(typeOptions[0], false)
            selectedTypeIndex = 0
            actvCategory.text.clear()
            actvSourceRecipient.text.clear()
            actvPaymentMethod.text.clear()
            etTransactionReference.setText("")
            etTags.setText("")
            etNotes.setText("")
            selectedDate = null
            selectedTime = null
            tvSelectedDate.text = "Select Date"
            tvSelectedDate.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            tvSelectedTime.text = "Select Time"
            tvSelectedTime.setTextColor(resources.getColor(android.R.color.darker_gray, null))
        }

        // Add button
        btnAdd.setOnClickListener {
            // Validate inputs
            val amount = etAmount.text.toString().trim()
            val categoryText = actvCategory.text.toString().trim()
            val sourceRecipientText = actvSourceRecipient.text.toString().trim()
            val paymentMethodText = actvPaymentMethod.text.toString().trim()
            val transactionReference = etTransactionReference.text.toString().trim()
            val tagsInput = etTags.text.toString().trim()
            val notes = etNotes.text.toString().trim()

            when {
                amount.isEmpty() -> {
                    Toast.makeText(this, "Please enter amount", Toast.LENGTH_SHORT).show()
                }
                categoryText.isEmpty() -> {
                    Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
                }
                sourceRecipientText.isEmpty() -> {
                    val label = if (selectedTypeIndex == 0) "source" else "recipient"
                    Toast.makeText(this, "Please select a $label", Toast.LENGTH_SHORT).show()
                }
                paymentMethodText.isEmpty() -> {
                    Toast.makeText(this, "Please select a payment method", Toast.LENGTH_SHORT).show()
                }
                selectedDate == null -> {
                    Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
                }
                selectedTime == null -> {
                    Toast.makeText(this, "Please select a time", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    val isIncome = selectedTypeIndex == 0
                    val categories = if (isIncome) incomeCategories else expenseCategories
                    val sourceRecipient = if (isIncome) sources else recipients

                    // Find category ID by name
                    val categoryId = categories.find { it.second == categoryText }?.first
                    val sourceRecipientId = sourceRecipient.find { it.second == sourceRecipientText }?.first

                    if (categoryId == null || sourceRecipientId == null) {
                        Toast.makeText(this, "Invalid selection. Please try again.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    val paymentMethod = paymentMethodText.lowercase().replace(" ", "_")

                    // Submit transaction
                    submitTransaction(
                        isIncome = isIncome,
                        amount = amount,
                        categoryId = categoryId,
                        sourceRecipientId = sourceRecipientId,
                        date = selectedDateFormat.format(selectedDate!!),
                        time = selectedTime!!,
                        paymentMethod = paymentMethod,
                        transactionReference = transactionReference,
                        tags = tagsInput,
                        notes = notes,
                        dialog = dialog
                    )
                }
            }
        }

        dialog.show()
    }

    private fun submitTransaction(
        isIncome: Boolean,
        amount: String,
        categoryId: String,
        sourceRecipientId: String,
        date: String,
        time: String,
        paymentMethod: String,
        transactionReference: String,
        tags: String,
        notes: String,
        dialog: android.app.AlertDialog
    ) {
        val sessionToken = getSessionToken()
        if (sessionToken == null) {
            navigateToLogin()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val endpoint = if (isIncome) {
                    ApiConfig.Endpoints.ADD_INCOME
                } else {
                    ApiConfig.Endpoints.ADD_EXPENSE
                }

                val url = URL(ApiConfig.BASE_URL + endpoint)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty(ApiConfig.HEADER_SESSION_TOKEN, sessionToken)
                connection.setRequestProperty(ApiConfig.HEADER_CONTENT_TYPE, ApiConfig.CONTENT_TYPE_JSON)
                connection.doOutput = true
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                // Convert tags string to JSON array
                val tagsArray = if (tags.isNotEmpty()) {
                    org.json.JSONArray(tags.split(",").map { it.trim() }.filter { it.isNotEmpty() })
                } else {
                    org.json.JSONArray()
                }

                // Build JSON payload
                val jsonPayload = if (isIncome) {
                    JSONObject().apply {
                        put("amount", amount.toDouble())
                        put("currency", "INR")
                        put("category_id", categoryId)
                        put("source_id", sourceRecipientId)
                        put("date", date)
                        put("time", time)
                        put("payment_method", paymentMethod)
                        if (transactionReference.isNotEmpty()) put("transaction_reference", transactionReference)
                        if (tagsArray.length() > 0) put("tags", tagsArray)
                        if (notes.isNotEmpty()) put("notes", notes)
                    }
                } else {
                    JSONObject().apply {
                        put("amount", amount.toDouble())
                        put("currency", "INR")
                        put("category_id", categoryId)
                        put("recipient_id", sourceRecipientId)
                        put("date", date)
                        put("time", time)
                        put("payment_method", paymentMethod)
                        if (transactionReference.isNotEmpty()) put("transaction_reference", transactionReference)
                        if (tagsArray.length() > 0) put("tags", tagsArray)
                        if (notes.isNotEmpty()) put("notes", notes)
                    }
                }

                connection.outputStream.write(jsonPayload.toString().toByteArray())

                val responseCode = connection.responseCode
                val response = if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                    connection.inputStream.bufferedReader().readText()
                } else {
                    connection.errorStream?.bufferedReader()?.readText() ?: ""
                }

                withContext(Dispatchers.Main) {
                    if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                        val jsonResponse = JSONObject(response)
                        if (jsonResponse.getString("status") == "success") {
                            Toast.makeText(
                                this@TransactionsActivity,
                                "Transaction added successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                            dialog.dismiss()
                            // Refresh transactions list
                            loadTransactions()
                        } else {
                            val reason = jsonResponse.optString("reason", "Failed to add transaction")
                            Toast.makeText(this@TransactionsActivity, reason, Toast.LENGTH_SHORT).show()
                        }
                    } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                        Toast.makeText(
                            this@TransactionsActivity,
                            "Session expired. Please login again",
                            Toast.LENGTH_SHORT
                        ).show()
                        navigateToLogin()
                    } else {
                        Toast.makeText(
                            this@TransactionsActivity,
                            "Failed to add transaction",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@TransactionsActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_EDIT_TRANSACTION && resultCode == RESULT_OK) {
            // Refresh transactions after edit/delete
            loadTransactions()
        }
    }

    override fun onBackPressed() {
        when {
            isFilterDrawerOpen() -> closeFilterDrawer()
            drawerLayout.isDrawerOpen(GravityCompat.END) -> drawerLayout.closeDrawer(GravityCompat.END)
            else -> super.onBackPressed()
        }
    }
}