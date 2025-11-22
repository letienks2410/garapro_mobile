package com.example.garapro.ui.RepairProgress.archived

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.garapro.data.model.RepairProgresses.ArchivedJob
import com.example.garapro.databinding.BottomSheetArchivedJobDetailBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import java.text.SimpleDateFormat
import java.util.Locale

class RepairArchivedJobDetailBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_JOB = "arg_job"

        fun newInstance(job: ArchivedJob): RepairArchivedJobDetailBottomSheet {
            return RepairArchivedJobDetailBottomSheet().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_JOB, job)
                }
            }
        }
    }

    private var _binding: BottomSheetArchivedJobDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetArchivedJobDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val job = arguments?.getParcelable<ArchivedJob>(ARG_JOB)
        if (job == null) {
            dismiss()
            return
        }

        bindJob(job)
    }

    private fun bindJob(job: ArchivedJob) = with(binding) {
        tvJobName.text = job.jobName
        tvJobStatus.text = job.status
        tvJobTotalAmount.text = "Total: ${formatCurrency(job.totalAmount)}"

        tvRepairTime.text = "Repair time: ${formatRepairTime(job.repair?.startTime, job.repair?.endTime)}"
        tvRepairNotes.text = "Notes: ${job.repair?.notes ?: "-"}"

        rvTechnicians.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = ArchivedJobTechnicianAdapter(job.technicians)
        }

        rvParts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = ArchivedJobPartAdapter(job.parts)
        }
    }

    private fun formatCurrency(amount: Double): String {
        return String.format("%,.0f đ", amount)
    }

    private fun formatRepairTime(start: String?, end: String?): String {
        if (start.isNullOrEmpty() && end.isNullOrEmpty()) return "-"

        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
