package com.example.personalwealthmanager.presentation.sources

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.personalwealthmanager.R
import com.example.personalwealthmanager.domain.model.Source

class SourceAdapter(
    private var sources: List<Source>,
    private val showEditButton: Boolean,
    private val onEditClick: ((Source) -> Unit)? = null
) : RecyclerView.Adapter<SourceAdapter.SourceViewHolder>() {

    class SourceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSourceIcon: TextView = view.findViewById(R.id.tvSourceIcon)
        val tvSourceName: TextView = view.findViewById(R.id.tvSourceName)
        val tvSourceDescription: TextView = view.findViewById(R.id.tvSourceDescription)
        val btnEditSource: ImageView = view.findViewById(R.id.btnEditSource)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SourceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_source, parent, false)
        return SourceViewHolder(view)
    }

    override fun onBindViewHolder(holder: SourceViewHolder, position: Int) {
        val source = sources[position]

        // Set source icon (default icon for sources)
        holder.tvSourceIcon.text = "\uD83D\uDCB0" // Money bag emoji

        // Set source name
        holder.tvSourceName.text = source.name

        // Set description if available
        if (!source.description.isNullOrBlank()) {
            holder.tvSourceDescription.text = source.description
            holder.tvSourceDescription.visibility = View.VISIBLE
        } else {
            holder.tvSourceDescription.visibility = View.GONE
        }

        // Show edit button only for user-specific sources
        if (showEditButton && source.isUserSpecific) {
            holder.btnEditSource.visibility = View.VISIBLE
            holder.btnEditSource.setOnClickListener {
                onEditClick?.invoke(source)
            }
        } else {
            holder.btnEditSource.visibility = View.GONE
        }
    }

    override fun getItemCount() = sources.size

    fun updateSources(newSources: List<Source>) {
        sources = newSources
        notifyDataSetChanged()
    }
}
