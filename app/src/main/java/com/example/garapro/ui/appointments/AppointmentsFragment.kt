package com.example.garapro.ui.appointments

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.garapro.R
import com.example.garapro.data.local.TokenManager
import com.example.garapro.data.model.NetworkResult
import com.example.garapro.data.model.repairRequest.Branch
import com.example.garapro.data.model.repairRequest.RepairRequest
import com.example.garapro.data.model.repairRequest.Vehicle
import com.example.garapro.data.repository.repairRequest.BookingRepository
import com.example.garapro.databinding.FragmentAppointmentsBinding
import com.example.garapro.ui.repairRequest.BookingActivity
import com.example.garapro.hubs.RepairRequestSignalrService
import com.example.garapro.utils.Constants
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class AppointmentsFragment : Fragment() {

    private lateinit var binding: FragmentAppointmentsBinding
    private lateinit var tokenManager: TokenManager
    private lateinit var repository: BookingRepository

    private lateinit var adapter: RepairRequestAdapter
    private var isFilterVisible = false
    private val viewModel: AppointmentsViewModel by viewModels {
        BookingViewModelFactory(repository)
    }
    companion object {
        private const val PREFS_AUTH = "auth_prefs"
        private const val KEY_USER_ID = "user_id"
    }

    // ========== SIGNALR HUB ==========
    private lateinit var repairHub: RepairRequestSignalrService
    private var currentUserIdForSignalR: String? = null

    private val currentRepairOrderIdForSignalR: String? = null
    // =================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tokenManager = TokenManager(requireContext())
        repository = BookingRepository(requireContext(), tokenManager)

        // Khởi tạo hub
        val hubUrl = Constants.BASE_URL_SIGNALR + "/hubs/repairRequest"
        repairHub = RepairRequestSignalrService(hubUrl)
        repairHub.setupListeners()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAppointmentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFilterSection()
        setupSwipeRefresh()
        setupCreateBookingButton()
        setupEmptyState()

        observeViewModel()
        observeSignalREvents()

        viewModel.loadInitialData()
    }


    // Lắng nghe event từ SignalR hub
    private fun observeSignalREvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repairHub.events.collect { repairRequestId ->
                    Log.d("SignalR", "Received event for repairRequestId=$repairRequestId -> refresh list")
                    // Bất cứ RepairRequest nào của user thay đổi -> reload list
                    viewModel.refreshData()
                }
            }
        }
    }

    // Kết nối / ngắt kết nối hub theo lifecycle
    override fun onStart() {
        super.onStart()

        val prefs = requireContext().getSharedPreferences(PREFS_AUTH, android.content.Context.MODE_PRIVATE)
        val userId = prefs.getString(KEY_USER_ID, null)
        currentUserIdForSignalR = userId

        if (!userId.isNullOrEmpty()) {
            Log.d("SignalR", "onStart: connectAndJoinUser($userId)")
            repairHub.connectAndJoinUser(userId)
        } else {
            Log.w("SignalR", "onStart: userId is null, not joining user group")
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d("SignalR", "onStop: leaveGroupAndStop()")
        repairHub.leaveUserGroupAndStop()
    }

    private fun setupEmptyState() {
        // Đổi text cho phù hợp màn Appointments
        binding.emptyState.tvEmptyTitle.text = "No appointments"
        binding.emptyState.tvEmptyMessage.text = "Your repair appointments will appear here."

        // Show nút + action tạo booking
        binding.emptyState.btnEmptyAction.apply {
            visibility = View.VISIBLE
            text = "Create appointment"
            setOnClickListener {
                navigateToBookingActivity()
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = RepairRequestAdapter(
            onItemClick = { showRepairRequestDetail(it) },
            onCancelClick = { cancelRepairRequest(it) }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@AppointmentsFragment.adapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (!canScrollVertically(1)) {
                        viewModel.loadMoreData()
                    }
                }
            })
        }
    }

    private fun setupCreateBookingButton() {
        binding.btnCreateBooking.setOnClickListener {
            navigateToBookingActivity()
        }
    }

    private fun navigateToBookingActivity() {
        val intent = Intent(requireContext(), BookingActivity::class.java)
        startActivity(intent)
    }

    private fun setupFilterSection() {
        // Toggle filter visibility
        binding.btnFilter.setOnClickListener {
            toggleFilterVisibility()
        }

        // Clear filter button
        binding.btnClearFilter.setOnClickListener {
            clearAllFilters()
        }

        // Status chips
        setupStatusSpinner()
        // Setup Spinners với adapter rỗng ban đầu
        setupVehicleSpinner()
        setupBranchSpinner()
    }

    private fun clearAllFilters() {
        // Clear status chips
        binding.spinnerStatus.setSelection(0)

        // Reset vehicle spinner to "All"
        binding.spinnerVehicle.setSelection(0)

        // Reset branch spinner to "All"
        binding.spinnerBranch.setSelection(0)

        // Clear all filters in ViewModel
        viewModel.clearAllFilters()

        Toast.makeText(requireContext(), "All filters cleared", Toast.LENGTH_SHORT).show()
    }

    private fun setupStatusSpinner() {
        // Tạo adapter với các trạng thái
        val statusAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.repair_status_array,
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.spinnerStatus.adapter = statusAdapter

        binding.spinnerStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Chỉ filter khi position > 0 (không phải "All Status")
                val status = when (position) {
                    1 -> 0 // Pending
                    2 -> 1 // Accepted
                    3 -> 2 // Arrived
                    4 -> 3 // Cancelled
                    else -> null // All Status
                }
                viewModel.filterByStatus(status)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Không làm gì
            }
        }
    }

    private fun setupVehicleSpinner() {
        // Tạo adapter với mảng rỗng ban đầu
        val adapter = ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            add("All") // Thêm item mặc định
        }

        binding.spinnerVehicle.adapter = adapter

        binding.spinnerVehicle.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Chỉ filter khi position > 0 (không phải "All Vehicles")
                if (position > 0) {
                    val vehicleId = viewModel.vehicles.value.getOrNull(position - 1)?.vehicleID
                    viewModel.filterByVehicle(vehicleId)
                } else {
                    viewModel.filterByVehicle(null)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Không làm gì
            }
        }
    }

    private fun setupBranchSpinner() {
        // Tạo adapter với mảng rỗng ban đầu
        val adapter = ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            add("All") // Thêm item mặc định
        }

        binding.spinnerBranch.adapter = adapter

        binding.spinnerBranch.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Chỉ filter khi position > 0 (không phải "All Branches")
                if (position > 0) {
                    val branchId = viewModel.branches.value.getOrNull(position - 1)?.branchId
                    viewModel.filterByBranch(branchId)
                } else {
                    viewModel.filterByBranch(null)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Không làm gì
            }
        }
    }

    private fun toggleFilterVisibility() {
        isFilterVisible = !isFilterVisible
        binding.cardFilter.visibility = if (isFilterVisible) View.VISIBLE else View.GONE

        val icon = if (isFilterVisible) R.drawable.ic_ft_expand_less else R.drawable.ic_filter
        binding.btnFilter.setImageResource(icon)
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshData()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.repairRequests.collect { requests ->
                adapter.submitList(requests)
                binding.swipeRefreshLayout.isRefreshing = false

                if (requests.isNullOrEmpty()) {
                    // Hiện empty state, ẩn list
                    binding.emptyState.root.visibility = View.VISIBLE
                    binding.swipeRefreshLayout.visibility = View.GONE
                } else {
                    // Có data: hiện list, ẩn empty state
                    binding.emptyState.root.visibility = View.GONE
                    binding.swipeRefreshLayout.visibility = View.VISIBLE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.vehicles.collect { vehicles ->
                updateVehicleSpinner(vehicles)
                adapter.setVehicles(vehicles)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.branches.collect { branches ->
                updateBranchSpinner(branches)
                adapter.setBranches(branches)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                if (!isLoading) {
                    binding.swipeRefreshLayout.isRefreshing = false
                }
            }
        }
    }

    private fun updateVehicleSpinner(vehicles: List<Vehicle>) {
        val adapter = ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // Thêm item "All Vehicles" đầu tiên
        adapter.add("All")

        // Thêm các vehicles
        vehicles.forEach { vehicle ->
            adapter.add("${vehicle.brandName} ${vehicle.modelName} - ${vehicle.licensePlate}")
        }

        binding.spinnerVehicle.adapter = adapter
    }

    private fun updateBranchSpinner(branches: List<Branch>) {
        val adapter = ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // Thêm item "All Branches" đầu tiên
        adapter.add("All")

        // Thêm các branches
        branches.forEach { branch ->
            adapter.add(branch.branchName)
        }

        binding.spinnerBranch.adapter = adapter
    }

    private fun showRepairRequestDetail(repairRequest: RepairRequest) {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            try {
                // Không cần gọi API detail ở đây nữa, để Fragment detail tự load
                val bundle = bundleOf(
                    "repairRequestId" to repairRequest.repairRequestID
                )

                findNavController().navigate(
                    R.id.action_global_appointmentDetailFragment,
                    bundle
                )
            } catch (e: Exception) {
                Log.d("repairRequest", e.message ?: "")
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun cancelRepairRequest(repairRequest: RepairRequest) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cancel appointment")
            .setMessage("Are you sure you want to cancel this repair request?")
            .setPositiveButton("Cancel request") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        binding.progressBar.visibility = View.VISIBLE

                        val result = repository.cancelRepairRequest(repairRequest.repairRequestID)

                        when (result) {
                            is NetworkResult.Success -> {
                                showSuccessDialog()
                                // Refresh lại danh sách
                                viewModel.refreshData()
                            }
                            is NetworkResult.Error -> {
                                showErrorDialog(result.message ?: "Failed to cancel request.")
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            requireContext(),
                            "Error: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    } finally {
                        binding.progressBar.visibility = View.GONE
                    }
                }
            }
            .setNegativeButton("Keep request", null)
            .show()
    }

    private fun showSuccessDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Success")
            .setMessage("Cancel successfully!")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                viewModel.refreshData()
            }
            .setCancelable(false)
            .show()
    }

    private fun showErrorDialog(message: String?) {
        AlertDialog.Builder(requireContext())
            .setTitle("Error")
            .setMessage(message ?: "Booking failed. Please try again.")
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshData()

    }
}
