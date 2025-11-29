package com.example.garapro.ui.RepairProgress.archived

import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.garapro.data.model.RepairProgresses.RepairOrderArchivedListItem
import com.example.garapro.utils.MoneyUtils
import java.text.SimpleDateFormat
import java.util.Locale

class RepairOrderArchivedAdapter(
    private val onItemClick: (RepairOrderArchivedListItem) -> Unit
) : androidx.recyclerview.widget.ListAdapter<RepairOrderArchivedListItem,
        RepairOrderArchivedAdapter.ViewHolder>(
    object : androidx.recyclerview.widget.DiffUtil.ItemCallback<RepairOrderArchivedListItem>() {
        override fun areItemsTheSame(
            oldItem: RepairOrderArchivedListItem,
            newItem: RepairOrderArchivedListItem
        ) = oldItem.repairOrderId == newItem.repairOrderId

        override fun areContentsTheSame(
            oldItem: RepairOrderArchivedListItem,
            newItem: RepairOrderArchivedListItem
        ) = oldItem == newItem
    }
) {

    inner class ViewHolder(val binding: com.example.garapro.databinding.ItemRepairOrderArchivedBinding) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {
        fun bind(item: RepairOrderArchivedListItem) {
            binding.tvLicensePlate.text = item.licensePlate +" "+item.brandName+ " "+item.modelName
            binding.tvBranchModel.text = "${item.branchName}"
            binding.tvReceiveDate.text = "${formatDateTime(item.receiveDate)}"
            binding.tvCompletionDate.text =" ${formatDateTime(item.completionDate)}"
            binding.tvCost.text = "${MoneyUtils.formatVietnameseCurrency(item.cost)}"

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val b = com.example.garapro.databinding.ItemRepairOrderArchivedBinding
            .inflate(inflater, parent, false)
        return ViewHolder(b)
    }

    private fun formatDateTime(value: String?): String {
        if (value.isNullOrEmpty()) return "-"

        return try {
            val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val output = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val date = input.parse(value)
            date?.let { output.format(it) } ?: "-"
        } catch (e: Exception) {
            "-" // fallback nếu lỗi format
        }
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
