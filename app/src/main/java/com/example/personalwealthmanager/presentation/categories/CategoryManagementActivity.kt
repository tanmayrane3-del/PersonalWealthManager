package com.pwm.personalwealthmanager.presentation.categories

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.activity.viewModels
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pwm.personalwealthmanager.R
import com.pwm.personalwealthmanager.domain.model.Category
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CategoryManagementActivity : com.pwm.personalwealthmanager.presentation.base.BaseDrawerActivity() {

    override fun getSelfButtonId() = R.id.btnCategoryManagement

    private val viewModel: CategoryManagementViewModel by viewModels()

    private lateinit var progressBar: ProgressBar
    private lateinit var etSearch: EditText
    private lateinit var fabAddCategory: FloatingActionButton

    // Income section
    private lateinit var incomeHeader: LinearLayout
    private lateinit var ivIncomeExpand: ImageView
    private lateinit var incomeContent: LinearLayout
    private lateinit var tvIncomeCount: TextView
    private lateinit var rvIncomeGlobalCategories: RecyclerView
    private lateinit var incomeDivider: View
    private lateinit var tvIncomeCustomLabel: TextView
    private lateinit var rvIncomeUserCategories: RecyclerView
    private lateinit var tvNoIncomeUserCategories: TextView

    // Expense section
    private lateinit var expenseHeader: LinearLayout
    private lateinit var ivExpenseExpand: ImageView
    private lateinit var expenseContent: LinearLayout
    private lateinit var tvExpenseCount: TextView
    private lateinit var rvExpenseGlobalCategories: RecyclerView
    private lateinit var expenseDivider: View
    private lateinit var tvExpenseCustomLabel: TextView
    private lateinit var rvExpenseUserCategories: RecyclerView
    private lateinit var tvNoExpenseUserCategories: TextView

    // Adapters
    private lateinit var incomeGlobalAdapter: CategoryAdapter
    private lateinit var incomeUserAdapter: CategoryAdapter
    private lateinit var expenseGlobalAdapter: CategoryAdapter
    private lateinit var expenseUserAdapter: CategoryAdapter

    private var isIncomeExpanded = true
    private var isExpenseExpanded = true
    private var searchQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_management)

        initializeViews()
        setupRecyclerViews()
        setupClickListeners()
        setupDrawerMenu()
        setupBottomNav()
        observeState()
    }

    private fun initializeViews() {
        progressBar = findViewById(R.id.progressBar)
        etSearch = findViewById(R.id.etSearch)
        fabAddCategory = findViewById(R.id.fabAddCategory)

        incomeHeader = findViewById(R.id.incomeHeader)
        ivIncomeExpand = findViewById(R.id.ivIncomeExpand)
        incomeContent = findViewById(R.id.incomeContent)
        tvIncomeCount = findViewById(R.id.tvIncomeCount)
        rvIncomeGlobalCategories = findViewById(R.id.rvIncomeGlobalCategories)
        incomeDivider = findViewById(R.id.incomeDivider)
        tvIncomeCustomLabel = findViewById(R.id.tvIncomeCustomLabel)
        rvIncomeUserCategories = findViewById(R.id.rvIncomeUserCategories)
        tvNoIncomeUserCategories = findViewById(R.id.tvNoIncomeUserCategories)

        expenseHeader = findViewById(R.id.expenseHeader)
        ivExpenseExpand = findViewById(R.id.ivExpenseExpand)
        expenseContent = findViewById(R.id.expenseContent)
        tvExpenseCount = findViewById(R.id.tvExpenseCount)
        rvExpenseGlobalCategories = findViewById(R.id.rvExpenseGlobalCategories)
        expenseDivider = findViewById(R.id.expenseDivider)
        tvExpenseCustomLabel = findViewById(R.id.tvExpenseCustomLabel)
        rvExpenseUserCategories = findViewById(R.id.rvExpenseUserCategories)
        tvNoExpenseUserCategories = findViewById(R.id.tvNoExpenseUserCategories)
    }

    private fun setupRecyclerViews() {
        incomeGlobalAdapter = CategoryAdapter(emptyList())
        rvIncomeGlobalCategories.layoutManager = LinearLayoutManager(this)
        rvIncomeGlobalCategories.adapter = incomeGlobalAdapter

        incomeUserAdapter = CategoryAdapter(
            categories = emptyList(),
            onItemClick = { category -> showEditCategoryDialog(category, "income") }
        )
        rvIncomeUserCategories.layoutManager = LinearLayoutManager(this)
        rvIncomeUserCategories.adapter = incomeUserAdapter

        expenseGlobalAdapter = CategoryAdapter(emptyList())
        rvExpenseGlobalCategories.layoutManager = LinearLayoutManager(this)
        rvExpenseGlobalCategories.adapter = expenseGlobalAdapter

        expenseUserAdapter = CategoryAdapter(
            categories = emptyList(),
            onItemClick = { category -> showEditCategoryDialog(category, "expense") }
        )
        rvExpenseUserCategories.layoutManager = LinearLayoutManager(this)
        rvExpenseUserCategories.adapter = expenseUserAdapter
    }

    private fun setupClickListeners() {
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<ImageView>(R.id.btnMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        incomeHeader.setOnClickListener {
            isIncomeExpanded = !isIncomeExpanded
            setIncomeExpanded(isIncomeExpanded)
        }

        expenseHeader.setOnClickListener {
            isExpenseExpanded = !isExpenseExpanded
            setExpenseExpanded(isExpenseExpanded)
        }

        fabAddCategory.setOnClickListener {
            val types = arrayOf("Income Category", "Expense Category")
            AlertDialog.Builder(this)
                .setTitle("Add New Category")
                .setItems(types) { _, which ->
                    val type = if (which == 0) "income" else "expense"
                    showCreateCategoryDialog(type)
                }
                .show()
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString() ?: ""
                applySearchFilter(viewModel.state.value)
            }
        })
    }

    private fun applySearchFilter(state: CategoryManagementState) {
        fun filter(list: List<Category>) =
            if (searchQuery.isBlank()) list
            else list.filter { c ->
                c.name.contains(searchQuery, ignoreCase = true) ||
                c.description?.contains(searchQuery, ignoreCase = true) == true
            }

        val incomeGlobal = filter(state.incomeCategories.filter { it.isGlobal })
        val incomeUser = filter(state.incomeCategories.filter { it.isUserSpecific })
        incomeGlobalAdapter.updateCategories(incomeGlobal)
        incomeUserAdapter.updateCategories(incomeUser)
        tvIncomeCount.text = "${incomeGlobal.size + incomeUser.size} TOTAL"
        val showIncomeCustom = incomeUser.isNotEmpty()
        incomeDivider.visibility = if (showIncomeCustom && incomeGlobal.isNotEmpty()) View.VISIBLE else View.GONE
        tvIncomeCustomLabel.visibility = if (showIncomeCustom) View.VISIBLE else View.GONE
        tvNoIncomeUserCategories.visibility = if (incomeUser.isEmpty()) View.VISIBLE else View.GONE

        val expenseGlobal = filter(state.expenseCategories.filter { it.isGlobal })
        val expenseUser = filter(state.expenseCategories.filter { it.isUserSpecific })
        expenseGlobalAdapter.updateCategories(expenseGlobal)
        expenseUserAdapter.updateCategories(expenseUser)
        tvExpenseCount.text = "${expenseGlobal.size + expenseUser.size} TOTAL"
        val showExpenseCustom = expenseUser.isNotEmpty()
        expenseDivider.visibility = if (showExpenseCustom && expenseGlobal.isNotEmpty()) View.VISIBLE else View.GONE
        tvExpenseCustomLabel.visibility = if (showExpenseCustom) View.VISIBLE else View.GONE
        tvNoExpenseUserCategories.visibility = if (expenseUser.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun setIncomeExpanded(expanded: Boolean) {
        incomeContent.visibility = if (expanded) View.VISIBLE else View.GONE
        ivIncomeExpand.setImageResource(if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more)
    }

    private fun setExpenseExpanded(expanded: Boolean) {
        expenseContent.visibility = if (expanded) View.VISIBLE else View.GONE
        ivExpenseExpand.setImageResource(if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more)
    }

    private fun showCreateCategoryDialog(type: String) {
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(R.layout.dialog_edit_category)

        val tvDialogTitle = dialog.findViewById<TextView>(R.id.tvDialogTitle)!!
        val etCategoryName = dialog.findViewById<TextInputEditText>(R.id.etCategoryName)!!
        val etCategoryDescription = dialog.findViewById<TextInputEditText>(R.id.etCategoryDescription)!!
        val etIcon = dialog.findViewById<TextInputEditText>(R.id.etIcon)!!
        val tvTransactionWarning = dialog.findViewById<TextView>(R.id.tvTransactionWarning)!!
        val progressBar = dialog.findViewById<ProgressBar>(R.id.progressBar)!!
        val tvError = dialog.findViewById<TextView>(R.id.tvError)!!
        val buttonsLayout = dialog.findViewById<LinearLayout>(R.id.buttonsLayout)!!
        val btnCancel = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)!!
        val btnSave = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSave)!!
        val btnDelete = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDelete)!!
        val toggleIncomeType = dialog.findViewById<MaterialButtonToggleGroup>(R.id.toggleIncomeType)!!
        val toggleSpendingType = dialog.findViewById<MaterialButtonToggleGroup>(R.id.toggleSpendingType)!!

        val typeLabel = if (type == "income") "Income" else "Expense"
        tvDialogTitle.text = "Add $typeLabel Category"
        tvTransactionWarning.visibility = View.GONE
        btnDelete.visibility = View.GONE

        // Show appropriate budget type toggle
        if (type == "income") {
            toggleIncomeType.visibility = View.VISIBLE
            toggleSpendingType.visibility = View.GONE
        } else {
            toggleIncomeType.visibility = View.GONE
            toggleSpendingType.visibility = View.VISIBLE
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val name = etCategoryName.text.toString().trim()
            val description = etCategoryDescription.text.toString().trim().ifEmpty { null }
            val icon = etIcon.text.toString().trim().ifEmpty { null }

            if (name.isEmpty()) {
                tvError.text = getString(R.string.name_required)
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (icon != null && !isValidEmoji(icon)) {
                tvError.text = "Please enter a valid emoji icon"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            val budgetType = if (type == "income") {
                when (toggleIncomeType.checkedButtonId) {
                    R.id.btnPrimary -> "primary"
                    R.id.btnPassive -> "passive"
                    R.id.btnOneTime -> "one_time"
                    else -> null
                }
            } else {
                when (toggleSpendingType.checkedButtonId) {
                    R.id.btnNeeds -> "need"
                    R.id.btnWants -> "want"
                    R.id.btnSavings -> "savings_investment"
                    else -> null
                }
            }

            tvError.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
            buttonsLayout.visibility = View.GONE

            viewModel.createCategory(type, name, description, icon, budgetType)

            lifecycleScope.launch {
                viewModel.state.collect { state ->
                    if (state.createSuccess) {
                        viewModel.clearSuccessStates()
                        dialog.dismiss()
                        Toast.makeText(
                            this@CategoryManagementActivity,
                            "$typeLabel category created",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@collect
                    }
                    if (!state.isCreating && state.error != null) {
                        progressBar.visibility = View.GONE
                        buttonsLayout.visibility = View.VISIBLE
                        tvError.text = state.error
                        tvError.visibility = View.VISIBLE
                        viewModel.clearError()
                    }
                }
            }
        }

        showExpandedBottomSheet(dialog)
    }

    private fun showEditCategoryDialog(category: Category, type: String) {
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(R.layout.dialog_edit_category)

        val etCategoryName = dialog.findViewById<TextInputEditText>(R.id.etCategoryName)!!
        val etCategoryDescription = dialog.findViewById<TextInputEditText>(R.id.etCategoryDescription)!!
        val etIcon = dialog.findViewById<TextInputEditText>(R.id.etIcon)!!
        val tvTransactionWarning = dialog.findViewById<TextView>(R.id.tvTransactionWarning)!!
        val progressBar = dialog.findViewById<ProgressBar>(R.id.progressBar)!!
        val tvError = dialog.findViewById<TextView>(R.id.tvError)!!
        val buttonsLayout = dialog.findViewById<LinearLayout>(R.id.buttonsLayout)!!
        val btnCancel = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)!!
        val btnSave = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSave)!!
        val btnDelete = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDelete)!!
        val btnCloseDialog = dialog.findViewById<ImageView>(R.id.btnCloseDialog)!!
        val toggleIncomeType = dialog.findViewById<MaterialButtonToggleGroup>(R.id.toggleIncomeType)!!
        val toggleSpendingType = dialog.findViewById<MaterialButtonToggleGroup>(R.id.toggleSpendingType)!!

        etCategoryName.setText(category.name)
        etCategoryDescription.setText(category.description ?: "")
        etIcon.setText(category.icon ?: "")

        // Show appropriate budget type toggle and pre-select existing value
        if (type == "income") {
            toggleIncomeType.visibility = View.VISIBLE
            toggleSpendingType.visibility = View.GONE
            when (category.incomeType) {
                "primary" -> toggleIncomeType.check(R.id.btnPrimary)
                "passive" -> toggleIncomeType.check(R.id.btnPassive)
                "one_time" -> toggleIncomeType.check(R.id.btnOneTime)
            }
        } else {
            toggleIncomeType.visibility = View.GONE
            toggleSpendingType.visibility = View.VISIBLE
            when (category.spendingType) {
                "need" -> toggleSpendingType.check(R.id.btnNeeds)
                "want" -> toggleSpendingType.check(R.id.btnWants)
                "savings_investment" -> toggleSpendingType.check(R.id.btnSavings)
            }
        }

        if (category.transactionCount > 0) {
            tvTransactionWarning.text = getString(R.string.category_has_transactions, category.transactionCount)
            tvTransactionWarning.visibility = View.VISIBLE
            btnDelete.visibility = View.GONE
        } else {
            tvTransactionWarning.visibility = View.GONE
            btnDelete.visibility = View.VISIBLE
        }

        btnCloseDialog.setOnClickListener { dialog.dismiss() }
        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val name = etCategoryName.text.toString().trim()
            val description = etCategoryDescription.text.toString().trim().ifEmpty { null }
            val icon = etIcon.text.toString().trim().ifEmpty { null }

            if (name.isEmpty()) {
                tvError.text = getString(R.string.name_required)
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (icon != null && !isValidEmoji(icon)) {
                tvError.text = "Please enter a valid emoji icon"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            val budgetType = if (type == "income") {
                when (toggleIncomeType.checkedButtonId) {
                    R.id.btnPrimary -> "primary"
                    R.id.btnPassive -> "passive"
                    R.id.btnOneTime -> "one_time"
                    else -> null
                }
            } else {
                when (toggleSpendingType.checkedButtonId) {
                    R.id.btnNeeds -> "need"
                    R.id.btnWants -> "want"
                    R.id.btnSavings -> "savings_investment"
                    else -> null
                }
            }

            tvError.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
            buttonsLayout.visibility = View.GONE
            btnDelete.visibility = View.GONE

            viewModel.updateCategory(type, category.id, name, description, icon, budgetType)

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
                        if (category.transactionCount == 0) btnDelete.visibility = View.VISIBLE
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

        showExpandedBottomSheet(dialog)
    }

    private fun showExpandedBottomSheet(dialog: BottomSheetDialog) {
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { view ->
                val lp = view.layoutParams
                lp.height = (resources.displayMetrics.heightPixels * 0.75).toInt()
                view.layoutParams = lp
                BottomSheetBehavior.from(view).apply {
                    state = BottomSheetBehavior.STATE_EXPANDED
                    skipCollapsed = true
                }
            }
        }
        dialog.show()
    }

    private fun isValidEmoji(text: String): Boolean {
        if (text.isEmpty()) return false
        var i = 0
        while (i < text.length) {
            val codePoint = text.codePointAt(i)
            if (codePoint in 0x1F300..0x1F9FF ||
                codePoint in 0x2600..0x26FF ||
                codePoint in 0x2700..0x27BF ||
                codePoint in 0x1F600..0x1F64F ||
                codePoint in 0x1F680..0x1F6FF ||
                codePoint in 0x1F1E0..0x1F1FF
            ) return true
            i += Character.charCount(codePoint)
        }
        return false
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                applySearchFilter(state)

                state.error?.let { error ->
                    if (!state.isUpdating && !state.isDeleting && !state.isCreating) {
                        Toast.makeText(this@CategoryManagementActivity, error, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
