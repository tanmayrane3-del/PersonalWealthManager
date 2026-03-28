package com.example.personalwealthmanager.presentation.transactions

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.personalwealthmanager.R
import com.example.personalwealthmanager.domain.model.Transaction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class TransactionsAdapter(
    private var items: List<TransactionListItem> = emptyList(),
    private val onItemClick: (Transaction) -> Unit,
    private val onItemLongClick: (Transaction) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    sealed class TransactionListItem {
        data class Header(val displayDate: String, val netAmount: Double) : TransactionListItem()
        data class Item(val transaction: Transaction) : TransactionListItem()
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1

        fun buildListItems(transactions: List<Transaction>): List<TransactionListItem> {
            val result = mutableListOf<TransactionListItem>()
            val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val displayMonthDay = SimpleDateFormat("MMM d", Locale.getDefault())
            val dayOfWeek = SimpleDateFormat("EEE — MMM d", Locale.getDefault())

            val today = apiFormat.format(Date())
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            val yesterday = apiFormat.format(cal.time)

            val grouped = transactions.groupBy { it.date }
            val sortedDates = grouped.keys.sortedDescending()

            for (date in sortedDates) {
                val group = grouped[date] ?: continue

                val net = group.sumOf { t ->
                    val amt = t.amount.toDoubleOrNull() ?: 0.0
                    if (t.type == "income") amt else -amt
                }

                val parsedDate = runCatching { apiFormat.parse(date)!! }.getOrNull()
                val displayDate = when (date) {
                    today -> "TODAY — ${parsedDate?.let { displayMonthDay.format(it).uppercase() } ?: date}"
                    yesterday -> "YESTERDAY — ${parsedDate?.let { displayMonthDay.format(it).uppercase() } ?: date}"
                    else -> parsedDate?.let { dayOfWeek.format(it).uppercase() } ?: date
                }

                result.add(TransactionListItem.Header(displayDate, net))
                group.sortedByDescending { it.time }.forEach { t ->
                    result.add(TransactionListItem.Item(t))
                }
            }
            return result
        }
    }

    // ── ViewHolders ──────────────────────────────────────────────────────────────

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvHeaderDate: TextView = view.findViewById(R.id.tvHeaderDate)
        val tvHeaderNet: TextView = view.findViewById(R.id.tvHeaderNet)
    }

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconBackground: View = view.findViewById(R.id.iconBackground)
        val tvCategoryIcon: TextView = view.findViewById(R.id.tvCategoryIcon)
        val tvCategoryName: TextView = view.findViewById(R.id.tvCategoryName)
        val tvTransactionDetails: TextView = view.findViewById(R.id.tvTransactionDetails)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
    }

    // ── Adapter overrides ─────────────────────────────────────────────────────────

    override fun getItemViewType(position: Int) = when (items[position]) {
        is TransactionListItem.Header -> TYPE_HEADER
        is TransactionListItem.Item -> TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(
                inflater.inflate(R.layout.item_transaction_header, parent, false)
            )
            else -> ItemViewHolder(
                inflater.inflate(R.layout.item_transaction, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is TransactionListItem.Header -> bindHeader(holder as HeaderViewHolder, item)
            is TransactionListItem.Item -> bindItem(holder as ItemViewHolder, item.transaction)
        }
    }

    override fun getItemCount() = items.size

    // ── Bind helpers ──────────────────────────────────────────────────────────────

    private fun bindHeader(holder: HeaderViewHolder, header: TransactionListItem.Header) {
        holder.tvHeaderDate.text = header.displayDate

        val cf = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        val abs = Math.abs(header.netAmount)
        val prefix = if (header.netAmount >= 0) "+" else "−"
        holder.tvHeaderNet.text = "$prefix${cf.format(abs)}"

        val ctx = holder.itemView.context
        holder.tvHeaderNet.setTextColor(
            if (header.netAmount >= 0) ctx.getColor(R.color.tx_amount_income)
            else ctx.getColor(R.color.tx_text_dark)
        )
    }

    private fun bindItem(holder: ItemViewHolder, transaction: Transaction) {
        val ctx = holder.itemView.context

        // Icon emoji
        holder.tvCategoryIcon.text = transaction.categoryIcon
            ?: if (transaction.type == "income") "💰" else "💸"

        // Category name
        holder.tvCategoryName.text = transaction.categoryName

        // Subtitle: payment method • source or recipient
        val subtitle = buildString {
            if (transaction.paymentMethod.isNotEmpty()) append(transaction.paymentMethod)
            if (transaction.type == "income") {
                transaction.sourceName?.let { if (isNotEmpty()) append(" • "); append(it) }
            } else {
                transaction.recipientName?.let { if (isNotEmpty()) append(" • "); append(it) }
            }
        }
        holder.tvTransactionDetails.text = subtitle

        // Time
        holder.tvTime.text = formatTime(transaction.time)

        // Amount
        val cf = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        val amount = transaction.amount.toDoubleOrNull() ?: 0.0
        val prefix = if (transaction.type == "income") "+ " else "− "
        holder.tvAmount.text = prefix + cf.format(amount)

        val amountColor = if (transaction.type == "income")
            ctx.getColor(R.color.tx_amount_income)
        else
            ctx.getColor(R.color.tx_amount_expense)
        holder.tvAmount.setTextColor(amountColor)

        // Icon background color (rounded square)
        val isOther = transaction.categoryName.equals("Other", ignoreCase = true) ||
                (transaction.type == "expense" &&
                        transaction.recipientName.equals("Other", ignoreCase = true))

        val iconBgColor = when {
            isOther -> ctx.getColor(R.color.tx_icon_bg_other)
            transaction.type == "income" -> ctx.getColor(R.color.tx_icon_bg_income)
            else -> ctx.getColor(R.color.tx_icon_bg_expense)
        }
        val dp12 = ctx.resources.displayMetrics.density * 12f
        holder.iconBackground.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp12
            setColor(iconBgColor)
        }

        holder.itemView.setOnClickListener { onItemClick(transaction) }
        holder.itemView.setOnLongClickListener { onItemLongClick(transaction); true }
    }

    // ── Public update ─────────────────────────────────────────────────────────────

    fun updateTransactions(transactions: List<Transaction>) {
        items = buildListItems(transactions)
        notifyDataSetChanged()
    }

    // ── Time formatter ────────────────────────────────────────────────────────────

    private fun formatTime(raw: String): String {
        if (raw.isBlank()) return ""
        val tryFormats = listOf("HH:mm:ss", "HH:mm", "h:mm a", "hh:mm a")
        val outFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        for (fmt in tryFormats) {
            try {
                val parsed = SimpleDateFormat(fmt, Locale.getDefault()).parse(raw) ?: continue
                return outFormat.format(parsed)
            } catch (_: Exception) { }
        }
        return raw
    }
}
