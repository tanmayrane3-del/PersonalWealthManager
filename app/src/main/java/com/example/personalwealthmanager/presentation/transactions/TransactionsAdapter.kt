package com.example.personalwealthmanager.presentation.transactions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.personalwealthmanager.R
import com.example.personalwealthmanager.domain.model.Transaction
import java.text.NumberFormat
import java.util.Locale

class TransactionsAdapter(
    private var transactions: List<Transaction>,
    private val onItemClick: (Transaction) -> Unit,
    private val onItemLongClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionsAdapter.TransactionViewHolder>() {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cvCard: CardView = view as CardView
        val tvCategoryIcon: TextView = view.findViewById(R.id.tvCategoryIcon)
        val tvCategoryName: TextView = view.findViewById(R.id.tvCategoryName)
        val tvTransactionDetails: TextView = view.findViewById(R.id.tvTransactionDetails)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]

        // Set category icon from database, with fallback based on type
        holder.tvCategoryIcon.text = transaction.categoryIcon
            ?: if (transaction.type == "income") "💰" else "💸"

        // Set category name
        holder.tvCategoryName.text = transaction.categoryName

        // Set transaction details (date, payment method, source/recipient)
        val details = buildString {
            append(transaction.date)
            if (transaction.paymentMethod.isNotEmpty()) {
                append(" • ${transaction.paymentMethod}")
            }
            if (transaction.type == "income") {
                transaction.sourceName?.let { append(" • $it") }
            } else {
                transaction.recipientName?.let { append(" • $it") }
            }
        }
        holder.tvTransactionDetails.text = details

        // Set amount with prefix based on type
        val amount = transaction.amount.toDoubleOrNull() ?: 0.0
        val prefix = if (transaction.type == "income") "+ " else "− "
        holder.tvAmount.text = prefix + currencyFormat.format(amount)

        // Set amount color based on type
        val color = if (transaction.type == "income") {
            holder.itemView.context.getColor(R.color.income_green)
        } else {
            holder.itemView.context.getColor(R.color.expense_red)
        }
        holder.tvAmount.setTextColor(color)

        // Highlight in light red if category or recipient is "Other" (needs user review)
        val isOtherCategory = transaction.categoryName.equals("Other", ignoreCase = true)
        val isOtherRecipient = transaction.type == "expense" &&
            transaction.recipientName.equals("Other", ignoreCase = true)
        val cardBackgroundColor = if (isOtherCategory || isOtherRecipient) {
            holder.itemView.context.getColor(R.color.card_other_highlight)
        } else {
            holder.itemView.context.getColor(R.color.card_medium_teal)
        }
        holder.cvCard.setCardBackgroundColor(cardBackgroundColor)

        holder.itemView.setOnClickListener {
            onItemClick(transaction)
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClick(transaction)
            true
        }
    }

    override fun getItemCount() = transactions.size

    fun updateTransactions(newTransactions: List<Transaction>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }
}
