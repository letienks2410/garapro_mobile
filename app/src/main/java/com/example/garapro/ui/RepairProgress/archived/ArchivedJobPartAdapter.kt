package com.example.garapro.ui.RepairProgress.archived

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.garapro.data.model.RepairProgresses.ArchivedJobPart
import com.example.garapro.databinding.ItemArchivedJobPartBinding


class ArchivedJobPartAdapter(
    private val items: List<ArchivedJobPart>
) : RecyclerView.Adapter<ArchivedJobPartAdapter.ViewHolder>() {

    inner class ViewHolder(
        private val binding: ItemArchivedJobPartBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ArchivedJobPart) = with(binding) {
            tvPartName.text = item.partName
            tvPartInfo.text = "x${item.quantity} • ${formatCurrency(item.unitPrice)} • ${formatCurrency(item.lineTotal)}"
        }

        private fun formatCurrency(amount: Double): String {
            return String.format("%,.0f đ", amount)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemArchivedJobPartBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }
}
