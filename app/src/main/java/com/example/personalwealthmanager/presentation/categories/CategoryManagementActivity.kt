package com.example.personalwealthmanager.presentation.categories

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.Window
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
import com.example.personalwealthmanager.domain.model.Category
import com.example.personalwealthmanager.presentation.recipients.RecipientManagementActivity
import com.example.personalwealthmanager.presentation.sources.SourceManagementActivity
import com.example.personalwealthmanager.presentation.transactions.TransactionsActivity
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CategoryManagementActivity : AppCompatActivity() {

    private val viewModel: CategoryManagementViewModel by viewModels()

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var progressBar: ProgressBar

    // Income section views
    private lateinit var incomeHeader: LinearLayout
    private lateinit var incomeContent: LinearLayout
    private lateinit var ivIncomeExpand: ImageView
    private lateinit var rvIncomeGlobalCategories: RecyclerView
    private lateinit var rvIncomeUserCategories: RecyclerView
    private lateinit var tvNoIncomeUserCategories: TextView

    // Expense section views
    private lateinit var expenseHeader: LinearLayout
    private lateinit var expenseContent: LinearLayout
    private lateinit var ivExpenseExpand: ImageView
    private lateinit var rvExpenseGlobalCategories: RecyclerView
    private lateinit var rvExpenseUserCategories: RecyclerView
    private lateinit var tvNoExpenseUserCategories: TextView

    // Adapters
    private lateinit var incomeGlobalAdapter: CategoryAdapter
    private lateinit var incomeUserAdapter: CategoryAdapter
    private lateinit var expenseGlobalAdapter: CategoryAdapter
    private lateinit var expenseUserAdapter: CategoryAdapter

    // Management menu state
    private var isManagementExpanded = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_management)

        initializeViews()
        setupRecyclerViews()
        setupClickListeners()
        setupNavigationDrawer()
        observeState()
    }

    private fun initializeViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        progressBar = findViewById(R.id.progressBar)

        // Income section
        incomeHeader = findViewById(R.id.incomeHeader)
        incomeContent = findViewById(R.id.incomeContent)
        ivIncomeExpand = findViewById(R.id.ivIncomeExpand)
        rvIncomeGlobalCategories = findViewById(R.id.rvIncomeGlobalCategories)
        rvIncomeUserCategories = findViewById(R.id.rvIncomeUserCategories)
        tvNoIncomeUserCategories = findViewById(R.id.tvNoIncomeUserCategories)

        // Expense section
        expenseHeader = findViewById(R.id.expenseHeader)
        expenseContent = findViewById(R.id.expenseContent)
        ivExpenseExpand = findViewById(R.id.ivExpenseExpand)
        rvExpenseGlobalCategories = findViewById(R.id.rvExpenseGlobalCategories)
        rvExpenseUserCategories = findViewById(R.id.rvExpenseUserCategories)
        tvNoExpenseUserCategories = findViewById(R.id.tvNoExpenseUserCategories)
    }

    private fun setupRecyclerViews() {
        // Income Global Categories - no edit button
        incomeGlobalAdapter = CategoryAdapter(
            categories = emptyList(),
            showEditButton = false
        )
        rvIncomeGlobalCategories.layoutManager = LinearLayoutManager(this)
        rvIncomeGlobalCategories.adapter = incomeGlobalAdapter

        // Income User Categories - with edit button
        incomeUserAdapter = CategoryAdapter(
            categories = emptyList(),
            showEditButton = true,
            onEditClick = { category ->
                showEditCategoryDialog(category, "income")
            }
        )
        rvIncomeUserCategories.layoutManager = LinearLayoutManager(this)
        rvIncomeUserCategories.adapter = incomeUserAdapter

        // Expense Global Categories - no edit button
        expenseGlobalAdapter = CategoryAdapter(
            categories = emptyList(),
            showEditButton = false
        )
        rvExpenseGlobalCategories.layoutManager = LinearLayoutManager(this)
        rvExpenseGlobalCategories.adapter = expenseGlobalAdapter

        // Expense User Categories - with edit button
        expenseUserAdapter = CategoryAdapter(
            categories = emptyList(),
            showEditButton = true,
            onEditClick = { category ->
                showEditCategoryDialog(category, "expense")
            }
        )
        rvExpenseUserCategories.layoutManager = LinearLayoutManager(this)
        rvExpenseUserCategories.adapter = expenseUserAdapter
    }

    private fun setupClickListeners() {
        // Back button
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Menu button
        findViewById<ImageView>(R.id.btnMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        // Income section collapse/expand
        incomeHeader.setOnClickListener {
            viewModel.toggleIncomeSection()
        }

        // Expense section collapse/expand
        expenseHeader.setOnClickListener {
            viewModel.toggleExpenseSection()
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

        // Transactions button
        headerView.findViewById<Button>(R.id.btnTransactions)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            val intent = Intent(this, TransactionsActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Management expand/collapse
        val btnManagement = headerView.findViewById<Button>(R.id.btnManagement)
        val ivManagementExpand = headerView.findViewById<ImageView>(R.id.ivManagementExpand)
        val managementChildItems = headerView.findViewById<LinearLayout>(R.id.managementChildItems)

        // Set initial state - expanded since we're on the category management screen
        managementChildItems?.visibility = View.VISIBLE
        ivManagementExpand?.setImageResource(R.drawable.ic_expand_less)

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

        // Category management button - already on this screen
        headerView.findViewById<Button>(R.id.btnCategoryManagement)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            // Already on category management screen
        }

        // Source management button
        headerView.findViewById<Button>(R.id.btnSourceManagement)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            val intent = Intent(this, SourceManagementActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Recipient management button
        headerView.findViewById<Button>(R.id.btnRecipientManagement)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            val intent = Intent(this, RecipientManagementActivity::class.java)
            startActivity(intent)
            finish()
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

    private fun showEditCategoryDialog(category: Category, type: String) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_edit_category)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val etCategoryName = dialog.findViewById<TextInputEditText>(R.id.etCategoryName)
        val etCategoryDescription = dialog.findViewById<TextInputEditText>(R.id.etCategoryDescription)
        val etIcon = dialog.findViewById<TextInputEditText>(R.id.etIcon)
        val tvTransactionWarning = dialog.findViewById<TextView>(R.id.tvTransactionWarning)
        val progressBar = dialog.findViewById<ProgressBar>(R.id.progressBar)
        val tvError = dialog.findViewById<TextView>(R.id.tvError)
        val buttonsLayout = dialog.findViewById<LinearLayout>(R.id.buttonsLayout)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialog.findViewById<Button>(R.id.btnSave)
        val btnDelete = dialog.findViewById<Button>(R.id.btnDelete)
        val btnCloseDialog = dialog.findViewById<ImageView>(R.id.btnCloseDialog)

        // Pre-populate fields
        etCategoryName.setText(category.name)
        etCategoryDescription.setText(category.description ?: "")
        etIcon.setText(category.icon ?: "")

        // Show transaction warning or delete button based on transaction count
        if (category.transactionCount > 0) {
            tvTransactionWarning.text = getString(R.string.category_has_transactions, category.transactionCount)
            tvTransactionWarning.visibility = View.VISIBLE
            btnDelete.visibility = View.GONE
        } else {
            tvTransactionWarning.visibility = View.GONE
            btnDelete.visibility = View.VISIBLE
        }

        btnCloseDialog.setOnClickListener {
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val name = etCategoryName.text.toString().trim()
            val description = etCategoryDescription.text.toString().trim().ifEmpty { null }
            val icon = etIcon.text.toString().trim().ifEmpty { null }

            if (name.isEmpty()) {
                tvError.text = getString(R.string.name_required)
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            // Validate icon is emoji
            if (icon != null && !isValidEmoji(icon)) {
                tvError.text = "Please enter a valid emoji icon"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            tvError.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
            buttonsLayout.visibility = View.GONE
            btnDelete.visibility = View.GONE

            viewModel.updateCategory(type, category.id, name, description, icon)

            // Observe for result
            lifecycleScope.launch {
                viewModel.state.collect { state ->
                    if (state.updateSuccess) {
                        viewModel.clearSuccessStates()
                        dialog.dismiss()
                        Toast.makeText(this@CategoryManagementActivity, R.string.category_updated, Toast.LENGTH_SHORT).show()
                        return@collect
                    }
                    if (!state.isUpdating && state.error != null) {
                        progressBar.visibility = View.GONE
                        buttonsLayout.visibility = View.VISIBLE
                        if (category.transactionCount == 0) {
                            btnDelete.visibility = View.VISIBLE
                        }
                        tvError.text = state.error
                        tvError.visibility = View.VISIBLE
                        viewModel.clearError()
                    }
                }
            }
        }

        btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.delete_category)
                .setMessage(R.string.delete_category_confirm)
                .setPositiveButton(R.string.delete) { _, _ ->
                    progressBar.visibility = View.VISIBLE
                    buttonsLayout.visibility = View.GONE
                    btnDelete.visibility = View.GONE

                    viewModel.deleteCategory(type, category.id)

                    lifecycleScope.launch {
                        viewModel.state.collect { state ->
                            if (state.deleteSuccess) {
                                viewModel.clearSuccessStates()
                                dialog.dismiss()
                                Toast.makeText(this@CategoryManagementActivity, R.string.category_deleted, Toast.LENGTH_SHORT).show()
                                return@collect
                            }
                            if (!state.isDeleting && state.error != null) {
                                progressBar.visibility = View.GONE
                                buttonsLayout.visibility = View.VISIBLE
                                btnDelete.visibility = View.VISIBLE
                                tvError.text = state.error
                                tvError.visibility = View.VISIBLE
                                viewModel.clearError()
                            }
                        }
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        dialog.show()
    }

    private fun isValidEmoji(text: String): Boolean {
        if (text.isEmpty()) return false

        // Check if string contains at least one emoji
        var i = 0
        while (i < text.length) {
            val codePoint = text.codePointAt(i)
            // Check for common emoji ranges
            if (codePoint in 0x1F300..0x1F9FF || // Miscellaneous Symbols and Pictographs, Emoticons, etc.
                codePoint in 0x2600..0x26FF ||   // Miscellaneous Symbols
                codePoint in 0x2700..0x27BF ||   // Dingbats
                codePoint in 0x1F600..0x1F64F || // Emoticons
                codePoint in 0x1F680..0x1F6FF || // Transport and Map Symbols
                codePoint in 0x1F1E0..0x1F1FF    // Flags
            ) {
                return true
            }
            i += Character.charCount(codePoint)
        }
        return false
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                // Update Income section
                updateIncomeSectionVisibility(state.isIncomeExpanded)
                val incomeGlobal = state.incomeCategories.filter { it.isGlobal }
                val incomeUser = state.incomeCategories.filter { it.isUserSpecific }
                incomeGlobalAdapter.updateCategories(incomeGlobal)
                incomeUserAdapter.updateCategories(incomeUser)
                tvNoIncomeUserCategories.visibility = if (incomeUser.isEmpty()) View.VISIBLE else View.GONE

                // Update Expense section
                updateExpenseSectionVisibility(state.isExpenseExpanded)
                val expenseGlobal = state.expenseCategories.filter { it.isGlobal }
                val expenseUser = state.expenseCategories.filter { it.isUserSpecific }
                expenseGlobalAdapter.updateCategories(expenseGlobal)
                expenseUserAdapter.updateCategories(expenseUser)
                tvNoExpenseUserCategories.visibility = if (expenseUser.isEmpty()) View.VISIBLE else View.GONE

                // Show error if any
                state.error?.let { error ->
                    if (!state.isUpdating && !state.isDeleting) {
                        Toast.makeText(this@CategoryManagementActivity, error, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun updateIncomeSectionVisibility(isExpanded: Boolean) {
        incomeContent.visibility = if (isExpanded) View.VISIBLE else View.GONE
        ivIncomeExpand.setImageResource(
            if (isExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
        )
    }

    private fun updateExpenseSectionVisibility(isExpanded: Boolean) {
        expenseContent.visibility = if (isExpanded) View.VISIBLE else View.GONE
        ivExpenseExpand.setImageResource(
            if (isExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }
}
