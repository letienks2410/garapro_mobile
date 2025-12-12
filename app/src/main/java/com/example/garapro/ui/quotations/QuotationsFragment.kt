package com.example.garapro.ui.quotations

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.garapro.R
import com.example.garapro.data.model.quotations.Quotation
import com.example.garapro.data.model.quotations.QuotationStatus
import com.example.garapro.data.remote.RetrofitInstance
import com.example.garapro.data.repository.QuotationRepository
import com.example.garapro.databinding.FragmentQuotationsBinding
import com.example.garapro.hubs.QuotationSignalRService
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class QuotationsFragment : Fragment() {

    private var _binding: FragmentQuotationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: QuotationListViewModel
    private var isFilterVisible = false

    private var quotationHubService: QuotationSignalRService? = null

    private lateinit var quotationAdapter: QuotationAdapter

    companion object {
        private const val PREFS_AUTH = "auth_prefs"
        private const val KEY_USER_ID = "user_id"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuotationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repository = QuotationRepository(RetrofitInstance.quotationService)
        viewModel = QuotationListViewModel(repository)

        setupRecyclerView()
        setupUI()
        setupObservers()
        initQuotationHub()
        observeQuotationHubEvents()
    }

    private fun setupRecyclerView() {
        quotationAdapter = QuotationAdapter { quotation ->
            val bundle = Bundle().apply {
                putString("quotationId", quotation.quotationId)
            }
            findNavController().navigate(
                R.id.action_global_quotationDetailFragment,
                bundle
            )
        }

        binding.rvQuotations.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = quotationAdapter

            // Phân trang khi scroll đến cuối
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    // chỉ xử lý khi scroll xuống
                    if (dy <= 0) return

                    val pagination = viewModel.paginationInfo.value
                    val isLoading = viewModel.isLoading.value == true

                    if (pagination != null &&
                        !isLoading &&
                        pagination.pageNumber < pagination.totalPages &&
                        !recyclerView.canScrollVertically(1)
                    ) {
                        // load trang tiếp theo
                        viewModel.loadQuotations(pagination.pageNumber + 1, pagination.pageSize)
                    }
                }
            })
        }
    }

    private fun setupUI() {

        val statusItems = listOf(
            "All",
            "Sent",
            "Approved",
            "Rejected",
            "Expired",
            "Good"
        )

        val statusAdapter = android.widget.ArrayAdapter(
            requireContext(),

            android.R.layout.simple_spinner_item,
            statusItems
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerStatus.adapter = statusAdapter


        binding.swipeRefresh.setOnRefreshListener {
            // load lại từ trang 1
            viewModel.loadQuotations(pageNumber = 1)
        }

        // Toggle filter visibility
        binding.btnFilter.setOnClickListener {
            toggleFilterVisibility()
        }

        // Apply filter
        binding.btnApplyFilter.setOnClickListener {
            applyFilter()
            toggleFilterVisibility()
        }

        // Clear filter
        binding.btnClearFilter.setOnClickListener {
            clearFilter()
        }

    }

    private fun initQuotationHub() {
        val prefs = requireContext().getSharedPreferences(PREFS_AUTH, Context.MODE_PRIVATE)
        val userId = prefs.getString(KEY_USER_ID, null)

        if (userId.isNullOrEmpty()) {
            return
        }

        val hubUrl = com.example.garapro.utils.Constants.BASE_URL_SIGNALR + "/hubs/quotation"

        quotationHubService = QuotationSignalRService(hubUrl).apply {
            setupListeners()
            startAndJoinUser(userId)
        }
    }

    private fun observeQuotationHubEvents() {
        val service = quotationHubService
        if (service == null) {
            Log.w("SignalR", "observeQuotationHubEvents: quotationHubService is null")
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            Log.d("SignalR", "Start collecting quotationHubService.events")
            service.events.collect {
                Log.d("R", "load from SignalR")
                viewModel.loadQuotations(pageNumber = 1)
            }
        }
    }

    private fun toggleFilterVisibility() {
        isFilterVisible = !isFilterVisible

        if (isFilterVisible) {
            binding.cardFilter.visibility = View.VISIBLE
            binding.btnFilter.setImageResource(R.drawable.ic_ft_expand_less)
        } else {
            binding.cardFilter.visibility = View.GONE
            binding.btnFilter.setImageResource(R.drawable.ic_filter)
        }
    }

    private fun applyFilter() {
        val position = binding.spinnerStatus.selectedItemPosition
        val status = when (position) {
            0 -> null
            1 -> QuotationStatus.Sent
            2 -> QuotationStatus.Approved
            3 -> QuotationStatus.Rejected
            4 -> QuotationStatus.Expired
            5 -> QuotationStatus.Good
            else -> null
        }

        viewModel.filterByStatus(status)
    }

    private fun clearFilter() {
        binding.spinnerStatus.setSelection(0)
        viewModel.filterByStatus(null)
    }

    private fun updateChipAppearance() {
        // Dùng style Chip.Filter nên không cần custom thêm
    }

    private fun setupObservers() {
        viewModel.quotations.observe(viewLifecycleOwner) { quotations ->
            binding.swipeRefresh.isRefreshing = false
            if (quotations.isNullOrEmpty()) {
                showEmptyState()
            } else {
                showQuotations(quotations)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading == true) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.selectedStatus.observe(viewLifecycleOwner) { status ->
            updateSelectedFilter(status)
        }
    }

    private fun updateSelectedFilter(status: QuotationStatus?) {
        val position = when (status) {
            null -> 0
            QuotationStatus.Sent -> 1
            QuotationStatus.Approved -> 2
            QuotationStatus.Rejected -> 3
            QuotationStatus.Expired -> 4
            QuotationStatus.Good -> 5
            QuotationStatus.Pending -> 0
        }
        binding.spinnerStatus.setSelection(position)
    }

    private fun showEmptyState() {
        binding.emptyState.root.visibility = View.VISIBLE
        binding.rvQuotations.visibility = View.GONE
    }

    private fun showQuotations(quotations: List<Quotation>) {
        binding.emptyState.root.visibility = View.GONE
        binding.rvQuotations.visibility = View.VISIBLE
        quotationAdapter.submitList(quotations)
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadQuotations(pageNumber = 1)

    }

    override fun onDestroyView() {
        super.onDestroyView()

        try {
            quotationHubService?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        quotationHubService = null
        _binding = null
    }
}
