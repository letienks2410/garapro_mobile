package com.example.garapro.ui.RepairProgress

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.garapro.R
import com.example.garapro.data.model.RepairProgresses.FilterChipData
import com.example.garapro.data.model.RepairProgresses.RepairOrderArchivedFilter
import com.example.garapro.data.model.RepairProgresses.RepairOrderFilter
import com.example.garapro.data.model.RepairProgresses.RoType
import com.example.garapro.data.repository.RepairProgress.RepairProgressRepository
import com.example.garapro.databinding.FragmentRepairProgressListBinding
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.example.garapro.data.model.RepairProgresses.RepairOrderListItem
import com.example.garapro.data.model.payments.CreatePaymentRequest
import com.example.garapro.ui.feedback.RatingActivity
import com.example.garapro.hubs.JobSignalRService
import com.example.garapro.ui.payments.PaymentBillActivity
import com.example.garapro.utils.Constants
import com.google.android.material.datepicker.MaterialDatePicker

import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class RepairProgressListFragment : Fragment() {

    private lateinit var binding: FragmentRepairProgressListBinding
    private val viewModel: RepairProgressViewModel by viewModels()
    private lateinit var adapter: RepairOrderAdapter


    private var jobHubService: JobSignalRService? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentRepairProgressListBinding.inflate(inflater, container, false)
        return binding.root
    }
    companion object {
        private const val PREFS_AUTH = "auth_prefs"
        private const val KEY_USER_ID = "user_id"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFilter()
        observeViewModel()
        setupSwipeRefresh()
        initJobHub()
        observeJobHubEvents()
    }



    private fun setupRecyclerView() {
        adapter = RepairOrderAdapter(
            onItemClick = { repairOrder ->
                navigateToDetail(repairOrder.repairOrderId)
            },
            onPaymentClick = { repairOrder ->
                navigateToPaymentBill(repairOrder.repairOrderId)
            },

        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@RepairProgressListFragment.adapter
            addItemDecoration(
                DividerItemDecoration(
                    requireContext(),
                    LinearLayoutManager.VERTICAL
                )
            )

            //  Phân trang: load thêm khi cuộn tới cuối
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    // chỉ xử lý khi scroll xuống
                    if (dy <= 0) return

                    if (!recyclerView.canScrollVertically(1)) {
                        viewModel.loadNextPage()
                    }
                }
            })
        }
    }

    private fun initJobHub() {
        val jobHubUrl = Constants.BASE_URL_SIGNALR + "/hubs/job"

        jobHubService = JobSignalRService(jobHubUrl).apply {
            setupListeners()
            start {
                joinUserToRepairOrderGroup()
            }
        }
    }

    private fun joinUserToRepairOrderGroup() {
        val prefs = requireContext().getSharedPreferences(PREFS_AUTH, Context.MODE_PRIVATE)
        val userId = prefs.getString(KEY_USER_ID, null)

        if (userId.isNullOrEmpty()) {
            Log.w("SignalR", "UserId null  không join RepairOrderUserGroup")
            return
        }

        jobHubService?.joinRepairOrderUserGroup(userId)
    }
    private fun observeJobHubEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            jobHubService?.events?.collect { roId ->
                Log.d("SignalR", "JobHub event for RO in list: $roId")
                //  Cứ có tín hiệu là reload list
                viewModel.loadRepairOrders()
            }
        }
    }






    private fun setupFilter() {
        // Filter button
        binding.filterButton.setOnClickListener {
            viewModel.toggleFilterVisibility()
        }

        // Clear filter button
        binding.clearFilterButton.setOnClickListener {
            viewModel.clearFilter()
        }

        // Setup filter options
        setupFilterOptions()
    }

    private fun setupFilterOptions() {
        // Status filter
        binding.statusFilterLayout.setEndIconOnClickListener {
            showStatusFilterDialog()
        }
        binding.statusFilter.setOnClickListener {
            showStatusFilterDialog()
        }

        // RO Type filter
        binding.roTypeFilterLayout.setEndIconOnClickListener {
            showRoTypeFilterDialog()
        }
        binding.roTypeFilter.setOnClickListener {
            showRoTypeFilterDialog()
        }

        // Paid status filter
        binding.paidStatusFilterLayout.setEndIconOnClickListener {
            showPaidStatusFilterDialog()
        }
        binding.paidStatusFilter.setOnClickListener {
            showPaidStatusFilterDialog()
        }

        // Date filter
        binding.dateFilter.setOnClickListener {
            showDateRangePicker()
        }
    }


    private fun showStatusFilterDialog() {
        val statuses = viewModel.orderStatuses.value
        val items = statuses.map { it.statusName }.toTypedArray()
        val checkedItem = viewModel.filterState.value.statusId?.let { statusId ->
            statuses.indexOfFirst { it.orderStatusId == statusId }
        } ?: -1

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Filter by Status")
            .setSingleChoiceItems(items, checkedItem) { dialog, which ->
                val selectedStatus = statuses.getOrNull(which)
                viewModel.updateStatusFilter(selectedStatus?.orderStatusId)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRoTypeFilterDialog() {
        val roTypes = arrayOf("Walk-in", "Scheduled", "Breakdown")
        val currentFilter = viewModel.filterState.value.roType
        val checkedItem = currentFilter?.ordinal ?: -1

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Filter by RO Type")
            .setSingleChoiceItems(roTypes, checkedItem) { dialog, which ->
                val selectedType = when (which) {
                    0 -> RoType.WalkIn
                    1 -> RoType.Scheduled
                    2 -> RoType.Breakdown
                    else -> null
                }
                viewModel.updateRoTypeFilter(selectedType)
                dialog.dismiss()
            }
            .setNegativeButton("Clear") { dialog, _ ->
                viewModel.updateRoTypeFilter(null)
                dialog.dismiss()
            }
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showPaidStatusFilterDialog() {
        val paidStatuses = arrayOf("Pending Payment", "Partially Paid", "Paid")
        val currentFilter = viewModel.filterState.value.paidStatus
        val checkedItem = when (currentFilter) {
            "Unpaid" -> 0
            "Paid" -> 1
            else -> -1
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Filter by Payment Status")
            .setSingleChoiceItems(paidStatuses, checkedItem) { dialog, which ->
                val selectedStatus = when (which) {
                    0 -> "Unpaid"
                    1 -> "Paid"
                    else -> null
                }
                viewModel.updatePaidStatusFilter(selectedStatus)
                dialog.dismiss()
            }
            .setNegativeButton("Clear") { dialog, _ ->
                viewModel.updatePaidStatusFilter(null)
                dialog.dismiss()
            }
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showDateRangePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(R.string.select_date_range)
            .setTheme(R.style.ThemeOverlay_GaraPro_DatePicker)
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            val start = selection.first
            val end = selection.second

            if (start != null && end != null) {
                val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val displayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val cal = Calendar.getInstance()

                cal.timeInMillis = start
                val fromApi = apiFormat.format(cal.time)
                val fromDisplay = displayFormat.format(cal.time)

                cal.timeInMillis = end
                val toApi = apiFormat.format(cal.time)
                val toDisplay = displayFormat.format(cal.time)

                viewModel.updateDateFilter(fromApi, toApi)
                binding.dateFilter.text = "$fromDisplay - $toDisplay"
            }
        }

        picker.show(parentFragmentManager, "date_range_picker")
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.repairOrders.collect { response ->
                when (response) {
                    is RepairProgressRepository.ApiResponse.Loading -> {
                        if (!binding.swipeRefresh.isRefreshing) {
                            binding.progressBar.visibility = View.VISIBLE
                        }
                        binding.emptyState.visibility = View.GONE
                    }

                    is RepairProgressRepository.ApiResponse.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.swipeRefresh.isRefreshing = false

                        val orders = response.data.items
                        adapter.submitList(orders)



                        binding.emptyState.visibility =
                            if (orders.isEmpty()) View.VISIBLE else View.GONE
                        binding.emptyText.text =
                            if (viewModel.filterChips.value.isNotEmpty()) {
                                "No orders match the current filters"
                            } else {
                                "No repair orders found"
                            }

                    }


                    is RepairProgressRepository.ApiResponse.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.swipeRefresh.isRefreshing = false
                        showError(response.message)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.showFilter.collect { show ->
                binding.filterContainer.visibility = if (show) View.VISIBLE else View.GONE
                binding.filterButton.setImageResource(
                    if (show) R.drawable.ic_ft_expand_less else R.drawable.ic_filter
                )
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.filterState.collect { filter ->
                updateFilterInputs(filter)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.filterChips.collect { chips ->
                updateFilterChips(chips)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.loadingSpinner.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
    }

    private fun updateFilterInputs(filter: RepairOrderFilter) {
        // Update status
        filter.statusId?.let { statusId ->
            val statusName = viewModel.orderStatuses.value.find { it.orderStatusId == statusId }?.statusName ?: "Status"
            val statusEnglish = when (statusName) {
                "Completed" -> "Completed"
                "In Progress" -> "In Progress"
                "Pending" -> "Pending"
                else -> "Unknown"
            }
            binding.statusFilter.setText(statusEnglish)
        } ?: run {
            binding.statusFilter.setText("")
        }

        // Update RO Type
        filter.roType?.let { roType ->
            binding.roTypeFilter.setText(
                when (roType) {
                    RoType.WalkIn -> "Walk-in"
                    RoType.Scheduled -> "Scheduled"
                    RoType.Breakdown -> "Breakdown"
                }
            )
        } ?: run {
            binding.roTypeFilter.setText("")
        }

        // Update payment status
        filter.paidStatus?.let { paidStatus ->
            binding.paidStatusFilter.setText(
                when (paidStatus) {
                    "Unpaid" -> "Pending Payment"

                    "Paid" -> "Paid"
                    else -> paidStatus
                }
            )
        } ?: run {
            binding.paidStatusFilter.setText("")
        }

        // Update date
        if (filter.fromDate != null || filter.toDate != null) {
            val fromText = filter.fromDate ?: ""
            val toText = filter.toDate ?: ""
            binding.dateFilter.text = "$fromText - $toText"
        } else {
            binding.dateFilter.text = "Select date range"
        }
    }

    private fun updateFilterChips(chips: List<FilterChipData>) {
        binding.filterChipGroup.removeAllViews()

        chips.forEach { chipData ->
            val chip = Chip(requireContext()).apply {
                text = chipData.text
                isCloseIconVisible = true
                tag = chipData.id
                setOnCloseIconClickListener {
                    viewModel.removeFilterChip(chipData.id)
                }
            }
            binding.filterChipGroup.addView(chip)
        }

        binding.filterChipGroup.visibility = if (chips.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadFirstPage()
        }
    }

    private fun navigateToDetail(repairOrderId: String) {
        val bundle = Bundle().apply {
            putString("repairOrderId", repairOrderId)
        }
        findNavController().navigate(
            R.id.action_repairTrackingFragment_to_repairProgressDetailFragment,
            bundle
        )
    }
    private fun navigateToPaymentBill(repairOrderId: String) {
        val intent = Intent(requireContext(), PaymentBillActivity::class.java)
        intent.putExtra(PaymentBillActivity.EXTRA_REPAIR_ORDER_ID, repairOrderId)
        startActivity(intent)
    }


    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun leaveRepairOrderUserGroup() {
        val prefs = requireContext().getSharedPreferences(PREFS_AUTH, Context.MODE_PRIVATE)
        val userId = prefs.getString(KEY_USER_ID, null)

        if (!userId.isNullOrEmpty()) {
            jobHubService?.leaveRepairOrderUserGroup(userId)
        }
        jobHubService?.stop()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        leaveRepairOrderUserGroup()
        jobHubService = null


    }
    override fun onResume() {
        super.onResume()

        viewModel.loadFirstPage()
    }
}