package com.example.personalwealthmanager.presentation.transactions

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.*
import androidx.activity.viewModels
import com.example.personalwealthmanager.domain.model.Transaction
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.personalwealthmanager.R
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class EditTransactionActivity : AppCompatActivity() {

    private val viewModel: TransactionDetailViewModel by viewModels()

    private lateinit var actvTransactionType: AutoCompleteTextView
    private lateinit var etAmount: EditText
    private lateinit var actvCategory: AutoCompleteTextView
    private lateinit var actvSourceRecipient: AutoCompleteTextView
    private lateinit var tvSourceRecipientLabel: TextView
    private lateinit var tvSelectedDate: TextView
    private lateinit var tvSelectedTime: TextView
    private lateinit var datePickerLayout: LinearLayout
    private lateinit var timePickerLayout: LinearLayout
    private lateinit var actvPaymentMethod: AutoCompleteTextView
    private lateinit var etTransactionReference: EditText
    private lateinit var etNotes: EditText
    private lateinit var btnUpdate: Button
    private lateinit var btnCancel: Button
    private lateinit var btnDelete: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var contentCard: View

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val displayTimeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    private var selectedDate: Calendar = Calendar.getInstance()
    private var selectedTime: Calendar = Calendar.getInstance()
    private var mode: String = "add"
    private var transactionId: String? = null
    private var isIncome: Boolean = true
    private var hasPopulatedForm: Boolean = false
    private var currentCategoryType: String? = null  // Track which type categories are loaded for
    private var addCategoryDialog: Dialog? = null

    companion object {
        private const val ADD_NEW_CATEGORY = "➕ Add New Category"
        private const val ADD_NEW_SOURCE = "➕ Add New Source"
        private const val ADD_NEW_RECIPIENT = "➕ Add New Recipient"
    }

    private var addSourceDialog: Dialog? = null
    private var addRecipientDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_transaction)

        // Read intent extras FIRST before setting up UI
        mode = intent.getStringExtra("mode") ?: "add"
        transactionId = intent.getStringExtra("transaction_id")
        isIncome = intent.getBooleanExtra("is_income", true)

        initViews()
        setupClickListeners()
        observeState()

        if (mode == "edit" && transactionId != null) {
            // Edit mode: hide content, show loader, fetch data
            contentCard.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
            btnDelete.visibility = View.VISIBLE
            viewModel.loadTransaction(transactionId!!, isIncome)
        } else {
            // Add mode: hide content, show loader, fetch metadata
            contentCard.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
            btnDelete.visibility = View.GONE
            viewModel.loadMetadata(if (isIncome) "income" else "expense")
        }
    }

    private fun initViews() {
        actvTransactionType = findViewById(R.id.actvTransactionType)
        etAmount = findViewById(R.id.etAmount)
        actvCategory = findViewById(R.id.actvCategory)
        actvSourceRecipient = findViewById(R.id.actvSourceRecipient)
        tvSourceRecipientLabel = findViewById(R.id.tvSourceRecipientLabel)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        tvSelectedTime = findViewById(R.id.tvSelectedTime)
        datePickerLayout = findViewById(R.id.datePickerLayout)
        timePickerLayout = findViewById(R.id.timePickerLayout)
        actvPaymentMethod = findViewById(R.id.actvPaymentMethod)
        etTransactionReference = findViewById(R.id.etTransactionReference)
        etNotes = findViewById(R.id.etNotes)
        btnUpdate = findViewById(R.id.btnUpdate)
        btnCancel = findViewById(R.id.btnCancel)
        btnDelete = findViewById(R.id.btnDelete)
        progressBar = findViewById(R.id.progressBar)
        contentCard = findViewById(R.id.contentCard)

        // Set initial date and time
        tvSelectedDate.text = dateFormat.format(selectedDate.time)
        tvSelectedTime.text = displayTimeFormat.format(selectedTime.time)
    }

    private fun setupDropdowns() {
        // Type dropdown
        val typeOptions = arrayOf("Income", "Expense")
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, typeOptions)
        actvTransactionType.setAdapter(typeAdapter)
        actvTransactionType.setText(if (isIncome) "Income" else "Expense", false)

        actvTransactionType.setOnItemClickListener { _, _, position, _ ->
            isIncome = position == 0
            currentCategoryType = null  // Reset so new categories can be loaded
            viewModel.loadMetadata(if (isIncome) "income" else "expense")
            updateUIForType()
        }

        // Payment method dropdown
        val paymentMethods = arrayOf("Cash", "Credit Card", "Debit Card", "UPI", "Net Banking", "Other")
        val paymentAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, paymentMethods)
        actvPaymentMethod.setAdapter(paymentAdapter)

        // Setup dropdown behavior - show on focus and click
        setupAutoCompleteDropdown(actvTransactionType)
        setupAutoCompleteDropdown(actvPaymentMethod)
    }

    private fun setupAutoCompleteDropdown(autoCompleteTextView: AutoCompleteTextView) {
        // Set threshold to 0 to show all items even without typing
        autoCompleteTextView.threshold = 0

        autoCompleteTextView.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                autoCompleteTextView.post {
                    if (autoCompleteTextView.adapter != null && autoCompleteTextView.adapter.count > 0) {
                        autoCompleteTextView.showDropDown()
                    }
                }
            }
        }
        autoCompleteTextView.setOnClickListener {
            autoCompleteTextView.post {
                if (autoCompleteTextView.adapter != null && autoCompleteTextView.adapter.count > 0) {
                    autoCompleteTextView.showDropDown()
                }
            }
        }
    }

    private fun setupClickListeners() {
        datePickerLayout.setOnClickListener {
            showDatePicker()
        }

        timePickerLayout.setOnClickListener {
            showTimePicker()
        }

        btnUpdate.setOnClickListener {
            saveTransaction()
        }

        btnCancel.setOnClickListener {
            finish()
        }

        btnDelete.setOnClickListener {
            showDeleteConfirmation()
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun updateUIForType() {
        tvSourceRecipientLabel.text = if (isIncome) "Source *" else "Recipient *"
        actvSourceRecipient.hint = if (isIncome) "Select Source" else "Select Recipient"
    }

    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedDate.set(year, month, day)
                tvSelectedDate.text = dateFormat.format(selectedDate.time)
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker() {
        TimePickerDialog(
            this,
            { _, hour, minute ->
                selectedTime.set(Calendar.HOUR_OF_DAY, hour)
                selectedTime.set(Calendar.MINUTE, minute)
                tvSelectedTime.text = displayTimeFormat.format(selectedTime.time)
            },
            selectedTime.get(Calendar.HOUR_OF_DAY),
            selectedTime.get(Calendar.MINUTE),
            false
        ).show()
    }

    private fun saveTransaction() {
        val amount = etAmount.text.toString()
        val categoryText = actvCategory.text.toString()
        val sourceRecipientText = actvSourceRecipient.text.toString()
        val paymentMethod = actvPaymentMethod.text.toString()
        val reference = etTransactionReference.text.toString().ifEmpty { null }
        val notes = etNotes.text.toString().ifEmpty { null }

        // Validation
        if (amount.isEmpty()) {
            Toast.makeText(this, "Please enter amount", Toast.LENGTH_SHORT).show()
            return
        }

        if (categoryText.isEmpty()) {
            Toast.makeText(this, "Please select category", Toast.LENGTH_SHORT).show()
            return
        }

        if (sourceRecipientText.isEmpty()) {
            Toast.makeText(this, "Please select ${if (isIncome) "source" else "recipient"}", Toast.LENGTH_SHORT).show()
            return
        }

        if (paymentMethod.isEmpty()) {
            Toast.makeText(this, "Please select payment method", Toast.LENGTH_SHORT).show()
            return
        }

        val state = viewModel.state.value
        val categoryId = state.categories.find { it.name == categoryText }?.id
        val sourceRecipientId = if (isIncome) {
            state.sources.find { it.name == sourceRecipientText }?.id
        } else {
            state.recipients.find { it.name == sourceRecipientText }?.id
        }

        if (categoryId == null || sourceRecipientId == null) {
            Toast.makeText(this, "Please select all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val type = if (isIncome) "income" else "expense"
        val date = apiDateFormat.format(selectedDate.time)
        val time = timeFormat.format(selectedTime.time)

        viewModel.saveTransaction(
            type = type,
            date = date,
            time = time,
            amount = amount,
            categoryId = categoryId,
            sourceRecipientId = sourceRecipientId,
            paymentMethod = paymentMethod.lowercase().replace(" ", "_"),
            transactionReference = reference,
            tags = null,
            notes = notes
        )
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete") { _, _ ->
                if (transactionId != null) {
                    viewModel.deleteTransaction(transactionId!!, isIncome)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                Log.d("EditTransaction", "State: loading=${state.isLoading}, categories=${state.categories.size}, sources=${state.sources.size}, recipients=${state.recipients.size}, transaction=${state.transaction != null}, error=${state.error}")

                // Handle saving state
                if (state.isSaving) {
                    progressBar.visibility = View.VISIBLE
                    btnUpdate.isEnabled = false
                    return@collect
                }

                btnUpdate.isEnabled = true

                // Check if data is ready
                val isDataReady = if (mode == "edit") {
                    // Edit mode: need categories AND transaction
                    !state.isLoading && state.categories.isNotEmpty() && state.transaction != null
                } else {
                    // Add mode: need categories only
                    !state.isLoading && state.categories.isNotEmpty()
                }

                if (isDataReady && !hasPopulatedForm) {
                    Log.d("EditTransaction", "Data ready, setting up form")
                    hasPopulatedForm = true

                    // Setup base dropdowns (type, payment method) and their listeners
                    setupDropdowns()

                    // Setup dynamic dropdowns (category, source/recipient) from state
                    setupDropdownAdapters(state)

                    // Populate form if editing
                    if (mode == "edit" && state.transaction != null) {
                        populateForm(state.transaction)
                    }

                    // Hide loader, show content
                    progressBar.visibility = View.GONE
                    contentCard.visibility = View.VISIBLE
                    updateUIForType()
                }

                // Handle type change (after initial load)
                if (hasPopulatedForm && state.categories.isNotEmpty() && currentCategoryType != (if (isIncome) "income" else "expense")) {
                    Log.d("EditTransaction", "Type changed, updating adapters")
                    setupDropdownAdapters(state)
                }

                if (state.saveSuccess) {
                    Toast.makeText(this@EditTransactionActivity, "Saved successfully", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }

                state.error?.let { error ->
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@EditTransactionActivity, error, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupDropdownAdapters(state: TransactionDetailState) {
        val expectedType = if (isIncome) "income" else "expense"
        currentCategoryType = expectedType

        // Category dropdown - add "Add New Category" at the end
        val categoryNames = state.categories.map { it.name }.toMutableList()
        categoryNames.add(ADD_NEW_CATEGORY)
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categoryNames)
        actvCategory.setAdapter(categoryAdapter)

        // Handle category selection
        actvCategory.setOnItemClickListener { _, _, position, _ ->
            val selectedItem = categoryNames[position]
            if (selectedItem == ADD_NEW_CATEGORY) {
                // Clear the selection and show dialog
                actvCategory.setText("", false)
                showAddCategoryDialog()
            }
        }

        // Source/Recipient dropdown with "Add New" option
        if (isIncome) {
            val sourceNames = state.sources.map { it.name }.toMutableList()
            sourceNames.add(ADD_NEW_SOURCE)
            val sourceAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, sourceNames)
            actvSourceRecipient.setAdapter(sourceAdapter)
            actvSourceRecipient.setOnItemClickListener { _, _, position, _ ->
                if (sourceNames[position] == ADD_NEW_SOURCE) {
                    actvSourceRecipient.setText("", false)
                    showAddSourceDialog()
                }
            }
        } else {
            val recipientNames = state.recipients.map { it.name }.toMutableList()
            recipientNames.add(ADD_NEW_RECIPIENT)
            val recipientAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, recipientNames)
            actvSourceRecipient.setAdapter(recipientAdapter)
            actvSourceRecipient.setOnItemClickListener { _, _, position, _ ->
                if (recipientNames[position] == ADD_NEW_RECIPIENT) {
                    actvSourceRecipient.setText("", false)
                    showAddRecipientDialog()
                }
            }
        }

        // Setup dropdown click behavior
        setupAutoCompleteDropdown(actvCategory)
        setupAutoCompleteDropdown(actvSourceRecipient)
    }

    private fun populateForm(transaction: Transaction) {
        Log.d("EditTransaction", "Populating form with: $transaction")

        // Transaction type
        actvTransactionType.setText(if (transaction.type == "income") "Income" else "Expense", false)

        // Amount
        etAmount.setText(transaction.amount)

        // Notes and reference
        etNotes.setText(transaction.notes ?: "")
        etTransactionReference.setText(transaction.transactionReference ?: "")

        // Category
        actvCategory.setText(transaction.categoryName, false)

        // Source/Recipient
        val sourceRecipientName = if (transaction.type == "income") {
            transaction.sourceName
        } else {
            transaction.recipientName
        }
        actvSourceRecipient.setText(sourceRecipientName ?: "", false)

        // Payment method (convert from snake_case to Title Case)
        val displayPaymentMethod = transaction.paymentMethod
            .split("_")
            .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
        actvPaymentMethod.setText(displayPaymentMethod, false)

        // Date
        try {
            val parsedDate = apiDateFormat.parse(transaction.date)
            parsedDate?.let {
                selectedDate.time = it
                tvSelectedDate.text = dateFormat.format(selectedDate.time)
            }
        } catch (e: Exception) {
            Log.e("EditTransaction", "Date parse error: ${e.message}")
        }

        // Time
        try {
            val parsedTime = timeFormat.parse(transaction.time)
            parsedTime?.let {
                val tempCal = Calendar.getInstance()
                tempCal.time = it
                selectedTime.set(Calendar.HOUR_OF_DAY, tempCal.get(Calendar.HOUR_OF_DAY))
                selectedTime.set(Calendar.MINUTE, tempCal.get(Calendar.MINUTE))
                tvSelectedTime.text = displayTimeFormat.format(selectedTime.time)
            }
        } catch (e: Exception) {
            Log.e("EditTransaction", "Time parse error: ${e.message}")
        }
    }

    private fun showAddCategoryDialog() {
        addCategoryDialog = Dialog(this)
        addCategoryDialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        addCategoryDialog?.setContentView(R.layout.dialog_add_category)
        addCategoryDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        addCategoryDialog?.setCancelable(true)

        val etCategoryName = addCategoryDialog?.findViewById<TextInputEditText>(R.id.etCategoryName)
        val etCategoryDescription = addCategoryDialog?.findViewById<TextInputEditText>(R.id.etCategoryDescription)
        val etIcon = addCategoryDialog?.findViewById<TextInputEditText>(R.id.etIcon)
        val btnCreate = addCategoryDialog?.findViewById<Button>(R.id.btnCreate)
        val btnCancel = addCategoryDialog?.findViewById<Button>(R.id.btnCancel)
        val btnCloseDialog = addCategoryDialog?.findViewById<ImageView>(R.id.btnCloseDialog)
        val progressBar = addCategoryDialog?.findViewById<ProgressBar>(R.id.progressBar)
        val tvError = addCategoryDialog?.findViewById<TextView>(R.id.tvError)
        val buttonsLayout = addCategoryDialog?.findViewById<LinearLayout>(R.id.buttonsLayout)
        val tvDialogTitle = addCategoryDialog?.findViewById<TextView>(R.id.tvDialogTitle)

        // Update title based on type
        tvDialogTitle?.text = if (isIncome) "Add Income Category" else "Add Expense Category"

        btnCloseDialog?.setOnClickListener {
            addCategoryDialog?.dismiss()
        }

        btnCancel?.setOnClickListener {
            addCategoryDialog?.dismiss()
        }

        btnCreate?.setOnClickListener {
            val name = etCategoryName?.text?.toString()?.trim() ?: ""
            val description = etCategoryDescription?.text?.toString()?.trim()?.ifEmpty { null }
            val icon = etIcon?.text?.toString()?.trim() ?: ""

            if (name.isEmpty()) {
                tvError?.text = "Category name is required"
                tvError?.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (icon.isEmpty()) {
                tvError?.text = "Please enter an emoji icon"
                tvError?.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (!isEmoji(icon)) {
                tvError?.text = "Please enter a valid emoji (e.g. 💰, 🏠, 🚗)"
                tvError?.visibility = View.VISIBLE
                return@setOnClickListener
            }

            // Extract just the first emoji in case user entered multiple
            val firstEmoji = extractFirstEmoji(icon)

            tvError?.visibility = View.GONE
            viewModel.createCategory(
                type = if (isIncome) "income" else "expense",
                name = name,
                description = description,
                icon = firstEmoji
            )
        }

        // Observe category creation state
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                // Handle creating state
                if (state.isCreatingCategory) {
                    progressBar?.visibility = View.VISIBLE
                    buttonsLayout?.visibility = View.GONE
                    tvError?.visibility = View.GONE
                } else {
                    progressBar?.visibility = View.GONE
                    buttonsLayout?.visibility = View.VISIBLE
                }

                // Handle error
                state.categoryCreationError?.let { error ->
                    tvError?.text = error
                    tvError?.visibility = View.VISIBLE
                }

                // Handle success
                state.categoryCreated?.let { category ->
                    // Set the newly created category in the dropdown
                    actvCategory.setText(category.name, false)

                    // Update the adapter with new categories
                    val updatedNames = state.categories.map { it.name }.toMutableList()
                    updatedNames.add(ADD_NEW_CATEGORY)
                    val newAdapter = ArrayAdapter(this@EditTransactionActivity, android.R.layout.simple_dropdown_item_1line, updatedNames)
                    actvCategory.setAdapter(newAdapter)

                    // Clear the state and dismiss dialog
                    viewModel.clearCategoryCreationState()
                    addCategoryDialog?.dismiss()
                    Toast.makeText(this@EditTransactionActivity, "Category created successfully", Toast.LENGTH_SHORT).show()
                }
            }
        }

        addCategoryDialog?.show()
    }

    private fun showAddSourceDialog() {
        addSourceDialog = Dialog(this)
        addSourceDialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        addSourceDialog?.setContentView(R.layout.dialog_edit_source)
        addSourceDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        addSourceDialog?.setCancelable(true)

        val tvDialogTitle = addSourceDialog?.findViewById<TextView>(R.id.tvDialogTitle)
        val etSourceName = addSourceDialog?.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSourceName)
        val etSourceDescription = addSourceDialog?.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSourceDescription)
        val etSourceIdentifier = addSourceDialog?.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSourceIdentifier)
        val spinnerDefaultCategory = addSourceDialog?.findViewById<Spinner>(R.id.spinnerDefaultCategory)
        val btnSave = addSourceDialog?.findViewById<Button>(R.id.btnSave)
        val btnCancel = addSourceDialog?.findViewById<Button>(R.id.btnCancel)
        val btnCloseDialog = addSourceDialog?.findViewById<ImageView>(R.id.btnCloseDialog)
        val progressBar = addSourceDialog?.findViewById<ProgressBar>(R.id.progressBar)
        val tvError = addSourceDialog?.findViewById<TextView>(R.id.tvError)
        val buttonsLayout = addSourceDialog?.findViewById<LinearLayout>(R.id.buttonsLayout)
        val tvTransactionWarning = addSourceDialog?.findViewById<TextView>(R.id.tvTransactionWarning)
        val btnDelete = addSourceDialog?.findViewById<Button>(R.id.btnDelete)

        tvDialogTitle?.text = "Add New Source"
        tvTransactionWarning?.visibility = View.GONE
        btnDelete?.visibility = View.GONE

        // Populate income category spinner (state.categories holds income categories when isIncome==true)
        val incomeCategories = viewModel.state.value.categories
        val categoryNames = mutableListOf("-- None --") + incomeCategories.map { it.name }
        val categorySpinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryNames)
        categorySpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDefaultCategory?.adapter = categorySpinnerAdapter

        btnCloseDialog?.setOnClickListener { addSourceDialog?.dismiss() }
        btnCancel?.setOnClickListener { addSourceDialog?.dismiss() }

        btnSave?.setOnClickListener {
            val name = etSourceName?.text?.toString()?.trim() ?: ""
            val description = etSourceDescription?.text?.toString()?.trim()?.ifEmpty { null }
            val sourceIdentifier = etSourceIdentifier?.text?.toString()?.trim()?.ifEmpty { null }
            val selectedPos = spinnerDefaultCategory?.selectedItemPosition ?: 0
            val defaultCategoryId = if (selectedPos > 0) incomeCategories[selectedPos - 1].id else null
            if (name.isEmpty()) {
                tvError?.text = "Source name is required"
                tvError?.visibility = View.VISIBLE
                return@setOnClickListener
            }
            tvError?.visibility = View.GONE
            viewModel.createSource(name, description, sourceIdentifier, defaultCategoryId)
        }

        lifecycleScope.launch {
            viewModel.state.collect { state ->
                if (state.isCreatingSource) {
                    progressBar?.visibility = View.VISIBLE
                    buttonsLayout?.visibility = View.GONE
                    tvError?.visibility = View.GONE
                } else {
                    progressBar?.visibility = View.GONE
                    buttonsLayout?.visibility = View.VISIBLE
                }
                state.sourceCreationError?.let { error ->
                    tvError?.text = error
                    tvError?.visibility = View.VISIBLE
                }
                state.sourceCreated?.let { source ->
                    actvSourceRecipient.setText(source.name, false)
                    val updatedNames = viewModel.state.value.sources.map { it.name }.toMutableList()
                    updatedNames.add(ADD_NEW_SOURCE)
                    actvSourceRecipient.setAdapter(ArrayAdapter(this@EditTransactionActivity, android.R.layout.simple_dropdown_item_1line, updatedNames))
                    viewModel.clearSourceCreationState()
                    addSourceDialog?.dismiss()
                    Toast.makeText(this@EditTransactionActivity, "Source created successfully", Toast.LENGTH_SHORT).show()
                }
            }
        }

        addSourceDialog?.show()
    }

    private fun showAddRecipientDialog() {
        addRecipientDialog = Dialog(this)
        addRecipientDialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        addRecipientDialog?.setContentView(R.layout.dialog_edit_recipient)
        addRecipientDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        addRecipientDialog?.setCancelable(true)

        val tvDialogTitle = addRecipientDialog?.findViewById<TextView>(R.id.tvDialogTitle)
        val etRecipientName = addRecipientDialog?.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etRecipientName)
        val etRecipientDescription = addRecipientDialog?.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etRecipientDescription)
        val cbFavorite = addRecipientDialog?.findViewById<android.widget.CheckBox>(R.id.cbFavorite)
        val spinnerDefaultCategory = addRecipientDialog?.findViewById<Spinner>(R.id.spinnerDefaultCategory)
        val btnSave = addRecipientDialog?.findViewById<Button>(R.id.btnSave)
        val btnCancel = addRecipientDialog?.findViewById<Button>(R.id.btnCancel)
        val btnCloseDialog = addRecipientDialog?.findViewById<ImageView>(R.id.btnCloseDialog)
        val progressBar = addRecipientDialog?.findViewById<ProgressBar>(R.id.progressBar)
        val tvError = addRecipientDialog?.findViewById<TextView>(R.id.tvError)
        val buttonsLayout = addRecipientDialog?.findViewById<LinearLayout>(R.id.buttonsLayout)
        val tvTransactionWarning = addRecipientDialog?.findViewById<TextView>(R.id.tvTransactionWarning)
        val btnDelete = addRecipientDialog?.findViewById<Button>(R.id.btnDelete)

        tvDialogTitle?.text = "Add New Recipient"
        tvTransactionWarning?.visibility = View.GONE
        btnDelete?.visibility = View.GONE

        // Populate expense categories spinner
        val expenseCategories = viewModel.state.value.expenseCategories
        val categoryNames = mutableListOf("-- None --") + expenseCategories.map { it.name }
        val categorySpinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryNames)
        categorySpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDefaultCategory?.adapter = categorySpinnerAdapter

        btnCloseDialog?.setOnClickListener { addRecipientDialog?.dismiss() }
        btnCancel?.setOnClickListener { addRecipientDialog?.dismiss() }

        btnSave?.setOnClickListener {
            val name = etRecipientName?.text?.toString()?.trim() ?: ""
            val description = etRecipientDescription?.text?.toString()?.trim()?.ifEmpty { null }
            val isFavorite = cbFavorite?.isChecked ?: false
            val selectedPos = spinnerDefaultCategory?.selectedItemPosition ?: 0
            val defaultCategoryId = if (selectedPos > 0) expenseCategories[selectedPos - 1].id else null
            if (name.isEmpty()) {
                tvError?.text = "Recipient name is required"
                tvError?.visibility = View.VISIBLE
                return@setOnClickListener
            }
            tvError?.visibility = View.GONE
            viewModel.createRecipient(name, description, isFavorite, defaultCategoryId)
        }

        lifecycleScope.launch {
            viewModel.state.collect { state ->
                if (state.isCreatingRecipient) {
                    progressBar?.visibility = View.VISIBLE
                    buttonsLayout?.visibility = View.GONE
                    tvError?.visibility = View.GONE
                } else {
                    progressBar?.visibility = View.GONE
                    buttonsLayout?.visibility = View.VISIBLE
                }
                state.recipientCreationError?.let { error ->
                    tvError?.text = error
                    tvError?.visibility = View.VISIBLE
                }
                state.recipientCreated?.let { recipient ->
                    actvSourceRecipient.setText(recipient.name, false)
                    val updatedNames = viewModel.state.value.recipients.map { it.name }.toMutableList()
                    updatedNames.add(ADD_NEW_RECIPIENT)
                    actvSourceRecipient.setAdapter(ArrayAdapter(this@EditTransactionActivity, android.R.layout.simple_dropdown_item_1line, updatedNames))
                    viewModel.clearRecipientCreationState()
                    addRecipientDialog?.dismiss()
                    Toast.makeText(this@EditTransactionActivity, "Recipient created successfully", Toast.LENGTH_SHORT).show()
                }
            }
        }

        addRecipientDialog?.show()
    }

    private fun isEmoji(text: String): Boolean {
        if (text.isEmpty()) return false

        // Check if text contains emoji using Character type detection
        for (codePoint in text.codePoints()) {
            val type = Character.getType(codePoint)
            // Emoji types: OTHER_SYMBOL, SURROGATE (for complex emojis)
            if (type == Character.OTHER_SYMBOL.toInt() ||
                type == Character.SURROGATE.toInt() ||
                // Also check common emoji ranges
                codePoint in 0x1F300..0x1F9FF ||  // Misc Symbols, Emoticons, etc
                codePoint in 0x2600..0x26FF ||    // Misc symbols
                codePoint in 0x2700..0x27BF ||    // Dingbats
                codePoint in 0x1FA00..0x1FAFF) { // Extended symbols
                return true
            }
        }
        return false
    }

    private fun extractFirstEmoji(text: String): String {
        // Extract the first grapheme cluster (emoji) from the text
        val breakIterator = java.text.BreakIterator.getCharacterInstance()
        breakIterator.setText(text)
        val start = breakIterator.first()
        val end = breakIterator.next()
        return if (end != java.text.BreakIterator.DONE) {
            text.substring(start, end)
        } else {
            text
        }
    }
}
