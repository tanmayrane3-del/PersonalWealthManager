package com.example.personalwealthmanager.presentation.transactions

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.personalwealthmanager.R
import com.example.personalwealthmanager.MainActivity
import com.example.personalwealthmanager.presentation.categories.CategoryManagementActivity
import com.example.personalwealthmanager.presentation.sources.SourceManagementActivity
import com.example.personalwealthmanager.presentation.recipients.RecipientManagementActivity
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

        // Load initial filters from intent
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

        fabAddTransaction.setOnClickListener {
            val intent = Intent(this, EditTransactionActivity::class.java)
            intent.putExtra("mode", "add")
            startActivity(intent)
        }

        findViewById<FrameLayout>(R.id.filterDrawerContainer).setOnClickListener {
            closeFilterDrawer()
        }

        filterDrawer.setOnClickListener {
            // Consume click to prevent closing
        }
    }

    private fun setupNavigationDrawer() {
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        val headerView = navigationView.getHeaderView(0)

        // Set user email from SharedPreferences
        val sharedPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val userEmail = sharedPrefs.getString("user_email", "user@example.com")
        headerView.findViewById<TextView>(R.id.tvUserEmail)?.text = userEmail

        // Dashboard button
        headerView.findViewById<Button>(R.id.btnDashboard)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        // Transactions button - already on this screen
        headerView.findViewById<Button>(R.id.btnTransactions)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            // Already on transactions screen, just close drawer
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

        // Logout button
        headerView.findViewById<Button>(R.id.btnLogout)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            // Clear session
            sharedPrefs.edit().clear().apply()
            // Navigate to login
            val intent = Intent(this, com.example.personalwealthmanager.presentation.auth.login.LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun setupFilterDrawer() {
        val etDateFrom = filterDrawer.findViewById<EditText>(R.id.etDateFrom)
        val etDateTo = filterDrawer.findViewById<EditText>(R.id.etDateTo)
        val spinnerType = filterDrawer.findViewById<Spinner>(R.id.spinnerType)
        val etMinAmount = filterDrawer.findViewById<EditText>(R.id.etMinAmount)
        val etMaxAmount = filterDrawer.findViewById<EditText>(R.id.etMaxAmount)
        val btnReset = filterDrawer.findViewById<Button>(R.id.btnResetFilters)
        val btnApply = filterDrawer.findViewById<Button>(R.id.btnApplyFilters)
        val btnClose = filterDrawer.findViewById<ImageView>(R.id.btnCloseFilters)

        btnClose.setOnClickListener {
            closeFilterDrawer()
        }

        // Setup Type Spinner
        val typeOptions = listOf("Both", "Income", "Expense")
        spinnerType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, typeOptions).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // Date pickers
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

        // Reset button
        btnReset.setOnClickListener {
            etDateFrom.setText("")
            etDateTo.setText("")
            spinnerType.setSelection(0)
            etMinAmount.setText("")
            etMaxAmount.setText("")
            viewModel.resetFilters()
            closeFilterDrawer()
            viewModel.loadTransactions()
        }

        // Apply button
        btnApply.setOnClickListener {
            val dateFrom = if (etDateFrom.text.isNotEmpty()) {
                try {
                    apiDateFormat.format(displayDateFormat.parse(etDateFrom.text.toString())!!)
                } catch (e: Exception) {
                    null
                }
            } else null

            val dateTo = if (etDateTo.text.isNotEmpty()) {
                try {
                    apiDateFormat.format(displayDateFormat.parse(etDateTo.text.toString())!!)
                } catch (e: Exception) {
                    null
                }
            } else null

            val type = when (spinnerType.selectedItemPosition) {
                1 -> "income"
                2 -> "expense"
                else -> "both"
            }

            val filter = FilterState(
                dateFrom = dateFrom,
                dateTo = dateTo,
                type = type,
                minAmount = etMinAmount.text.toString().ifEmpty { null },
                maxAmount = etMaxAmount.text.toString().ifEmpty { null }
            )

            viewModel.updateFilter(filter)
            closeFilterDrawer()
            viewModel.loadTransactions()
        }
    }

    private fun loadInitialFilters() {
        val filterType = intent.getStringExtra("filter_type")
        val dateFrom = intent.getStringExtra("date_from")
        val dateTo = intent.getStringExtra("date_to")

        if (filterType != null || dateFrom != null || dateTo != null) {
            val filter = FilterState(
                type = filterType ?: "both",
                dateFrom = dateFrom,
                dateTo = dateTo
            )
            viewModel.updateFilter(filter)
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

    private fun openFilterDrawer() {
        findViewById<FrameLayout>(R.id.filterDrawerContainer).visibility = View.VISIBLE
        filterDrawer.translationX = -filterDrawer.width.toFloat()
        filterDrawer.animate()
            .translationX(0f)
            .setDuration(300)
            .start()
    }

    private fun closeFilterDrawer() {
        filterDrawer.animate()
            .translationX(-filterDrawer.width.toFloat())
            .setDuration(300)
            .withEndAction {
                findViewById<FrameLayout>(R.id.filterDrawerContainer).visibility = View.GONE
            }
            .start()
    }

    private fun isFilterDrawerOpen(): Boolean {
        return findViewById<FrameLayout>(R.id.filterDrawerContainer).visibility == View.VISIBLE
    }

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

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                if (state.transactions.isEmpty() && !state.isLoading) {
                    tvEmptyState.visibility = View.VISIBLE
                    rvTransactions.visibility = View.GONE
                } else {
                    tvEmptyState.visibility = View.GONE
                    rvTransactions.visibility = View.VISIBLE
                    adapter.updateTransactions(state.transactions)
                }

                state.error?.let { error ->
                    Toast.makeText(this@TransactionsActivity, error, Toast.LENGTH_LONG).show()
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
            isFilterDrawerOpen() -> closeFilterDrawer()
            drawerLayout.isDrawerOpen(GravityCompat.END) -> drawerLayout.closeDrawer(GravityCompat.END)
            else -> super.onBackPressed()
        }
    }
}
