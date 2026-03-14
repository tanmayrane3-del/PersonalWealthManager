package com.example.personalwealthmanager.presentation.sources

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
import com.example.personalwealthmanager.domain.model.Source
import com.example.personalwealthmanager.presentation.categories.CategoryManagementActivity
import com.example.personalwealthmanager.presentation.recipients.RecipientManagementActivity
import com.example.personalwealthmanager.presentation.transactions.TransactionsActivity
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SourceManagementActivity : AppCompatActivity() {

    private val viewModel: SourceManagementViewModel by viewModels()

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var progressBar: ProgressBar

    // Section views
    private lateinit var globalHeader: LinearLayout
    private lateinit var globalContent: LinearLayout
    private lateinit var ivGlobalExpand: ImageView
    private lateinit var rvGlobalSources: RecyclerView
    private lateinit var tvNoGlobalSources: TextView

    private lateinit var userHeader: LinearLayout
    private lateinit var userContent: LinearLayout
    private lateinit var ivUserExpand: ImageView
    private lateinit var rvUserSources: RecyclerView
    private lateinit var tvNoUserSources: TextView

    // Adapters
    private lateinit var globalAdapter: SourceAdapter
    private lateinit var userAdapter: SourceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_source_management)

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
        rvGlobalSources = findViewById(R.id.rvGlobalSources)
        tvNoGlobalSources = findViewById(R.id.tvNoGlobalSources)

        // User section
        userHeader = findViewById(R.id.userHeader)
        userContent = findViewById(R.id.userContent)
        ivUserExpand = findViewById(R.id.ivUserExpand)
        rvUserSources = findViewById(R.id.rvUserSources)
        tvNoUserSources = findViewById(R.id.tvNoUserSources)
    }

    private fun setupRecyclerViews() {
        // Global Sources - no edit button
        globalAdapter = SourceAdapter(
            sources = emptyList(),
            showEditButton = false
        )
        rvGlobalSources.layoutManager = LinearLayoutManager(this)
        rvGlobalSources.adapter = globalAdapter

        // User Sources - with edit button
        userAdapter = SourceAdapter(
            sources = emptyList(),
            showEditButton = true,
            onEditClick = { source ->
                showEditSourceDialog(source)
            }
        )
        rvUserSources.layoutManager = LinearLayoutManager(this)
        rvUserSources.adapter = userAdapter
    }

    private fun setupClickListeners() {
        // Back button
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Add button
        findViewById<ImageView>(R.id.btnAdd).setOnClickListener {
            showCreateSourceDialog()
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

        headerView.findViewById<Button>(R.id.btnSettings)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, com.example.personalwealthmanager.presentation.settings.SettingsActivity::class.java))
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

    private fun showCreateSourceDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_edit_source)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val tvDialogTitle = dialog.findViewById<TextView>(R.id.tvDialogTitle)
        val etSourceName = dialog.findViewById<TextInputEditText>(R.id.etSourceName)
        val etSourceDescription = dialog.findViewById<TextInputEditText>(R.id.etSourceDescription)
        val etSourceIdentifier = dialog.findViewById<TextInputEditText>(R.id.etSourceIdentifier)
        val spinnerDefaultCategory = dialog.findViewById<Spinner>(R.id.spinnerDefaultCategory)
        val progressBar = dialog.findViewById<ProgressBar>(R.id.progressBar)
        val tvError = dialog.findViewById<TextView>(R.id.tvError)
        val buttonsLayout = dialog.findViewById<LinearLayout>(R.id.buttonsLayout)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialog.findViewById<Button>(R.id.btnSave)
        val btnDelete = dialog.findViewById<Button>(R.id.btnDelete)
        val btnCloseDialog = dialog.findViewById<ImageView>(R.id.btnCloseDialog)

        tvDialogTitle.text = getString(R.string.add_source)
        btnDelete.visibility = View.GONE

        // Populate income category spinner
        val incomeCategories = viewModel.state.value.incomeCategories
        setupCategorySpinner(spinnerDefaultCategory, incomeCategories)

        btnCloseDialog.setOnClickListener { dialog.dismiss() }
        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val name = etSourceName.text.toString().trim()
            val description = etSourceDescription.text.toString().trim().ifEmpty { null }
            val sourceIdentifier = etSourceIdentifier.text.toString().trim().ifEmpty { null }
            val selectedPos = spinnerDefaultCategory.selectedItemPosition
            val defaultCategoryId = if (selectedPos > 0) incomeCategories[selectedPos - 1].id else null

            if (name.isEmpty()) {
                tvError.text = getString(R.string.name_required)
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            tvError.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
            buttonsLayout.visibility = View.GONE

            viewModel.createSource(name, description, null, null, sourceIdentifier, defaultCategoryId)

            lifecycleScope.launch {
                viewModel.state.collect { state ->
                    if (state.createSuccess) {
                        viewModel.clearSuccessStates()
                        dialog.dismiss()
                        Toast.makeText(this@SourceManagementActivity, R.string.source_created, Toast.LENGTH_SHORT).show()
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

    private fun showEditSourceDialog(source: Source) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_edit_source)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val etSourceName = dialog.findViewById<TextInputEditText>(R.id.etSourceName)
        val etSourceDescription = dialog.findViewById<TextInputEditText>(R.id.etSourceDescription)
        val etSourceIdentifier = dialog.findViewById<TextInputEditText>(R.id.etSourceIdentifier)
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
        etSourceName.setText(source.name)
        etSourceDescription.setText(source.description ?: "")
        etSourceIdentifier.setText(source.sourceIdentifier ?: "")

        // Populate income category spinner and pre-select existing value
        val incomeCategories = viewModel.state.value.incomeCategories
        setupCategorySpinner(spinnerDefaultCategory, incomeCategories)
        val preselectedIndex = incomeCategories.indexOfFirst { it.id == source.defaultCategoryId }
        if (preselectedIndex >= 0) spinnerDefaultCategory.setSelection(preselectedIndex + 1)

        // Show transaction warning or delete button based on transaction count
        if (source.transactionCount > 0) {
            tvTransactionWarning.text = getString(R.string.source_has_transactions, source.transactionCount)
            tvTransactionWarning.visibility = View.VISIBLE
            btnDelete.visibility = View.GONE
        } else {
            tvTransactionWarning.visibility = View.GONE
            btnDelete.visibility = View.VISIBLE
        }

        btnCloseDialog.setOnClickListener { dialog.dismiss() }
        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val name = etSourceName.text.toString().trim()
            val description = etSourceDescription.text.toString().trim().ifEmpty { null }
            val sourceIdentifier = etSourceIdentifier.text.toString().trim().ifEmpty { null }
            val selectedPos = spinnerDefaultCategory.selectedItemPosition
            val defaultCategoryId = if (selectedPos > 0) incomeCategories[selectedPos - 1].id else null

            if (name.isEmpty()) {
                tvError.text = getString(R.string.name_required)
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            tvError.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
            buttonsLayout.visibility = View.GONE
            btnDelete.visibility = View.GONE

            viewModel.updateSource(source.id, name, description, null, null, sourceIdentifier, defaultCategoryId)

            lifecycleScope.launch {
                viewModel.state.collect { state ->
                    if (state.updateSuccess) {
                        viewModel.clearSuccessStates()
                        dialog.dismiss()
                        Toast.makeText(this@SourceManagementActivity, R.string.source_updated, Toast.LENGTH_SHORT).show()
                        return@collect
                    }
                    if (!state.isUpdating && state.error != null) {
                        progressBar.visibility = View.GONE
                        buttonsLayout.visibility = View.VISIBLE
                        if (source.transactionCount == 0) {
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
                .setTitle(R.string.delete_source)
                .setMessage(R.string.delete_source_confirm)
                .setPositiveButton(R.string.delete) { _, _ ->
                    progressBar.visibility = View.VISIBLE
                    buttonsLayout.visibility = View.GONE
                    btnDelete.visibility = View.GONE

                    viewModel.deleteSource(source.id)

                    lifecycleScope.launch {
                        viewModel.state.collect { state ->
                            if (state.deleteSuccess) {
                                viewModel.clearSuccessStates()
                                dialog.dismiss()
                                Toast.makeText(this@SourceManagementActivity, R.string.source_deleted, Toast.LENGTH_SHORT).show()
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
                val globalSources = state.sources.filter { it.isGlobal }
                globalAdapter.updateSources(globalSources)
                tvNoGlobalSources.visibility = if (globalSources.isEmpty()) View.VISIBLE else View.GONE

                // Update User section
                updateUserSectionVisibility(state.isUserExpanded)
                val userSources = state.sources.filter { it.isUserSpecific }
                userAdapter.updateSources(userSources)
                tvNoUserSources.visibility = if (userSources.isEmpty()) View.VISIBLE else View.GONE

                // Show error if any
                state.error?.let { error ->
                    if (!state.isUpdating && !state.isDeleting && !state.isCreating) {
                        Toast.makeText(this@SourceManagementActivity, error, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun setupCategorySpinner(spinner: Spinner, categories: List<Category>) {
        val categoryNames = mutableListOf("-- None --") + categories.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
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
