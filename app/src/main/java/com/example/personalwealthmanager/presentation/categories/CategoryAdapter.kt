package com.pwm.personalwealthmanager.presentation.categories

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pwm.personalwealthmanager.R
import com.pwm.personalwealthmanager.domain.model.Category
import kotlin.math.abs

class CategoryAdapter(
    private var categories: List<Category>,
    private val onItemClick: ((Category) -> Unit)? = null
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

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

    class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCategoryIcon: TextView = view.findViewById(R.id.tvCategoryIcon)
        val tvCategoryName: TextView = view.findViewById(R.id.tvCategoryName)
        val tvCategoryDescription: TextView = view.findViewById(R.id.tvCategoryDescription)
        val ivChevron: ImageView = view.findViewById(R.id.ivChevron)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]

        // Colored circle with emoji icon
        val color = iconColors[abs(category.name.hashCode()) % iconColors.size]
        val circle = GradientDrawable()
        circle.shape = GradientDrawable.OVAL
        circle.setColor(color)
        holder.tvCategoryIcon.background = circle
        holder.tvCategoryIcon.text = category.icon ?: "📁"

        holder.tvCategoryName.text = category.name

        if (!category.description.isNullOrBlank()) {
            holder.tvCategoryDescription.text = category.description
            holder.tvCategoryDescription.visibility = View.VISIBLE
        } else {
            holder.tvCategoryDescription.visibility = View.GONE
        }

        // Chevron and click only for user-specific (editable) categories
        if (category.isUserSpecific) {
            holder.ivChevron.visibility = View.VISIBLE
            holder.itemView.setOnClickListener { onItemClick?.invoke(category) }
        } else {
            holder.ivChevron.visibility = View.INVISIBLE
            holder.itemView.setOnClickListener(null)
            holder.itemView.isClickable = false
        }
    }

    override fun getItemCount() = categories.size

    fun updateCategories(newCategories: List<Category>) {
        categories = newCategories
        notifyDataSetChanged()
    }
}
