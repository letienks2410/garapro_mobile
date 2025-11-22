package com.example.garapro.ui.RepairProgress.archived

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.garapro.R
import com.example.garapro.data.model.RepairProgresses.ArchivedJob
import com.example.garapro.data.model.RepairProgresses.RepairOrderArchivedDetail
import com.example.garapro.data.repository.RepairProgress.RepairProgressRepository
import com.example.garapro.databinding.FragmentRepairOrderArchivedDetailBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class RepairOrderArchivedDetailFragment : Fragment() {

    private var _binding: FragmentRepairOrderArchivedDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var jobsAdapter: ArchivedJobAdapter

    private val viewModel: RepairOrderArchivedDetailViewModel by viewModels {
        RepairOrderArchivedDetailViewModelFactory()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRepairOrderArchivedDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupJobsRecycler()
        observeViewModel()

        val repairOrderId = arguments?.getString("repairOrderId")
        if (repairOrderId == null) {
            Toast.makeText(requireContext(), "Missing repairOrderId", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.loadDetail(repairOrderId)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupJobsRecycler() {
        jobsAdapter = ArchivedJobAdapter { job ->
            showJobDetail(job)
        }
        binding.rvJobs.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = jobsAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.detailState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is RepairProgressRepository.ApiResponse.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is RepairProgressRepository.ApiResponse.Success -> {
                    binding.progressBar.visibility = View.GONE
                    bindDetail(state.data)
                }
                is RepairProgressRepository.ApiResponse.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun bindDetail(detail: RepairOrderArchivedDetail) = with(binding) {
        tvLicensePlate.text = detail.licensePlate
        tvBranchName.text = detail.branchName
        tvModelName.text = detail.modelName

        tvReceiveDate.text = formatDateTime(detail.receiveDate)
        tvCompletionDate.text = detail.completionDate?.let { formatDateTime(it) } ?: "-"
        tvArchivedAt.text = detail.archivedAt?.let { formatDateTime(it) } ?: "-"


        tvCost.text = formatCurrency(detail.cost)
        tvPaidAmount.text = formatCurrency(detail.paidAmount)
        tvNote.text = detail.note ?: "-"

        jobsAdapter.submitList(detail.jobs)
    }

    private fun formatDateTime(value: String?): String {
        if (value.isNullOrEmpty()) return "-"
        return try {
            val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val output = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val date = input.parse(value)
            date?.let { output.format(it) } ?: "-"
        } catch (e: Exception) {
            value
        }
    }

    private fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("vi", "VN")).format(amount)
    }

    private fun showJobDetail(job: ArchivedJob) {
        RepairArchivedJobDetailBottomSheet.newInstance(job)
            .show(childFragmentManager, "jobDetail")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
