package com.example.garapro.ui.quotations

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.garapro.data.model.quotations.QuotationServicePart
import com.example.garapro.databinding.ItemQuotationPartBinding
import java.text.NumberFormat
import java.util.Locale

class QuotationPartAdapter(
    private val parts: List<QuotationServicePart>,
    private val onPartToggle: (String) -> Unit
) : RecyclerView.Adapter<QuotationPartAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemQuotationPartBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(parts[position])
    }

    override fun getItemCount() = parts.size

    inner class ViewHolder(private val binding: ItemQuotationPartBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(part: QuotationServicePart) {
            binding.tvPartName.text = part.partName
            binding.tvPartDescription.text = part.partDescription
            binding.tvPartPrice.text = formatCurrency(part.totalPrice)
            binding.cbPart.isChecked = part.isSelected
            binding.tvRecommendation.visibility =
                if (part.isRecommended) View.VISIBLE else View.GONE
            binding.tvRecommendationNote.text = part.recommendationNote

            binding.cbPart.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked != part.isSelected) {
                    onPartToggle(part.quotationServicePartId)
                }
            }
        }

        private fun formatCurrency(amount: Double): String {
            return NumberFormat.getCurrencyInstance(Locale("vi", "VN")).format(amount)
        }
    }
}