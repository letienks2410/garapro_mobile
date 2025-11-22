package com.example.garapro.ui.RepairProgress.archived

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.Observer
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.example.garapro.R
import com.example.garapro.data.model.RepairProgresses.PagedResult
import com.example.garapro.data.model.RepairProgresses.RepairOrderArchivedFilter
import com.example.garapro.data.model.RepairProgresses.RepairOrderArchivedListItem
import com.example.garapro.data.model.RepairProgresses.RoType
import com.example.garapro.data.repository.RepairProgress.RepairProgressRepository
import com.example.garapro.databinding.FragmentRepairOrderArchivedListBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RepairOrderArchivedListFragment : Fragment() {

    private var _binding: FragmentRepairOrderArchivedListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: RepairOrderArchivedAdapter

    private val viewModel: RepairOrderArchivedListViewModel by viewModels {
        RepairOrderArchivedListViewModelFactory()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRepairOrderArchivedListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupSwipeRefresh()
        setupFilter()
        observeViewModel()

        // Lần đầu load
        viewModel.loadOrders()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        adapter = RepairOrderArchivedAdapter { item ->
            openDetail(item)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }
    }

    private fun setupFilter() {
        // Ẩn filter status vì archived luôn completed
        binding.statusFilterLayout.visibility = View.GONE

        // Nút mở/đóng filter
        binding.filterButton.setOnClickListener {
            binding.filterContainer.visibility =
                if (binding.filterContainer.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        // Clear filter
        binding.clearFilterButton.setOnClickListener {
            viewModel.clearFilter()
            // reset UI hiển thị
            binding.roTypeFilter.setText("", false)
            binding.paidStatusFilter.setText("", false)
            binding.dateFilter.text = getString(R.string.select_date_range) // tạo string resource nếu cần
        }

        // Setup các lựa chọn filter
        setupFilterOptions()
    }
    private fun setupFilterOptions() {
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

        // Date range filter
        binding.dateFilter.setOnClickListener {
            showDateRangePicker()
        }
    }
    private fun showRoTypeFilterDialog() {
        val roTypes = arrayOf("Walk-in", "Scheduled", "Breakdown")
        val currentFilter = viewModel.filterState.value?.roType
        val checkedItem = when (currentFilter) {
            RoType.WalkIn -> 0
            RoType.Scheduled -> 1
            RoType.Breakdown -> 2
            else -> -1
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Filter by Order Type")
            .setSingleChoiceItems(roTypes, checkedItem) { dialog, which ->
                val selectedType = when (which) {
                    0 -> RoType.WalkIn
                    1 -> RoType.Scheduled
                    2 -> RoType.Breakdown
                    else -> null
                }
                viewModel.updateRoTypeFilter(selectedType)
                binding.roTypeFilter.setText(
                    selectedType?.let { roTypes[which] } ?: "",
                    false
                )
                dialog.dismiss()
            }
            .setNegativeButton("Clear") { dialog, _ ->
                viewModel.updateRoTypeFilter(null)
                binding.roTypeFilter.setText("", false)
                dialog.dismiss()
            }
            .setPositiveButton("OK", null)
            .show()
    }
    private fun showPaidStatusFilterDialog() {
        val paidStatuses = arrayOf("Pending Payment", "Paid")
        val currentFilter = viewModel.filterState.value?.paidStatus

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
                binding.paidStatusFilter.setText(
                    if (selectedStatus == null) "" else paidStatuses[which],
                    false
                )
                dialog.dismiss()
            }
            .setNegativeButton("Clear") { dialog, _ ->
                viewModel.updatePaidStatusFilter(null)
                binding.paidStatusFilter.setText("", false)
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

                viewModel.updateDateRangeFilter(fromApi, toApi)
                binding.dateFilter.text = "$fromDisplay - $toDisplay"
            }
        }

        picker.show(parentFragmentManager, "archived_date_range_picker")
    }








    private fun observeViewModel() {
        viewModel.ordersState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is RepairProgressRepository.ApiResponse.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.emptyState.visibility = View.GONE
                }
                is RepairProgressRepository.ApiResponse.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    applyData(state.data)
                }
                is RepairProgressRepository.ApiResponse.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    binding.emptyState.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun applyData(paged: PagedResult<RepairOrderArchivedListItem>) {
        val items = paged.items ?: emptyList()
        if (items.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
        } else {
            binding.emptyState.visibility = View.GONE
        }
        adapter.submitList(items)
    }

    private fun openDetail(item: RepairOrderArchivedListItem) {
        val bundle = bundleOf(
            "repairOrderId" to item.repairOrderId
        )
        findNavController().navigate(
            R.id.repairArchivedDetailFragment,
            bundle
        )
        // hoặc nếu dùng action:
        // findNavController().navigate(R.id.action_repairArchivedFragment_to_repairArchivedDetailFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
