package com.example.garapro.ui.RepairProgress.archived

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.garapro.data.model.RepairProgresses.ArchivedJob
import com.example.garapro.databinding.ItemArchivedJobBinding
import java.text.SimpleDateFormat
import java.util.Locale

class ArchivedJobAdapter(
    private val onItemClick: (ArchivedJob) -> Unit
) : ListAdapter<ArchivedJob, ArchivedJobAdapter.JobViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<ArchivedJob>() {
        override fun areItemsTheSame(oldItem: ArchivedJob, newItem: ArchivedJob): Boolean =
            oldItem.jobId == newItem.jobId

        override fun areContentsTheSame(oldItem: ArchivedJob, newItem: ArchivedJob): Boolean =
            oldItem == newItem
    }

    inner class JobViewHolder(
        private val binding: ItemArchivedJobBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ArchivedJob) = with(binding) {
            tvJobName.text = item.jobName
            tvJobStatus.text = getStatusText(item.status)
            tvTotalAmount.text = formatCurrency(item.totalAmount)
            tvRepairTime.text = formatRepairTime(item.repair?.startTime, item.repair?.endTime)
            tvTechnicianCount.text = "Technicians: ${item.technicians.size}"
            tvPartCount.text = "Parts: ${item.parts.size}"

            root.setOnClickListener { onItemClick(item) }
        }

        private fun formatCurrency(amount: Double): String {
            return String.format("%,.0f đ", amount)
        }

        private fun formatRepairTime(start: String?, end: String?): String {
            if (start.isNullOrEmpty() && end.isNullOrEmpty()) return "-"

            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

            fun parseTime(value: String?): String? = try {
                if (value.isNullOrEmpty()) null
                else inputFormat.parse(value)?.let { outputFormat.format(it) }
            } catch (e: Exception) {
                null
            }

            val s = parseTime(start)
            val e = parseTime(end)

            return when {
                s != null && e != null -> "$s - $e"
                s != null -> s
                e != null -> e
                else -> "-"
            }
        }
    }
    private fun getStatusText(status: String): String {
        return when (status) {
            "0" -> "Pending"
            "1" -> "New"
            "2" -> "In Progress"
            "3" -> "Completed"
            "4" -> "On Hold"
            else -> "Unknown"
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemArchivedJobBinding.inflate(inflater, parent, false)
        return JobViewHolder(binding)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}