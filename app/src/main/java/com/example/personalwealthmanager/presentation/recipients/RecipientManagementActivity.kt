package com.example.personalwealthmanager.presentation.recipients

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
import com.example.personalwealthmanager.domain.model.Recipient
import com.example.personalwealthmanager.presentation.categories.CategoryManagementActivity
import com.example.personalwealthmanager.presentation.sources.SourceManagementActivity
import com.example.personalwealthmanager.presentation.transactions.TransactionsActivity
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RecipientManagementActivity : AppCompatActivity() {

    private val viewModel: RecipientManagementViewModel by viewModels()

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var progressBar: ProgressBar

    // Section views
    private lateinit var globalHeader: LinearLayout
    private lateinit var globalContent: LinearLayout
    private lateinit var ivGlobalExpand: ImageView
    private lateinit var rvGlobalRecipients: RecyclerView
    private lateinit var tvNoGlobalRecipients: TextView

    private lateinit var userHeader: LinearLayout
    private lateinit var userContent: LinearLayout
    private lateinit var ivUserExpand: ImageView
    private lateinit var rvUserRecipients: RecyclerView
    private lateinit var tvNoUserRecipients: TextView

    // Adapters
    private lateinit var globalAdapter: RecipientAdapter
    private lateinit var userAdapter: RecipientAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipient_management)

        initializeViews()
        setupRecyclerViews()
        setupClickListeners()
        setupNavigationDrawer()
        observeState()
    }

    private fun initializeViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        progressBar = findViewById(R.id.progressBar)

        // Global section
        globalHeader = findViewById(R.id.globalHeader)
        globalContent = findViewById(R.id.globalContent)
        ivGlobalExpand = findViewById(R.id.ivGlobalExpand)
        rvGlobalRecipients = findViewById(R.id.rvGlobalRecipients)
        tvNoGlobalRecipients = findViewById(R.id.tvNoGlobalRecipients)

        // User section
        userHeader = findViewById(R.id.userHeader)
        userContent = findViewById(R.id.userContent)
        ivUserExpand = findViewById(R.id.ivUserExpand)
        rvUserRecipients = findViewById(R.id.rvUserRecipients)
        tvNoUserRecipients = findViewById(R.id.tvNoUserRecipients)
    }

    private fun setupRecyclerViews() {
        // Global Recipients - no edit button
        globalAdapter = RecipientAdapter(
            recipients = emptyList(),
            showEditButton = false
        )
        rvGlobalRecipients.layoutManager = LinearLayoutManager(this)
        rvGlobalRecipients.adapter = globalAdapter

        // User Recipients - with edit button
        userAdapter = RecipientAdapter(
            recipients = emptyList(),
            showEditButton = true,
            onEditClick = { recipient ->
                showEditRecipientDialog(recipient)
            }
        )
        rvUserRecipients.layoutManager = LinearLayoutManager(this)
        rvUserRecipients.adapter = userAdapter
    }

    private fun setupClickListeners() {
        // Back button
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Add button
        findViewById<ImageView>(R.id.btnAdd).setOnClickListener {
            showCreateRecipientDialog()
        }

        // Menu button
        findViewById<ImageView>(R.id.btnMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        // Global section collapse/expand
        globalHeader.setOnClickListener {
            viewModel.toggleGlobalSection()
        }

        // User section collapse/expand
        userHeader.setOnClickListener {
            viewModel.toggleUserSection()
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

        // Set initial state - expanded
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

        // Category management button
        headerView.findViewById<Button>(R.id.btnCategoryManagement)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            val intent = Intent(this, CategoryManagementActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Logout button
        headerView.findViewById<Button>(R.id.btnLogout)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            sharedPrefs.edit().clear().apply()
            val intent = Intent(this, com.example.personalwealthmanager.presentation.auth.login.LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun showCreateRecipientDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_edit_recipient)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val tvDialogTitle = dialog.findViewById<TextView>(R.id.tvDialogTitle)
        val etRecipientName = dialog.findViewById<TextInputEditText>(R.id.etRecipientName)
        val etRecipientDescription = dialog.findViewById<TextInputEditText>(R.id.etRecipientDescription)
        val cbFavorite = dialog.findViewById<CheckBox>(R.id.cbFavorite)
        val etPaymentIdentifier = dialog.findViewById<TextInputEditText>(R.id.etPaymentIdentifier)
        val spinnerDefaultCategory = dialog.findViewById<Spinner>(R.id.spinnerDefaultCategory)
        val progressBar = dialog.findViewById<ProgressBar>(R.id.progressBar)
        val tvError = dialog.findViewById<TextView>(R.id.tvError)
        val buttonsLayout = dialog.findViewById<LinearLayout>(R.id.buttonsLayout)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialog.findViewById<Button>(R.id.btnSave)
        val btnDelete = dialog.findViewById<Button>(R.id.btnDelete)
        val btnCloseDialog = dialog.findViewById<ImageView>(R.id.btnCloseDialog)

        tvDialogTitle.text = getString(R.string.add_recipient)
        btnDelete.visibility = View.GONE

        // Populate category spinner
        val categories = viewModel.state.value.expenseCategories
        val categoryNames = mutableListOf("-- None (use Other) --") + categories.map { it.name }
        val categorySpinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryNames)
        categorySpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDefaultCategory.adapter = categorySpinnerAdapter

        btnCloseDialog.setOnClickListener { dialog.dismiss() }
        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val name = etRecipientName.text.toString().trim()
            val description = etRecipientDescription.text.toString().trim().ifEmpty { null }
            val isFavorite = cbFavorite.isChecked
            val paymentIdentifier = etPaymentIdentifier.text.toString().trim().ifEmpty { null }
            val selectedPos = spinnerDefaultCategory.selectedItemPosition
            val defaultCategoryId = if (selectedPos > 0) categories[selectedPos - 1].id else null

            if (name.isEmpty()) {
                tvError.text = getString(R.string.name_required)
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            tvError.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
            buttonsLayout.visibility = View.GONE

            viewModel.createRecipient(name, null, description, null, isFavorite, paymentIdentifier, defaultCategoryId)

            lifecycleScope.launch {
                viewModel.state.collect { state ->
                    if (state.createSuccess) {
                        viewModel.clearSuccessStates()
                        dialog.dismiss()
                        Toast.makeText(this@RecipientManagementActivity, R.string.recipient_created, Toast.LENGTH_SHORT).show()
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

        dialog.show()
    }

    private fun showEditRecipientDialog(recipient: Recipient) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_edit_recipient)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val etRecipientName = dialog.findViewById<TextInputEditText>(R.id.etRecipientName)
        val etRecipientDescription = dialog.findViewById<TextInputEditText>(R.id.etRecipientDescription)
        val cbFavorite = dialog.findViewById<CheckBox>(R.id.cbFavorite)
        val etPaymentIdentifier = dialog.findViewById<TextInputEditText>(R.id.etPaymentIdentifier)
        val spinnerDefaultCategory = dialog.findViewById<Spinner>(R.id.spinnerDefaultCategory)
        val tvTransactionWarning = dialog.findViewById<TextView>(R.id.tvTransactionWarning)
        val progressBar = dialog.findViewById<ProgressBar>(R.id.progressBar)
        val tvError = dialog.findViewById<TextView>(R.id.tvError)
        val buttonsLayout = dialog.findViewById<LinearLayout>(R.id.buttonsLayout)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialog.findViewById<Button>(R.id.btnSave)
        val btnDelete = dialog.findViewById<Button>(R.id.btnDelete)
        val btnCloseDialog = dialog.findViewById<ImageView>(R.id.btnCloseDialog)

        // Pre-populate fields
        etRecipientName.setText(recipient.name)
        etRecipientDescription.setText(recipient.description ?: "")
        cbFavorite.isChecked = recipient.isFavorite
        etPaymentIdentifier.setText(recipient.paymentIdentifier ?: "")

        // Populate category spinner and pre-select based on recipient's defaultCategoryId
        val categories = viewModel.state.value.expenseCategories
        val categoryNames = mutableListOf("-- None (use Other) --") + categories.map { it.name }
        val categorySpinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryNames)
        categorySpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDefaultCategory.adapter = categorySpinnerAdapter
        val preSelectedIndex = if (recipient.defaultCategoryId != null) {
            val idx = categories.indexOfFirst { it.id == recipient.defaultCategoryId }
            if (idx >= 0) idx + 1 else 0
        } else 0
        spinnerDefaultCategory.setSelection(preSelectedIndex)

        // Show transaction warning or delete button based on transaction count
        if (recipient.transactionCount > 0) {
            tvTransactionWarning.text = getString(R.string.recipient_has_transactions, recipient.transactionCount)
            tvTransactionWarning.visibility = View.VISIBLE
            btnDelete.visibility = View.GONE
        } else {
            tvTransactionWarning.visibility = View.GONE
            btnDelete.visibility = View.VISIBLE
        }

        btnCloseDialog.setOnClickListener { dialog.dismiss() }
        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val name = etRecipientName.text.toString().trim()
            val description = etRecipientDescription.text.toString().trim().ifEmpty { null }
            val isFavorite = cbFavorite.isChecked
            val paymentIdentifier = etPaymentIdentifier.text.toString().trim().ifEmpty { null }
            val selectedPos = spinnerDefaultCategory.selectedItemPosition
            val defaultCategoryId = if (selectedPos > 0) categories[selectedPos - 1].id else null

            if (name.isEmpty()) {
                tvError.text = getString(R.string.name_required)
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            tvError.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
            buttonsLayout.visibility = View.GONE
            btnDelete.visibility = View.GONE

            viewModel.updateRecipient(recipient.id, name, null, description, null, isFavorite, paymentIdentifier, defaultCategoryId)

            lifecycleScope.launch {
                viewModel.state.collect { state ->
                    if (state.updateSuccess) {
                        viewModel.clearSuccessStates()
                        dialog.dismiss()
                        Toast.makeText(this@RecipientManagementActivity, R.string.recipient_updated, Toast.LENGTH_SHORT).show()
                        return@collect
                    }
                    if (!state.isUpdating && state.error != null) {
                        progressBar.visibility = View.GONE
                        buttonsLayout.visibility = View.VISIBLE
                        if (recipient.transactionCount == 0) {
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
                .setTitle(R.string.delete_recipient)
                .setMessage(R.string.delete_recipient_confirm)
                .setPositiveButton(R.string.delete) { _, _ ->
                    progressBar.visibility = View.VISIBLE
                    buttonsLayout.visibility = View.GONE
                    btnDelete.visibility = View.GONE

                    viewModel.deleteRecipient(recipient.id)

                    lifecycleScope.launch {
                        viewModel.state.collect { state ->
                            if (state.deleteSuccess) {
                                viewModel.clearSuccessStates()
                                dialog.dismiss()
                                Toast.makeText(this@RecipientManagementActivity, R.string.recipient_deleted, Toast.LENGTH_SHORT).show()
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

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                // Update Global section
                updateGlobalSectionVisibility(state.isGlobalExpanded)
                val globalRecipients = state.recipients.filter { it.isGlobal }
                globalAdapter.updateRecipients(globalRecipients)
                tvNoGlobalRecipients.visibility = if (globalRecipients.isEmpty()) View.VISIBLE else View.GONE

                // Update User section
                updateUserSectionVisibility(state.isUserExpanded)
                val userRecipients = state.recipients.filter { it.isUserSpecific }
                userAdapter.updateRecipients(userRecipients)
                tvNoUserRecipients.visibility = if (userRecipients.isEmpty()) View.VISIBLE else View.GONE

                // Show error if any
                state.error?.let { error ->
                    if (!state.isUpdating && !state.isDeleting && !state.isCreating) {
                        Toast.makeText(this@RecipientManagementActivity, error, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun updateGlobalSectionVisibility(isExpanded: Boolean) {
        globalContent.visibility = if (isExpanded) View.VISIBLE else View.GONE
        ivGlobalExpand.setImageResource(
            if (isExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
        )
    }

    private fun updateUserSectionVisibility(isExpanded: Boolean) {
        userContent.visibility = if (isExpanded) View.VISIBLE else View.GONE
        ivUserExpand.setImageResource(
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
