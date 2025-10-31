package com.example.garapro.ui.RepairProgress

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.garapro.R
import com.example.garapro.data.model.RepairProgresses.Job
import com.example.garapro.data.model.RepairProgresses.Part
import com.example.garapro.data.model.RepairProgresses.Repair
import com.example.garapro.data.model.RepairProgresses.Technician
import com.example.garapro.databinding.ItemJobBinding
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class JobAdapter : ListAdapter<Job, JobAdapter.ViewHolder>(DiffCallback) {

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Job>() {
            override fun areItemsTheSame(oldItem: Job, newItem: Job): Boolean {
                return oldItem.jobId == newItem.jobId
            }

            override fun areContentsTheSame(oldItem: Job, newItem: Job): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemJobBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val job = getItem(position)
        holder.bind(job)
    }

    inner class ViewHolder(private val binding: ItemJobBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var isExpanded = false

        fun bind(job: Job) {
            binding.apply {
                jobName.text = job.jobName
                jobLevel.text = "Level ${job.level}"
                jobStatus.text = job.status
                jobAmount.text = formatCurrency(job.totalAmount)
                jobDeadline.text = formatDate(job.deadline)
                jobNote.text = job.note ?: "No note"

                // Set status background color
                setJobStatusColor(job.status)

                // Setup repair info
                job.repair?.let { repair ->
                    repairInfoLayout.visibility = View.VISIBLE
                    repairDescription.text = repair.description
                    estimatedTime.text = repair.estimatedTime ?: "N/A"

                    // Repair notes
                    repair.notes?.let { notes ->
                        repairNotes.text = notes
                        repairNotes.visibility = View.VISIBLE
                    } ?: run {
                        repairNotes.visibility = View.GONE
                    }

                    // Repair times
                    setupRepairTimes(repair)
                } ?: run {
                    repairInfoLayout.visibility = View.GONE
                }

                // Setup technicians
                if (job.technicians.isNotEmpty()) {
                    techniciansLayout.visibility = View.VISIBLE
                    setupTechnicians(job.technicians)
                } else {
                    techniciansLayout.visibility = View.GONE
                }

                // Setup parts
                if (job.parts.isNotEmpty()) {
                    partsLayout.visibility = View.VISIBLE
                    setupParts(job.parts)
                } else {
                    partsLayout.visibility = View.GONE
                }

                // Expand/Collapse functionality
                expandButton.setOnClickListener {
                    isExpanded = !isExpanded
                    toggleExpansion()
                }

                // Set initial state
                toggleExpansion()
            }
        }

        private fun setupRepairTimes(repair: Repair) {
            // You can add more detailed time information here if needed
            val times = StringBuilder()

            repair.startTime?.let { startTime ->
                times.append("Start: ${formatTime(startTime)}\n")
            }

            repair.endTime?.let { endTime ->
                times.append("End: ${formatTime(endTime)}\n")
            }

            repair.actualTime?.let { actualTime ->
                times.append("Actual: $actualTime")
            }

            // If you want to display times, you can add a TextView for it
        }

        private fun setupTechnicians(technicians: List<Technician>) {
            binding.techniciansChipGroup.removeAllViews()
            technicians.forEach { technician ->
                val chip = Chip(binding.root.context).apply {
                    text = technician.fullName
                    isCloseIconVisible = false
                    chipBackgroundColor = ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.gray_light)
                    )
                    setOnClickListener {
                        showTechnicianInfo(technician)
                    }
                }
                binding.techniciansChipGroup.addView(chip)
            }
        }

        private fun setupParts(parts: List<Part>) {
            binding.partsRecyclerView.layoutManager = LinearLayoutManager(
                binding.root.context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            val adapter = PartAdapter()
            adapter.submitList(parts)
            binding.partsRecyclerView.adapter = adapter
        }

        private fun toggleExpansion() {
            binding.apply {
                repairInfoLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
                techniciansLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
                partsLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE

                expandButton.text = if (isExpanded) "Hide Details" else "Show Details"
                expandButton.setIconResource(
                    if (isExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
                )
            }
        }

        private fun setJobStatusColor(status: String) {
            val color = when (status) {
                "Completed" -> R.color.green
                "In Progress" -> R.color.blue
                "New" -> R.color.orange
                "Cancelled" -> R.color.red
                else -> R.color.gray
            }
            binding.jobStatus.setBackgroundColor(
                ContextCompat.getColor(binding.root.context, color)
            )
        }

        private fun showTechnicianInfo(technician: Technician) {
            MaterialAlertDialogBuilder(binding.root.context)
                .setTitle(technician.fullName)
                .setMessage(
                    """
                    Email: ${technician.email}
                    Phone: ${technician.phoneNumber}
                    """.trimIndent()
                )
                .setPositiveButton("OK", null)
                .show()
        }

        private fun formatCurrency(amount: Double): String {
            return NumberFormat.getCurrencyInstance(Locale("vi", "VN")).format(amount)
        }

        private fun formatDate(dateString: String?): String {
            if (dateString == null) return "No deadline"
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val date = inputFormat.parse(dateString)
                outputFormat.format(date!!)
            } catch (e: Exception) {
                dateString
            }
        }

        private fun formatTime(timeString: String?): String {
            if (timeString == null) return "N/A"
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
                val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val date = inputFormat.parse(timeString)
                outputFormat.format(date!!)
            } catch (e: Exception) {
                timeString
            }
        }
    }
}