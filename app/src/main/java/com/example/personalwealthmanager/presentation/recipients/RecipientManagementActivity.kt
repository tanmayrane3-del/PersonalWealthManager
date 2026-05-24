package com.pwm.personalwealthmanager.presentation.recipients

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pwm.personalwealthmanager.R
import com.pwm.personalwealthmanager.domain.model.Recipient
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RecipientManagementActivity : com.pwm.personalwealthmanager.presentation.base.BaseDrawerActivity() {

    override fun getSelfButtonId() = R.id.btnRecipientManagement

    private val viewModel: RecipientManagementViewModel by viewModels()

    private lateinit var progressBar: ProgressBar
    private lateinit var etSearch: EditText
    private lateinit var globalHeader: LinearLayout
    private lateinit var ivGlobalExpand: ImageView
    private lateinit var rvGlobalRecipients: RecyclerView
    private lateinit var tvNoGlobalRecipients: TextView
    private lateinit var tvGlobalCount: TextView
    private lateinit var userHeader: LinearLayout
    private lateinit var ivUserExpand: ImageView
    private lateinit var rvUserRecipients: RecyclerView
    private lateinit var tvNoUserRecipients: TextView
    private lateinit var tvUserCount: TextView

    private var isGlobalExpanded = true
    private var isUserExpanded = true

    private lateinit var globalAdapter: RecipientAdapter
    private lateinit var userAdapter: RecipientAdapter

    private var searchQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipient_management)

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
        globalHeader = findViewById(R.id.globalHeader)
        ivGlobalExpand = findViewById(R.id.ivGlobalExpand)
        rvGlobalRecipients = findViewById(R.id.rvGlobalRecipients)
        tvNoGlobalRecipients = findViewById(R.id.tvNoGlobalRecipients)
        tvGlobalCount = findViewById(R.id.tvGlobalCount)
        userHeader = findViewById(R.id.userHeader)
        ivUserExpand = findViewById(R.id.ivUserExpand)
        rvUserRecipients = findViewById(R.id.rvUserRecipients)
        tvNoUserRecipients = findViewById(R.id.tvNoUserRecipients)
        tvUserCount = findViewById(R.id.tvUserCount)
    }

    private fun setupRecyclerViews() {
        globalAdapter = RecipientAdapter(emptyList())
        rvGlobalRecipients.layoutManager = LinearLayoutManager(this)
        rvGlobalRecipients.adapter = globalAdapter

        userAdapter = RecipientAdapter(
            recipients = emptyList(),
            onItemClick = { recipient -> showEditRecipientDialog(recipient) }
        )
        rvUserRecipients.layoutManager = LinearLayoutManager(this)
        rvUserRecipients.adapter = userAdapter
    }

    private fun setupClickListeners() {
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<ImageView>(R.id.btnAdd).setOnClickListener { showCreateRecipientDialog() }

        findViewById<ImageView>(R.id.btnMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        globalHeader.setOnClickListener {
            isGlobalExpanded = !isGlobalExpanded
            setGlobalExpanded(isGlobalExpanded)
        }

        userHeader.setOnClickListener {
            isUserExpanded = !isUserExpanded
            setUserExpanded(isUserExpanded)
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

    private fun applySearchFilter(state: RecipientManagementState) {
        val allGlobal = state.recipients.filter { it.isGlobal }
        val allUser = state.recipients.filter { it.isUserSpecific }

        val filteredGlobal = if (searchQuery.isBlank()) allGlobal
        else allGlobal.filter { r ->
            r.name.contains(searchQuery, ignoreCase = true) ||
            r.description?.contains(searchQuery, ignoreCase = true) == true
        }

        val filteredUser = if (searchQuery.isBlank()) allUser
        else allUser.filter { r ->
            r.name.contains(searchQuery, ignoreCase = true) ||
            r.description?.contains(searchQuery, ignoreCase = true) == true
        }

        globalAdapter.updateRecipients(filteredGlobal)
        tvGlobalCount.text = "${filteredGlobal.size} TOTAL"
        setGlobalExpanded(isGlobalExpanded)

        userAdapter.updateRecipients(filteredUser)
        tvUserCount.text = "${filteredUser.size} TOTAL"
        setUserExpanded(isUserExpanded)
    }

    private fun addIdentifierChip(chipGroup: ChipGroup, identifier: String) {
        val chip = Chip(this)
        chip.text = identifier
        chip.isCloseIconVisible = true
        chip.chipBackgroundColor = ColorStateList.valueOf(
            ContextCompat.getColor(this, R.color.teal_500)
        )
        chip.setTextColor(Color.WHITE)
        chip.closeIconTint = ColorStateList.valueOf(Color.WHITE)
        chip.setOnCloseIconClickListener { chipGroup.removeView(chip) }
        chipGroup.addView(chip)
    }

    private fun getIdentifiersFromChipGroup(chipGroup: ChipGroup): List<String> =
        (0 until chipGroup.childCount).map { (chipGroup.getChildAt(it) as Chip).text.toString() }

    private fun showCreateRecipientDialog() {
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(R.layout.dialog_edit_recipient)

        val tvDialogTitle = dialog.findViewById<TextView>(R.id.tvDialogTitle)!!
        val etRecipientName = dialog.findViewById<TextInputEditText>(R.id.etRecipientName)!!
        val etRecipientDescription = dialog.findViewById<TextInputEditText>(R.id.etRecipientDescription)!!
        val cbFavorite = dialog.findViewById<CheckBox>(R.id.cbFavorite)!!
        val chipGroupIdentifiers = dialog.findViewById<ChipGroup>(R.id.chipGroupIdentifiers)!!
        val etPaymentIdentifier = dialog.findViewById<TextInputEditText>(R.id.etPaymentIdentifier)!!
        val btnAddIdentifier = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAddIdentifier)!!
        val spinnerDefaultCategory = dialog.findViewById<Spinner>(R.id.spinnerDefaultCategory)!!
        val progressBar = dialog.findViewById<ProgressBar>(R.id.progressBar)!!
        val tvError = dialog.findViewById<TextView>(R.id.tvError)!!
        val buttonsLayout = dialog.findViewById<LinearLayout>(R.id.buttonsLayout)!!
        val btnCancel = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)!!
        val btnSave = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSave)!!
        val btnDelete = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDelete)!!
        val btnCloseDialog = dialog.findViewById<ImageView>(R.id.btnCloseDialog)

        tvDialogTitle.text = getString(R.string.add_recipient)
        btnDelete.visibility = View.GONE

        val categories = viewModel.state.value.expenseCategories
        val categoryNames = mutableListOf("-- None (use Other) --") + categories.map { it.name }
        val categorySpinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryNames)
        categorySpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDefaultCategory.adapter = categorySpinnerAdapter

        btnAddIdentifier.setOnClickListener {
            val id = etPaymentIdentifier.text.toString().trim()
            if (id.isEmpty()) return@setOnClickListener
            if (chipGroupIdentifiers.childCount >= 5) {
                tvError.text = "Maximum 5 payment identifiers allowed"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            addIdentifierChip(chipGroupIdentifiers, id)
            etPaymentIdentifier.setText("")
            tvError.visibility = View.GONE
        }

        btnCloseDialog?.setOnClickListener { dialog.dismiss() }
        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val name = etRecipientName.text.toString().trim()
            val description = etRecipientDescription.text.toString().trim().ifEmpty { null }
            val isFavorite = cbFavorite.isChecked
            val paymentIdentifiers = getIdentifiersFromChipGroup(chipGroupIdentifiers)
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

            viewModel.createRecipient(name, null, description, null, isFavorite, paymentIdentifiers, defaultCategoryId)

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

        showExpandedBottomSheet(dialog)
    }

    private fun showEditRecipientDialog(recipient: Recipient) {
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(R.layout.dialog_edit_recipient)

        val etRecipientName = dialog.findViewById<TextInputEditText>(R.id.etRecipientName)!!
        val etRecipientDescription = dialog.findViewById<TextInputEditText>(R.id.etRecipientDescription)!!
        val cbFavorite = dialog.findViewById<CheckBox>(R.id.cbFavorite)!!
        val chipGroupIdentifiers = dialog.findViewById<ChipGroup>(R.id.chipGroupIdentifiers)!!
        val etPaymentIdentifier = dialog.findViewById<TextInputEditText>(R.id.etPaymentIdentifier)!!
        val btnAddIdentifier = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAddIdentifier)!!
        val spinnerDefaultCategory = dialog.findViewById<Spinner>(R.id.spinnerDefaultCategory)!!
        val tvTransactionWarning = dialog.findViewById<TextView>(R.id.tvTransactionWarning)!!
        val progressBar = dialog.findViewById<ProgressBar>(R.id.progressBar)!!
        val tvError = dialog.findViewById<TextView>(R.id.tvError)!!
        val buttonsLayout = dialog.findViewById<LinearLayout>(R.id.buttonsLayout)!!
        val btnCancel = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)!!
        val btnSave = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSave)!!
        val btnDelete = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDelete)!!
        val btnCloseDialog = dialog.findViewById<ImageView>(R.id.btnCloseDialog)

        etRecipientName.setText(recipient.name)
        etRecipientDescription.setText(recipient.description ?: "")
        cbFavorite.isChecked = recipient.isFavorite
        recipient.paymentIdentifiers.forEach { addIdentifierChip(chipGroupIdentifiers, it) }

        btnAddIdentifier.setOnClickListener {
            val id = etPaymentIdentifier.text.toString().trim()
            if (id.isEmpty()) return@setOnClickListener
            if (chipGroupIdentifiers.childCount >= 5) {
                tvError.text = "Maximum 5 payment identifiers allowed"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            addIdentifierChip(chipGroupIdentifiers, id)
            etPaymentIdentifier.setText("")
            tvError.visibility = View.GONE
        }

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

        if (recipient.transactionCount > 0) {
            tvTransactionWarning.text = getString(R.string.recipient_has_transactions, recipient.transactionCount)
            tvTransactionWarning.visibility = View.VISIBLE
            btnDelete.visibility = View.GONE
        } else {
            tvTransactionWarning.visibility = View.GONE
            btnDelete.visibility = View.VISIBLE
        }

        btnCloseDialog?.setOnClickListener { dialog.dismiss() }
        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val name = etRecipientName.text.toString().trim()
            val description = etRecipientDescription.text.toString().trim().ifEmpty { null }
            val isFavorite = cbFavorite.isChecked
            val paymentIdentifiers = getIdentifiersFromChipGroup(chipGroupIdentifiers)
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

            viewModel.updateRecipient(recipient.id, name, null, description, null, isFavorite, paymentIdentifiers, defaultCategoryId)

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
                        if (recipient.transactionCount == 0) btnDelete.visibility = View.VISIBLE
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

        showExpandedBottomSheet(dialog)
    }

    private fun showExpandedBottomSheet(dialog: BottomSheetDialog) {
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { view ->
                val lp = view.layoutParams
                lp.height = (resources.displayMetrics.heightPixels * 0.70).toInt()
                view.layoutParams = lp
                BottomSheetBehavior.from(view).apply {
                    state = BottomSheetBehavior.STATE_EXPANDED
                    skipCollapsed = true
                }
            }
        }
        dialog.show()
    }

    private fun setGlobalExpanded(expanded: Boolean) {
        val contentVisibility = if (expanded) View.VISIBLE else View.GONE
        rvGlobalRecipients.visibility = contentVisibility
        tvNoGlobalRecipients.visibility = if (expanded && globalAdapter.itemCount == 0) View.VISIBLE else View.GONE
        ivGlobalExpand.setImageResource(if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more)
    }

    private fun setUserExpanded(expanded: Boolean) {
        val contentVisibility = if (expanded) View.VISIBLE else View.GONE
        rvUserRecipients.visibility = contentVisibility
        tvNoUserRecipients.visibility = if (expanded && userAdapter.itemCount == 0) View.VISIBLE else View.GONE
        ivUserExpand.setImageResource(if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more)
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                applySearchFilter(state)

                state.error?.let { error ->
                    if (!state.isUpdating && !state.isDeleting && !state.isCreating) {
                        Toast.makeText(this@RecipientManagementActivity, error, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
