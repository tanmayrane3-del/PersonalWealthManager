package com.example.personalwealthmanager.presentation.recipients

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.personalwealthmanager.R
import com.example.personalwealthmanager.domain.model.Recipient

class RecipientAdapter(
    private var recipients: List<Recipient>,
    private val showEditButton: Boolean,
    private val onEditClick: ((Recipient) -> Unit)? = null
) : RecyclerView.Adapter<RecipientAdapter.RecipientViewHolder>() {

    class RecipientViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRecipientIcon: TextView = view.findViewById(R.id.tvRecipientIcon)
        val tvRecipientName: TextView = view.findViewById(R.id.tvRecipientName)
        val tvRecipientDescription: TextView = view.findViewById(R.id.tvRecipientDescription)
        val ivFavorite: ImageView = view.findViewById(R.id.ivFavorite)
        val btnEditRecipient: ImageView = view.findViewById(R.id.btnEditRecipient)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipientViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipient, parent, false)
        return RecipientViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipientViewHolder, position: Int) {
        val recipient = recipients[position]

        // Set recipient icon (default icon for recipients)
        holder.tvRecipientIcon.text = "\uD83C\uDFEA" // Store emoji

        // Set recipient name
        holder.tvRecipientName.text = recipient.name

        // Set description if available
        if (!recipient.description.isNullOrBlank()) {
            holder.tvRecipientDescription.text = recipient.description
            holder.tvRecipientDescription.visibility = View.VISIBLE
        } else {
            holder.tvRecipientDescription.visibility = View.GONE
        }

        // Show favorite indicator
        holder.ivFavorite.visibility = if (recipient.isFavorite) View.VISIBLE else View.GONE

        // Show edit button only for user-specific recipients
        if (showEditButton && recipient.isUserSpecific) {
            holder.btnEditRecipient.visibility = View.VISIBLE
            holder.btnEditRecipient.setOnClickListener {
                onEditClick?.invoke(recipient)
            }
        } else {
            holder.btnEditRecipient.visibility = View.GONE
        }
    }

    override fun getItemCount() = recipients.size

    fun updateRecipients(newRecipients: List<Recipient>) {
        recipients = newRecipients
        notifyDataSetChanged()
    }
}
