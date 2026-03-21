package com.example.personalwealthmanager.presentation.transactions

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalwealthmanager.domain.repository.RecipientRepository
import com.example.personalwealthmanager.domain.repository.SourceRepository
import com.example.personalwealthmanager.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val sourceRepository: SourceRepository,
    private val recipientRepository: RecipientRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TransactionDetailState())
    val state: StateFlow<TransactionDetailState> = _state.asStateFlow()

    fun loadTransaction(transactionId: String, isIncome: Boolean) {
        viewModelScope.launch {
            Log.d("TransactionVM", "loadTransaction: id=$transactionId, isIncome=$isIncome")
            _state.update { it.copy(isLoading = true, error = null) }

            val type = if (isIncome) "income" else "expense"

            // Load both metadata and transaction
            val metadataResult = transactionRepository.getMetadata()
            Log.d("TransactionVM", "Metadata result: success=${metadataResult.isSuccess}, failure=${metadataResult.isFailure}")

            val transactionResult = transactionRepository.getTransactionById(transactionId, isIncome)
            Log.d("TransactionVM", "Transaction result: success=${transactionResult.isSuccess}, failure=${transactionResult.isFailure}")

            // Process results
            val metadata = metadataResult.getOrNull()
            val transaction = transactionResult.getOrNull()

            if (metadata != null && transaction != null) {
                val categories = if (type == "income") {
                    metadata.incomeCategories
                } else {
                    metadata.expenseCategories
                }
                Log.d("TransactionVM", "Success: categories=${categories.size}, sources=${metadata.sources.size}, recipients=${metadata.recipients.size}")
                _state.update {
                    it.copy(
                        isLoading = false,
                        categories = categories,
                        expenseCategories = metadata.expenseCategories,
                        sources = metadata.sources,
                        recipients = metadata.recipients,
                        transaction = transaction
                    )
                }
            } else {
                val errorMsg = transactionResult.exceptionOrNull()?.message
                    ?: metadataResult.exceptionOrNull()?.message
                    ?: "Failed to load transaction"
                Log.e("TransactionVM", "Error: $errorMsg")
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = errorMsg
                    )
                }
            }
        }
    }

    fun loadMetadata(type: String) {
        viewModelScope.launch {
            Log.d("TransactionVM", "loadMetadata: type=$type")
            _state.update { it.copy(error = null) }

            transactionRepository.getMetadata()
                .onSuccess { metadata ->
                    val categories = if (type == "income") {
                        metadata.incomeCategories
                    } else {
                        metadata.expenseCategories
                    }
                    Log.d("TransactionVM", "Metadata loaded: categories=${categories.size}, sources=${metadata.sources.size}, recipients=${metadata.recipients.size}")

                    _state.update {
                        it.copy(
                            categories = categories,
                            expenseCategories = metadata.expenseCategories,
                            sources = metadata.sources,
                            recipients = metadata.recipients
                        )
                    }
                }
                .onFailure { error ->
                    Log.e("TransactionVM", "Metadata error: ${error.message}")
                    _state.update {
                        it.copy(error = error.message ?: "Failed to load metadata")
                    }
                }
        }
    }

    fun saveTransaction(
        type: String,
        date: String,
        time: String,
        amount: String,
        categoryId: String,
        sourceRecipientId: String,
        paymentMethod: String,
        transactionReference: String?,
        tags: List<String>?,
        notes: String?
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }

            val result = if (_state.value.transaction != null) {
                // Update existing
                transactionRepository.updateTransaction(
                    id = _state.value.transaction!!.transactionId,
                    type = type,
                    date = date,
                    time = time,
                    amount = amount,
                    categoryId = categoryId,
                    sourceRecipientId = sourceRecipientId,
                    paymentMethod = paymentMethod,
                    transactionReference = transactionReference,
                    tags = tags,
                    notes = notes
                )
            } else {
                // Create new
                transactionRepository.createTransaction(
                    type = type,
                    date = date,
                    time = time,
                    amount = amount,
                    categoryId = categoryId,
                    sourceRecipientId = sourceRecipientId,
                    paymentMethod = paymentMethod,
                    transactionReference = transactionReference,
                    tags = tags,
                    notes = notes
                )
            }

            result.onSuccess {
                _state.update { it.copy(isSaving = false, saveSuccess = true) }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isSaving = false,
                        error = error.message ?: "Failed to save transaction"
                    )
                }
            }
        }
    }

    fun deleteTransaction(transactionId: String, isIncome: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }

            transactionRepository.deleteTransaction(transactionId, isIncome)
                .onSuccess {
                    _state.update { it.copy(isSaving = false, saveSuccess = true) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isSaving = false,
                            error = error.message ?: "Failed to delete transaction"
                        )
                    }
                }
        }
    }

    fun createCategory(
        type: String,
        name: String,
        description: String?,
        icon: String?
    ) {
        viewModelScope.launch {
            Log.d("TransactionVM", "createCategory: type=$type, name=$name")
            _state.update {
                it.copy(
                    isCreatingCategory = true,
                    categoryCreationError = null,
                    categoryCreated = null
                )
            }

            transactionRepository.createCategory(type, name, description, icon)
                .onSuccess { category ->
                    Log.d("TransactionVM", "Category created: ${category.id}, ${category.name}")
                    val updatedCategories = _state.value.categories + category
                    val updatedExpenseCategories = if (type == "expense") {
                        _state.value.expenseCategories + category
                    } else {
                        _state.value.expenseCategories
                    }
                    _state.update {
                        it.copy(
                            isCreatingCategory = false,
                            categoryCreated = category,
                            categories = updatedCategories,
                            expenseCategories = updatedExpenseCategories
                        )
                    }
                }
                .onFailure { error ->
                    Log.e("TransactionVM", "Category creation error: ${error.message}")
                    _state.update {
                        it.copy(
                            isCreatingCategory = false,
                            categoryCreationError = error.message ?: "Failed to create category"
                        )
                    }
                }
        }
    }

    fun clearCategoryCreationState() {
        _state.update {
            it.copy(
                categoryCreated = null,
                categoryCreationError = null
            )
        }
    }

    fun createSource(name: String, description: String?, sourceIdentifier: String? = null, defaultCategoryId: String? = null) {
        viewModelScope.launch {
            _state.update {
                it.copy(isCreatingSource = true, sourceCreationError = null, sourceCreated = null)
            }
            sourceRepository.createSource(name, description, null, null, sourceIdentifier, defaultCategoryId)
                .onSuccess { source ->
                    val updatedSources = _state.value.sources + source
                    _state.update {
                        it.copy(isCreatingSource = false, sourceCreated = source, sources = updatedSources)
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isCreatingSource = false,
                            sourceCreationError = error.message ?: "Failed to create source"
                        )
                    }
                }
        }
    }

    fun clearSourceCreationState() {
        _state.update { it.copy(sourceCreated = null, sourceCreationError = null) }
    }

    fun createRecipient(name: String, description: String?, isFavorite: Boolean, defaultCategoryId: String? = null, paymentIdentifiers: List<String> = emptyList()) {
        viewModelScope.launch {
            _state.update {
                it.copy(isCreatingRecipient = true, recipientCreationError = null, recipientCreated = null)
            }
            recipientRepository.createRecipient(name, null, description, null, isFavorite, paymentIdentifiers, defaultCategoryId)
                .onSuccess { recipient ->
                    val updatedRecipients = _state.value.recipients + recipient
                    _state.update {
                        it.copy(isCreatingRecipient = false, recipientCreated = recipient, recipients = updatedRecipients)
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isCreatingRecipient = false,
                            recipientCreationError = error.message ?: "Failed to create recipient"
                        )
                    }
                }
        }
    }

    fun clearRecipientCreationState() {
        _state.update { it.copy(recipientCreated = null, recipientCreationError = null) }
    }
}
