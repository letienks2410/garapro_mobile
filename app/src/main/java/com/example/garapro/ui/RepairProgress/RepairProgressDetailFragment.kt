package com.example.garapro.ui.RepairProgress

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.garapro.R
import com.example.garapro.data.model.RepairProgresses.RepairProgressDetail
import com.example.garapro.data.repository.RepairProgress.RepairProgressRepository
import com.example.garapro.databinding.FragmentRepairProgressDetailBinding
import com.example.garapro.hubs.JobSignalRService
import com.example.garapro.hubs.RepairOrderEvent
import com.example.garapro.hubs.RepairOrderSignalRService
import com.example.garapro.utils.Constants
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class RepairProgressDetailFragment : Fragment() {

    private var _binding: FragmentRepairProgressDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RepairProgressDetailViewModel by viewModels()
    private lateinit var jobAdapter: JobAdapter

    private var repairOrderId: String? = null

    // ✅ SignalR service chỉ dùng cho màn này
    private var signalRService: RepairOrderSignalRService? = null

    private var repairHubService: RepairOrderSignalRService? = null
    private var jobHubService: JobSignalRService? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRepairProgressDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getRepairOrderIdFromArguments()
        setupToolbar()
        setupRecyclerView()
        observeViewModel()
        loadRepairOrderDetail()

        initRepairHub()
        observeRepairHubEvents()

        initJobHub()
        observeJobHubEvents()
    }

    private fun getRepairOrderIdFromArguments() {
        repairOrderId = arguments?.getString(ARG_REPAIR_ORDER_ID)
        if (repairOrderId.isNullOrEmpty()) {
            showError("Repair order ID not found")
            findNavController().navigateUp()
        }
    }

    private fun initRepairHub() {
        // ⚠️ ĐÂY PHẢI LÀ URL ĐÚNG CỦA HUB: vd: https://your-api.com/repairHub
        val hubUrl =Constants.BASE_URL_SIGNALR +"/hubs/repair"

        signalRService = RepairOrderSignalRService(hubUrl).apply {
            setupListeners()
        }

        repairOrderId?.let { id ->
            signalRService?.connectAndJoin(id)
        }
    }

    private fun observeRepairHubEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            signalRService?.events?.collect { roId ->
                Log.d("SignalR", "Received event for RO: $roId")
                if (roId == repairOrderId) {
                    loadRepairOrderDetail()
                }
            }
        }
    }
    private fun initJobHub() {
        val jobHubUrl = Constants.BASE_URL_SIGNALR +"/hubs/job"
        jobHubService = JobSignalRService(jobHubUrl).apply {
            setupListeners()
        }
        repairOrderId?.let { id ->
            jobHubService?.connectAndJoinRepairOrder(id)
        }
    }

    private fun observeJobHubEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            jobHubService?.events?.collect { roId ->
                Log.d("SignalR", "JobHub event for RO: $roId")
                if (roId == repairOrderId) {
                    loadRepairOrderDetail()
                }
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        jobAdapter = JobAdapter()
        binding.jobsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = jobAdapter
            addItemDecoration(DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL))
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.repairOrderDetail.collect { response ->
                when (response) {
                    is RepairProgressRepository.ApiResponse.Loading -> {
                        showLoading(true)
                    }
                    is RepairProgressRepository.ApiResponse.Success -> {
                        showLoading(false)
                        bindRepairOrderDetail(response.data)
                    }
                    is RepairProgressRepository.ApiResponse.Error -> {
                        showLoading(false)
                        showError(response.message)
                    }
                    else -> {}
                }
            }
        }
    }

    private fun loadRepairOrderDetail() {
        repairOrderId?.let { id ->
            viewModel.loadRepairOrderDetail(id)
        } ?: showError("Repair order ID is null")
    }

    private fun bindRepairOrderDetail(detail: RepairProgressDetail) {
        binding.apply {
            // Vehicle Info
            vehicleInfo.text =
                "${detail.vehicle.licensePlate} • ${detail.vehicle.model} • ${detail.vehicle.year}"
            vehicleBrand.text = detail.vehicle.brand

            // Order Details
            receiveDate.text = formatDate(detail.receiveDate)
            estimatedCompletionDate.text = formatDate(detail.estimatedCompletionDate)
            roType.text = getROTypeName(detail.roType)
            paidStatus.text = getPaidStatusName(detail.paidStatus)
            note.text = detail.note ?: "No note"

            // Financial Info
            estimatedAmount.text = formatCurrency(detail.estimatedAmount)
            paidAmount.text = formatCurrency(detail.paidAmount)
            cost.text = formatCurrency(detail.cost)

            // Progress Section
            progressStatus.text = detail.progressStatus
            progressPercentage.text = "${detail.progressPercentage}%"
            progressJobBar.progress = detail.progressPercentage
            statusName.text = getStatusName(detail.orderStatus.statusName)

            // Set status color based on status
            setStatusColor(detail.orderStatus.statusName)

            // Jobs
            jobAdapter.submitList(detail.jobs)
        }
    }

    private fun setStatusColor(statusName: String) {
        val color = when (statusName) {
            "Completed" -> ContextCompat.getColor(requireContext(), R.color.green)
            "In Progress" -> ContextCompat.getColor(requireContext(), R.color.blue)
            "Pending" -> ContextCompat.getColor(requireContext(), R.color.orange)
            "Cancelled" -> ContextCompat.getColor(requireContext(), R.color.red)
            else -> ContextCompat.getColor(requireContext(), R.color.gray)
        }
        binding.statusName.setBackgroundColor(color)
    }

    private fun getStatusName(statusName: String): String {
        return when (statusName) {
            "Completed" -> "Completed"
            "In Progress" -> "In Progress"
            "Pending" -> "Pending"
            else -> "Unknown"
        }
    }

    private fun getPaidStatusName(statusName: String): String {
        return when (statusName) {
            "Paid" -> "Paid"
            "Unpaid" -> "Pending Payment"
            else -> "Unknown"
        }
    }

    private fun getROTypeName(statusName: String): String {
        return when (statusName) {
            "WalkIn" -> "Walk-in"
            "Scheduled" -> "Scheduled"
            "Breakdown" -> "Breakdown"
            else -> "Unknown"
        }
    }

    private fun formatDate(dateString: String?): String {
        if (dateString == null) return "N/A"
        return try {
            val inputFormat =
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            dateString
        }
    }

    private fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("vi", "VN")).format(amount)
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("Retry") {
                loadRepairOrderDetail()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        signalRService?.leaveGroupAndStop()
        signalRService = null

        jobHubService?.leaveRepairOrderGroupAndStop();
        jobHubService = null

        _binding = null
    }


    companion object {
        private const val ARG_REPAIR_ORDER_ID = "repairOrderId"

        fun newInstance(repairOrderId: String): RepairProgressDetailFragment {
            val args = Bundle().apply {
                putString(ARG_REPAIR_ORDER_ID, repairOrderId)
            }
            val fragment = RepairProgressDetailFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
