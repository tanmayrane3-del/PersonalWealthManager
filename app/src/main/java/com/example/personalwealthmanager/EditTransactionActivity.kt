package com.example.personalwealthmanager

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.personalwealthmanager.ApiConfig
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class EditTransactionActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var contentLayout: androidx.cardview.widget.CardView
    private lateinit var actvType: AutoCompleteTextView
    private lateinit var etAmount: EditText
    private lateinit var actvCategory: AutoCompleteTextView
    private lateinit var actvSourceRecipient: AutoCompleteTextView
    private lateinit var tilSourceRecipient: TextInputLayout
    private lateinit var tvSourceRecipientLabel: TextView
    private lateinit var tvSelectedDate: TextView
    private lateinit var tvSelectedTime: TextView
    private lateinit var datePickerLayout: LinearLayout
    private lateinit var timePickerLayout: LinearLayout
    private lateinit var actvPaymentMethod: AutoCompleteTextView
    private lateinit var etTransactionReference: EditText
    private lateinit var etTags: EditText
    private lateinit var etNotes: EditText
    private lateinit var btnDelete: Button
    private lateinit var btnCancel: Button
    private lateinit var btnUpdate: Button

    private var transactionId: String = ""
    private var isIncome: Boolean = true
    private var selectedDate: Date? = null
    private var selectedTime: String? = null
    private var selectedTypeIndex = 0

    private val incomeCategories = mutableListOf<Pair<String, String>>()
    private val expenseCategories = mutableListOf<Pair<String, String>>()
    private val sources = mutableListOf<Pair<String, String>>()
    private val recipients = mutableListOf<Pair<String, String>>()

    // Store the loaded transaction data
    private var transactionData: JSONObject? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_transaction)

        // Get transaction details from intent
        transactionId = intent.getStringExtra("transaction_id") ?: ""
        isIncome = intent.getBooleanExtra("is_income", true)

        println("EditTransaction: Starting with ID=$transactionId, isIncome=$isIncome")

        initializeViews()
        setupListeners()

        // Start loading flow
        loadTransactionFlow()
    }

    private fun initializeViews() {
        btnBack = findViewById(R.id.btnBack)
        progressBar = findViewById(R.id.progressBar)
        contentLayout = findViewById(R.id.contentCard)
        actvType = findViewById(R.id.actvTransactionType)
        etAmount = findViewById(R.id.etAmount)
        actvCategory = findViewById(R.id.actvCategory)
        actvSourceRecipient = findViewById(R.id.actvSourceRecipient)
        tilSourceRecipient = findViewById(R.id.tilSourceRecipient)
        tvSourceRecipientLabel = findViewById(R.id.tvSourceRecipientLabel)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        tvSelectedTime = findViewById(R.id.tvSelectedTime)
        datePickerLayout = findViewById(R.id.datePickerLayout)
        timePickerLayout = findViewById(R.id.timePickerLayout)
        actvPaymentMethod = findViewById(R.id.actvPaymentMethod)
        etTransactionReference = findViewById(R.id.etTransactionReference)
        etTags = findViewById(R.id.etTags)
        etNotes = findViewById(R.id.etNotes)
        btnDelete = findViewById(R.id.btnDelete)
        btnCancel = findViewById(R.id.btnCancel)
        btnUpdate = findViewById(R.id.btnUpdate)

        // Hide content initially, show loader
        contentLayout.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }
        btnCancel.setOnClickListener { finish() }

        btnDelete.setOnClickListener {
            showDeleteConfirmation()
        }

        btnUpdate.setOnClickListener {
            updateTransaction()
        }

        setupTransactionTypeDropdown()
        setupPaymentMethodDropdown()
        setupDateTimePickers()
    }

    private fun loadTransactionFlow() {
        println("EditTransaction: === STARTING LOAD FLOW ===")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = getSessionToken()
                if (token == null) {
                    println("EditTransaction: ERROR - No session token")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@EditTransactionActivity, "Session expired. Please login again.", Toast.LENGTH_LONG).show()
                        finish()
                    }
                    return@launch
                }

                // Step 1: Load metadata (categories, sources, recipients)
                println("EditTransaction: Step 1 - Loading metadata...")
                val metadataLoaded = loadMetadata(token)

                if (!metadataLoaded) {
                    println("EditTransaction: ERROR - Failed to load metadata")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@EditTransactionActivity, "Failed to load metadata", Toast.LENGTH_LONG).show()
                        finish()
                    }
                    return@launch
                }

                println("EditTransaction: Step 1 - Metadata loaded successfully")

                // Step 2: Load transaction data
                println("EditTransaction: Step 2 - Loading transaction data...")
                val transactionLoaded = loadTransactionData(token)

                if (!transactionLoaded) {
                    println("EditTransaction: ERROR - Failed to load transaction")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@EditTransactionActivity, "Failed to load transaction", Toast.LENGTH_LONG).show()
                        finish()
                    }
                    return@launch
                }

                println("EditTransaction: Step 2 - Transaction loaded successfully")

                // Step 3: Show the UI and populate fields
                println("EditTransaction: Step 3 - Populating UI...")
                withContext(Dispatchers.Main) {
                    // Hide loader, show content
                    progressBar.visibility = View.GONE
                    contentLayout.visibility = View.VISIBLE

                    // Populate dropdowns
                    updateCategoryDropdown()
                    updateSourceRecipientDropdown()

                    // Populate fields
                    transactionData?.let { populateFields(it) }
                }

                println("EditTransaction: === LOAD FLOW COMPLETE ===")

            } catch (e: Exception) {
                println("EditTransaction: ERROR - Exception in loadTransactionFlow: ${e.message}")
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditTransactionActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

    private suspend fun loadMetadata(token: String): Boolean {
        return try {
            // Load income categories
            val incomeCategoriesUrl = "${ApiConfig.BASE_URL}/api/categories/income"
            println("EditTransaction: Loading income categories from: $incomeCategoriesUrl")
            val incomeCategoriesConnection = URL(incomeCategoriesUrl).openConnection() as HttpURLConnection
            incomeCategoriesConnection.requestMethod = "GET"
            incomeCategoriesConnection.setRequestProperty("X-Session-Token", token)

            if (incomeCategoriesConnection.responseCode == 200) {
                val response = incomeCategoriesConnection.inputStream.bufferedReader().readText()
                val jsonResponse = JSONObject(response)
                val categoriesArray = jsonResponse.getJSONArray("data")

                incomeCategories.clear()
                for (i in 0 until categoriesArray.length()) {
                    val category = categoriesArray.getJSONObject(i)
                    val id = category.getString("id")
                    val name = category.getString("name")
                    incomeCategories.add(Pair(id, name))
                }
                println("EditTransaction: Loaded ${incomeCategories.size} income categories")
            } else {
                val errorBody = incomeCategoriesConnection.errorStream?.bufferedReader()?.readText() ?: "No error body"
                println("EditTransaction: ERROR - Income categories response: ${incomeCategoriesConnection.responseCode}")
                println("EditTransaction: ERROR - Error body: $errorBody")
                return false
            }

            // Load expense categories
            val expenseCategoriesUrl = "${ApiConfig.BASE_URL}/api/categories/expense"
            println("EditTransaction: Loading expense categories from: $expenseCategoriesUrl")
            val expenseCategoriesConnection = URL(expenseCategoriesUrl).openConnection() as HttpURLConnection
            expenseCategoriesConnection.requestMethod = "GET"
            expenseCategoriesConnection.setRequestProperty("X-Session-Token", token)

            if (expenseCategoriesConnection.responseCode == 200) {
                val response = expenseCategoriesConnection.inputStream.bufferedReader().readText()
                val jsonResponse = JSONObject(response)
                val categoriesArray = jsonResponse.getJSONArray("data")

                expenseCategories.clear()
                for (i in 0 until categoriesArray.length()) {
                    val category = categoriesArray.getJSONObject(i)
                    val id = category.getString("id")
                    val name = category.getString("name")
                    expenseCategories.add(Pair(id, name))
                }
                println("EditTransaction: Loaded ${expenseCategories.size} expense categories")
            } else {
                val errorBody = expenseCategoriesConnection.errorStream?.bufferedReader()?.readText() ?: "No error body"
                println("EditTransaction: ERROR - Expense categories response: ${expenseCategoriesConnection.responseCode}")
                println("EditTransaction: ERROR - Error body: $errorBody")
                return false
            }

            // Load sources
            val sourcesUrl = "${ApiConfig.BASE_URL}/api/sources"
            println("EditTransaction: Loading sources from: $sourcesUrl")
            val sourcesConnection = URL(sourcesUrl).openConnection() as HttpURLConnection
            sourcesConnection.requestMethod = "GET"
            sourcesConnection.setRequestProperty("X-Session-Token", token)

            if (sourcesConnection.responseCode == 200) {
                val response = sourcesConnection.inputStream.bufferedReader().readText()
                val jsonResponse = JSONObject(response)
                val sourcesArray = jsonResponse.getJSONArray("data")

                sources.clear()
                for (i in 0 until sourcesArray.length()) {
                    val source = sourcesArray.getJSONObject(i)
                    sources.add(Pair(source.getString("id"), source.getString("name")))
                }
                println("EditTransaction: Loaded ${sources.size} sources")
            } else {
                println("EditTransaction: ERROR - Sources response: ${sourcesConnection.responseCode}")
                return false
            }

            // Load recipients
            val recipientsUrl = "${ApiConfig.BASE_URL}/api/recipients"
            println("EditTransaction: Loading recipients from: $recipientsUrl")
            val recipientsConnection = URL(recipientsUrl).openConnection() as HttpURLConnection
            recipientsConnection.requestMethod = "GET"
            recipientsConnection.setRequestProperty("X-Session-Token", token)

            if (recipientsConnection.responseCode == 200) {
                val response = recipientsConnection.inputStream.bufferedReader().readText()
                val jsonResponse = JSONObject(response)
                val recipientsArray = jsonResponse.getJSONArray("data")

                recipients.clear()
                for (i in 0 until recipientsArray.length()) {
                    val recipient = recipientsArray.getJSONObject(i)
                    recipients.add(Pair(recipient.getString("id"), recipient.getString("name")))
                }
                println("EditTransaction: Loaded ${recipients.size} recipients")
            } else {
                println("EditTransaction: ERROR - Recipients response: ${recipientsConnection.responseCode}")
                return false
            }

            true
        } catch (e: Exception) {
            println("EditTransaction: ERROR - Exception in loadMetadata: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    private suspend fun loadTransactionData(token: String): Boolean {
        return try {
            val endpoint = if (isIncome) "income" else "expenses"
            val url = "${ApiConfig.BASE_URL}/api/$endpoint/$transactionId"

            println("EditTransaction: GET $url")

            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("X-Session-Token", token)

            val responseCode = connection.responseCode
            println("EditTransaction: Response code: $responseCode")

            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                println("EditTransaction: Response received (${response.length} chars)")

                val jsonResponse = JSONObject(response)
                transactionData = jsonResponse.getJSONObject("data")

                println("EditTransaction: Transaction data keys: ${transactionData?.keys()?.asSequence()?.toList()}")
                true
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                println("EditTransaction: ERROR - $responseCode: $errorResponse")
                false
            }
        } catch (e: Exception) {
            println("EditTransaction: ERROR - Exception in loadTransactionData: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    private fun setupTransactionTypeDropdown() {
        val typeOptions = arrayOf("Income", "Expense")
        val typeAdapter = ArrayAdapter(this, R.layout.spinner_dropdown_item, typeOptions)
        actvType.setAdapter(typeAdapter)

        actvType.setOnItemClickListener { _, _, position, _ ->
            selectedTypeIndex = position
            isIncome = position == 0
            tvSourceRecipientLabel.text = if (isIncome) "Source *" else "Recipient *"
            tilSourceRecipient.hint = if (isIncome) "Select Source" else "Select Recipient"

            updateCategoryDropdown()
            updateSourceRecipientDropdown()
        }
    }

    private fun setupPaymentMethodDropdown() {
        val paymentMethods = arrayOf(
            "Cash", "Bank Transfer", "Credit Card", "Debit Card",
            "UPI", "Wallet", "Cheque", "Other"
        )
        val paymentAdapter = ArrayAdapter(this, R.layout.spinner_dropdown_item, paymentMethods)
        actvPaymentMethod.setAdapter(paymentAdapter)
    }

    private fun setupDateTimePickers() {
        val selectedDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        datePickerLayout.setOnClickListener {
            val calendar = Calendar.getInstance()
            if (selectedDate != null) {
                calendar.time = selectedDate!!
            }

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

        timePickerLayout.setOnClickListener {
            val calendar = Calendar.getInstance()
            android.app.TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    calendar.set(Calendar.MINUTE, minute)
                    calendar.set(Calendar.SECOND, 0)
                    selectedTime = timeFormat.format(calendar.time)

                    val hour12 = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
                    val amPm = if (hourOfDay < 12) "AM" else "PM"
                    tvSelectedTime.text = String.format("%02d:%02d %s", hour12, minute, amPm)
                    tvSelectedTime.setTextColor(resources.getColor(R.color.text_dark, null))
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
            ).show()
        }
    }

    private fun updateCategoryDropdown() {
        val categories = if (isIncome) incomeCategories else expenseCategories
        val categoryNames = categories.map { it.second }.toTypedArray()
        val categoryAdapter = ArrayAdapter(this, R.layout.spinner_dropdown_item, categoryNames)
        actvCategory.setAdapter(categoryAdapter)
    }

    private fun updateSourceRecipientDropdown() {
        val sourceRecipient = if (isIncome) sources else recipients
        val names = sourceRecipient.map { it.second }.toTypedArray()
        val srAdapter = ArrayAdapter(this, R.layout.spinner_dropdown_item, names)
        actvSourceRecipient.setAdapter(srAdapter)
    }

    private fun populateFields(data: JSONObject) {
        try {
            println("EditTransaction: Populating fields...")

            // Type
            actvType.setText(if (isIncome) "Income" else "Expense", false)

            // Amount
            etAmount.setText(data.getString("amount"))

            // Category
            val categoryName = data.optString("category_name", "")
            if (categoryName.isNotEmpty()) {
                actvCategory.setText(categoryName, false)
                println("EditTransaction: Set category: $categoryName")
            }

            // Source/Recipient
            val srName = if (isIncome) {
                data.optString("source_name", "")
            } else {
                data.optString("recipient_name", "")
            }
            if (srName.isNotEmpty()) {
                actvSourceRecipient.setText(srName, false)
                println("EditTransaction: Set source/recipient: $srName")
            }

            // Date
            val dateStr = data.getString("date")
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val displayDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            selectedDate = dateFormat.parse(dateStr)
            tvSelectedDate.text = displayDateFormat.format(selectedDate!!)
            tvSelectedDate.setTextColor(resources.getColor(R.color.text_dark, null))

            // Time
            val timeStr = data.getString("time")
            selectedTime = timeStr
            val timeParts = timeStr.split(":")
            if (timeParts.size >= 2) {
                val hour = timeParts[0].toInt()
                val minute = timeParts[1].toInt()
                val hour12 = if (hour % 12 == 0) 12 else hour % 12
                val amPm = if (hour < 12) "AM" else "PM"
                tvSelectedTime.text = String.format("%02d:%02d %s", hour12, minute, amPm)
                tvSelectedTime.setTextColor(resources.getColor(R.color.text_dark, null))
            }

            // Payment Method
            val paymentMethod = data.optString("payment_method", "")
            if (paymentMethod.isNotEmpty()) {
                val formatted = paymentMethod.replace("_", " ").split(" ")
                    .joinToString(" ") { it.capitalize(Locale.getDefault()) }
                actvPaymentMethod.setText(formatted, false)
            }

            // Transaction Reference
            etTransactionReference.setText(data.optString("transaction_reference", ""))

            // Tags
            val tagsFromApi = data.optString("tags", "")
            // If tags come as JSON array string like "[\"tag1\",\"tag2\"]", convert to "tag1,tag2"
            val tagsFormatted = if (tagsFromApi.startsWith("[") && tagsFromApi.endsWith("]")) {
                try {
                    val tagsArray = org.json.JSONArray(tagsFromApi)
                    (0 until tagsArray.length()).joinToString(",") { tagsArray.getString(it) }
                } catch (e: Exception) {
                    tagsFromApi // If parsing fails, use as-is
                }
            } else {
                tagsFromApi
            }
            etTags.setText(tagsFormatted)

            // Notes
            etNotes.setText(data.optString("notes", ""))

            println("EditTransaction: Fields populated successfully")

        } catch (e: Exception) {
            println("EditTransaction: ERROR - Exception in populateFields: ${e.message}")
            e.printStackTrace()
            Toast.makeText(this, "Error populating fields: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateTransaction() {
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
                val label = if (isIncome) "source" else "recipient"
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
                val categories = if (isIncome) incomeCategories else expenseCategories
                val sourceRecipient = if (isIncome) sources else recipients

                val categoryId = categories.find { it.second == categoryText }?.first
                val sourceRecipientId = sourceRecipient.find { it.second == sourceRecipientText }?.first

                if (categoryId == null || sourceRecipientId == null) {
                    Toast.makeText(this, "Invalid selection. Please try again.", Toast.LENGTH_SHORT).show()
                    return
                }

                val paymentMethod = paymentMethodText.lowercase().replace(" ", "_")
                val selectedDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                // Show loader
                progressBar.visibility = View.VISIBLE
                btnUpdate.isEnabled = false
                btnDelete.isEnabled = false

                submitUpdate(
                    amount = amount,
                    categoryId = categoryId,
                    sourceRecipientId = sourceRecipientId,
                    date = selectedDateFormat.format(selectedDate!!),
                    time = selectedTime!!,
                    paymentMethod = paymentMethod,
                    transactionReference = transactionReference,
                    tags = tagsInput,
                    notes = notes
                )
            }
        }
    }

    private fun submitUpdate(
        amount: String,
        categoryId: String,
        sourceRecipientId: String,
        date: String,
        time: String,
        paymentMethod: String,
        transactionReference: String,
        tags: String,
        notes: String
    ) {
        println("EditTransaction: Submitting update...")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = getSessionToken() ?: return@launch
                val endpoint = if (isIncome) "income" else "expenses"
                val url = "${ApiConfig.BASE_URL}/api/$endpoint/$transactionId"

                println("EditTransaction: PUT $url")

                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "PUT"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("X-Session-Token", token)
                connection.doOutput = true

                val jsonBody = JSONObject().apply {
                    put("date", date)
                    put("time", time)
                    put("amount", amount)
                    put("currency", "INR")
                    put("category_id", categoryId)
                    if (isIncome) {
                        put("source_id", sourceRecipientId)
                    } else {
                        put("recipient_id", sourceRecipientId)
                    }
                    put("payment_method", paymentMethod)
                    if (transactionReference.isNotEmpty()) {
                        put("transaction_reference", transactionReference)
                    }
                    if (notes.isNotEmpty()) {
                        put("notes", notes)
                    }
                    // Convert tags from comma-separated string to JSONArray
                    if (tags.isNotEmpty()) {
                        val tagsArray = org.json.JSONArray(tags.split(",").map { it.trim() }.filter { it.isNotEmpty() })
                        put("tags", tagsArray)
                    }
                }

                println("EditTransaction: Request body: $jsonBody")
                connection.outputStream.write(jsonBody.toString().toByteArray())

                val responseCode = connection.responseCode
                println("EditTransaction: Update response code: $responseCode")

                // Read error response on IO thread BEFORE switching to Main
                val errorMessage = if (responseCode != 200) {
                    connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                } else {
                    null
                }

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnUpdate.isEnabled = true
                    btnDelete.isEnabled = true

                    if (responseCode == 200) {
                        Toast.makeText(this@EditTransactionActivity, "Transaction updated successfully", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        println("EditTransaction: Update error: $errorMessage")
                        Toast.makeText(this@EditTransactionActivity, "Failed to update transaction: $errorMessage", Toast.LENGTH_LONG).show()
                    }
                }

            } catch (e: Exception) {
                println("EditTransaction: ERROR - Update exception: ${e.message}")
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnUpdate.isEnabled = true
                    btnDelete.isEnabled = true
                    Toast.makeText(this@EditTransactionActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteTransaction()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTransaction() {
        println("EditTransaction: Deleting transaction...")

        // Show loader
        progressBar.visibility = View.VISIBLE
        btnUpdate.isEnabled = false
        btnDelete.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = getSessionToken() ?: return@launch
                val endpoint = if (isIncome) "income" else "expenses"
                val url = "${ApiConfig.BASE_URL}/api/$endpoint/$transactionId"

                println("EditTransaction: DELETE $url")

                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "DELETE"
                connection.setRequestProperty("X-Session-Token", token)

                val responseCode = connection.responseCode
                println("EditTransaction: Delete response code: $responseCode")

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE

                    if (responseCode == 200) {
                        Toast.makeText(this@EditTransactionActivity, "Transaction deleted successfully", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        btnUpdate.isEnabled = true
                        btnDelete.isEnabled = true
                        Toast.makeText(this@EditTransactionActivity, "Failed to delete transaction", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                println("EditTransaction: ERROR - Delete exception: ${e.message}")
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnUpdate.isEnabled = true
                    btnDelete.isEnabled = true
                    Toast.makeText(this@EditTransactionActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getSessionToken(): String? {
        val sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        return sharedPref.getString("session_token", null)
    }
}