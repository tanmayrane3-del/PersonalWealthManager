package com.example.personalwealthmanager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class TransactionsAdapter(
    private var transactions: List<Transaction>,
    private val onItemClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionsAdapter.TransactionViewHolder>() {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    private val inputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val outputDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
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

        // Set category icon (emoji)
        holder.tvCategoryIcon.text = transaction.categoryIcon ?: "💰"

        // Set category name
        holder.tvCategoryName.text = transaction.categoryName ?: "Unknown"

        // Format date
        val formattedDate = try {
            val date = inputDateFormat.parse(transaction.date)
            date?.let { outputDateFormat.format(it) } ?: transaction.date
        } catch (e: Exception) {
            transaction.date
        }

        // Build details string
        val details = buildString {
            append(formattedDate)
            transaction.paymentMethod?.let { append(" • $it") }
            if (transaction.transactionType == "income") {
                transaction.sourceName?.let { append(" • $it") }
            } else {
                transaction.recipientName?.let { append(" • $it") }
            }
        }
        holder.tvTransactionDetails.text = details

        // Set amount with color
        val formattedAmount = currencyFormat.format(transaction.amount)
        if (transaction.transactionType == "income") {
            holder.tvAmount.text = "+ $formattedAmount"
            holder.tvAmount.setTextColor(
                holder.itemView.context.getColor(R.color.amount_positive)
            )
        } else {
            holder.tvAmount.text = "− $formattedAmount"
            holder.tvAmount.setTextColor(
                holder.itemView.context.getColor(R.color.amount_negative)
            )
        }

        // Set click listener
        holder.itemView.setOnClickListener {
            onItemClick(transaction)
        }
    }

    override fun getItemCount() = transactions.size

    fun updateTransactions(newTransactions: List<Transaction>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }
}