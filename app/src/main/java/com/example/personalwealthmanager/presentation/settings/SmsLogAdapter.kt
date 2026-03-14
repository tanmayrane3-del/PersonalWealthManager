package com.example.personalwealthmanager.presentation.settings

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.personalwealthmanager.R
import com.example.personalwealthmanager.core.sms.queue.SmsQueueEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SmsLogAdapter(
    private var items: List<SmsQueueEntity>,
    private val onRetry: (SmsQueueEntity) -> Unit
) : RecyclerView.Adapter<SmsLogAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSender: TextView = view.findViewById(R.id.tvSender)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvBody: TextView = view.findViewById(R.id.tvBody)
        val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
        val tvError: TextView = view.findViewById(R.id.tvError)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_sms_log, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val ctx = holder.itemView.context

        holder.tvSender.text = item.sender
        holder.tvBody.text = item.body
        holder.tvTimestamp.text = dateFormat.format(Date(item.enqueuedAt))

        val (label, colorRes) = when (item.status) {
            "SUCCESS" -> "SUCCESS" to R.color.income_green
            "SKIPPED" -> "SKIPPED" to R.color.teal_500
            "FAILED"  -> "FAILED"  to R.color.error_red
            else      -> "PENDING" to R.color.warning_orange
        }
        holder.tvStatus.text = label
        holder.tvStatus.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(ctx, colorRes))

        if (!item.errorMessage.isNullOrBlank()) {
            holder.tvError.text = item.errorMessage
            holder.tvError.visibility = View.VISIBLE
        } else {
            holder.tvError.visibility = View.GONE
        }

        // Tap to retry on FAILED or PENDING cards
        if (item.status == "FAILED" || item.status == "PENDING") {
            holder.itemView.alpha = 1f
            holder.itemView.setOnClickListener { onRetry(item) }
        } else {
            holder.itemView.alpha = 0.85f
            holder.itemView.setOnClickListener(null)
        }
    }

    fun updateItems(newItems: List<SmsQueueEntity>) {
        items = newItems
        notifyDataSetChanged()
    }
}
