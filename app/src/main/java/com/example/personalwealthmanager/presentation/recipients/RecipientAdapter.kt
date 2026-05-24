package com.pwm.personalwealthmanager.presentation.recipients

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pwm.personalwealthmanager.R
import com.pwm.personalwealthmanager.domain.model.Recipient
import kotlin.math.abs

class RecipientAdapter(
    private var recipients: List<Recipient>,
    private val onItemClick: ((Recipient) -> Unit)? = null
) : RecyclerView.Adapter<RecipientAdapter.RecipientViewHolder>() {

    private val iconColors = intArrayOf(
        0xFF2196F3.toInt(), // Blue
        0xFF9C27B0.toInt(), // Purple
        0xFFE91E63.toInt(), // Pink
        0xFFFF5722.toInt(), // Deep Orange
        0xFF00BCD4.toInt(), // Cyan
        0xFF4CAF50.toInt(), // Green
        0xFFFF9800.toInt(), // Orange
        0xFF795548.toInt(), // Brown
        0xFF607D8B.toInt(), // Blue Grey
        0xFFE53935.toInt(), // Red
    )

    class RecipientViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRecipientIcon: TextView = view.findViewById(R.id.tvRecipientIcon)
        val tvRecipientName: TextView = view.findViewById(R.id.tvRecipientName)
        val tvRecipientDescription: TextView = view.findViewById(R.id.tvRecipientDescription)
        val ivFavorite: ImageView = view.findViewById(R.id.ivFavorite)
        val ivChevron: ImageView = view.findViewById(R.id.ivChevron)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipientViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipient, parent, false)
        return RecipientViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipientViewHolder, position: Int) {
        val recipient = recipients[position]

        // Colored circle with first letter
        val color = iconColors[abs(recipient.name.hashCode()) % iconColors.size]
        val circle = GradientDrawable()
        circle.shape = GradientDrawable.OVAL
        circle.setColor(color)
        holder.tvRecipientIcon.background = circle
        holder.tvRecipientIcon.text = recipient.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

        holder.tvRecipientName.text = recipient.name

        if (!recipient.description.isNullOrBlank()) {
            holder.tvRecipientDescription.text = recipient.description
            holder.tvRecipientDescription.visibility = View.VISIBLE
        } else {
            holder.tvRecipientDescription.visibility = View.GONE
        }

        holder.ivFavorite.visibility = if (recipient.isFavorite) View.VISIBLE else View.GONE

        // Chevron and click only for user-specific recipients
        if (recipient.isUserSpecific) {
            holder.ivChevron.visibility = View.VISIBLE
            holder.itemView.setOnClickListener { onItemClick?.invoke(recipient) }
        } else {
            holder.ivChevron.visibility = View.INVISIBLE
            holder.itemView.setOnClickListener(null)
            holder.itemView.isClickable = false
        }
    }

    override fun getItemCount() = recipients.size

    fun updateRecipients(newRecipients: List<Recipient>) {
        recipients = newRecipients
        notifyDataSetChanged()
    }
}
