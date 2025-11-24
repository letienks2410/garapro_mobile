package com.example.garapro.ui.repairRequest

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.garapro.R
import com.example.garapro.data.model.repairRequest.Service
import com.example.garapro.databinding.FragmentChildBookingCategorySelectionBinding

class ChildCategorySelectionFragment : BaseBookingFragment() {

    private var _binding: FragmentChildBookingCategorySelectionBinding? = null
    private val binding get() = _binding!!

    private lateinit var serviceAdapter: ServiceSimpleAdapter

    // State filter
    private var currentFilterCategoryId: String? = null
    private var currentFilterCategoryName: String = "All"

    // Cờ để tránh vòng lặp spinner -> API
    private var isUpdatingSpinner = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChildBookingCategorySelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        restoreFilterState()
        setupAdapters()
        setupFilterSpinner()
        setupObservers()
        setupListeners()
        setupSearch()

        applyCurrentFilter()

        Log.d("FilterDebug", "onViewCreated - currentFilter: $currentFilterCategoryId, name: $currentFilterCategoryName")
    }

    override fun onResume() {
        super.onResume()

    }

    private fun restoreFilterState() {
        val savedFilterState = bookingViewModel.getChildFilterState()
        if (savedFilterState != null) {
            currentFilterCategoryId = savedFilterState.categoryId
            currentFilterCategoryName = savedFilterState.categoryName ?: "All"

            savedFilterState.searchTerm?.let { searchTerm ->
                binding.etSearch.setText(searchTerm)
                Log.d("FilterDebug", "Restored search term: $searchTerm")
            }
        } else {
            Log.d("FilterDebug", "No saved filter state")
        }
    }

    private fun applyCurrentFilter() {
        Log.d("FilterDebug", "Applying current filter - id: $currentFilterCategoryId")

        loadChildCategories(
            childServiceCategoryId = currentFilterCategoryId,
            searchTerm = bookingViewModel.getChildFilterState()?.searchTerm
        )
    }

    private fun setupAdapters() {
        serviceAdapter = ServiceSimpleAdapter(
            services = emptyList(),
            onServiceSelected = { service ->
                bookingViewModel.toggleServiceSelection(service)
            },
            isServiceSelected = { service ->
                bookingViewModel.isServiceSelected(service)
            }
        )

        binding.rvChildCategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = serviceAdapter
        }
    }

    private fun setupFilterSpinner() {
        // Tạm thời set adapter trống, sẽ update khi có categories từ ViewModel
        val initialAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            mutableListOf("All")
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.spinnerFilter.adapter = initialAdapter

        binding.spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                // Nếu đang update spinner bằng code -> bỏ qua
                if (isUpdatingSpinner) {
                    Log.d("FilterDebug", "onItemSelected ignored (isUpdatingSpinner = true)")
                    return
                }

                val categories = bookingViewModel.allChildCategories.value ?: emptyList()

                val newCategoryId: String?
                val newCategoryName: String

                if (position == 0) {
                    newCategoryId = null
                    newCategoryName = "All"
                } else {
                    val category = categories.getOrNull(position - 1)
                    newCategoryId = category?.serviceCategoryId
                    newCategoryName = category?.categoryName ?: "All"
                }

                // Nếu không thay đổi filter -> không call API lại
                if (newCategoryId == currentFilterCategoryId) {
                    Log.d("FilterDebug", "Filter not changed, skip API call")
                    return
                }

                currentFilterCategoryId = newCategoryId
                currentFilterCategoryName = newCategoryName

                // Lưu lại filter vào ViewModel
                saveFilterState()

                // Load lại data theo filter
                loadChildCategories(
                    childServiceCategoryId = currentFilterCategoryId,
                    searchTerm = bookingViewModel.getChildFilterState()?.searchTerm
                )

                Log.d("FilterDebug", "Spinner selected - id: $currentFilterCategoryId, name: $currentFilterCategoryName")
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Không làm gì
            }
        }
    }

    private fun updateFilterSpinner() {
        val categories = bookingViewModel.allChildCategories.value ?: emptyList()

        val names = mutableListOf("All")
        names.addAll(categories.map { it.categoryName })

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            names
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // 🔒 Bật flag để onItemSelected không chạy trong lúc cập nhật
        isUpdatingSpinner = true

        binding.spinnerFilter.adapter = adapter

        // Set selection theo currentFilterCategoryId
        val index = if (currentFilterCategoryId == null) {
            0
        } else {
            val pos = categories.indexOfFirst { it.serviceCategoryId == currentFilterCategoryId }
            if (pos >= 0) pos + 1 else 0
        }

        binding.spinnerFilter.setSelection(index, false)

        // 🔓 Mở lại flag để user chọn mới thì xử lý
        isUpdatingSpinner = false
    }

    private fun setupObservers() {
        // Dùng allChildCategories để build danh sách filter cho Spinner
        bookingViewModel.allChildCategories.observe(viewLifecycleOwner) { categories ->
            Log.d("FilterDebug", "All categories updated - ${categories.size} categories")
            updateFilterSpinner()
        }

        bookingViewModel.childServiceCategories.observe(viewLifecycleOwner) { response ->
            val services = response.data.flatMap { it.services }
            Log.d("FilterDebug", "Filtered services updated - ${services.size} services")
            serviceAdapter.updateData(services)
        }

        bookingViewModel.selectedServices.observe(viewLifecycleOwner) { services ->
            updateSelectedServicesUI(services)
        }

        bookingViewModel.selectedParentCategory.observe(viewLifecycleOwner) { parent ->
            parent?.let {
                binding.tvSelectedParent.text = "Category: ${it.categoryName}"
            }
        }
    }

    private fun updateSelectedServicesUI(services: List<Service>) {
        binding.tvSelectedCount.text = "Đã chọn: ${services.size} dịch vụ"
        updateSelectedServicesPreview(services)
    }

    private fun updateSelectedServicesPreview(services: List<Service>) {
        val container = binding.containerSelectedPreview
        container.removeAllViews()

        if (services.isNotEmpty()) {
            container.visibility = View.VISIBLE
            // TODO: thêm UI hiển thị preview nếu cần
        } else {
            container.visibility = View.GONE
        }
    }

    private fun saveFilterState(searchTerm: String? = null) {
        val currentSearchTerm = searchTerm ?: binding.etSearch.text?.toString()?.trim()

        val filterState = BookingViewModel.ChildFilterState(
            categoryId = currentFilterCategoryId,
            categoryName = currentFilterCategoryName,
            searchTerm = if (currentSearchTerm.isNullOrEmpty()) null else currentSearchTerm
        )
        bookingViewModel.setChildFilterState(filterState)

        Log.d(
            "FilterDebug",
            "Filter state saved - id: $currentFilterCategoryId, name: $currentFilterCategoryName, search: $currentSearchTerm"
        )
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            private var searchRunnable: Runnable? = null
            private val handler = Handler(Looper.getMainLooper())

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(editable: Editable?) {
                searchRunnable?.let { handler.removeCallbacks(it) }

                searchRunnable = Runnable {
                    val searchTerm = editable?.toString()?.trim()

                    // Lưu search term vào ViewModel
                    saveFilterState(searchTerm = if (searchTerm.isNullOrEmpty()) null else searchTerm)

                    loadChildCategories(
                        childServiceCategoryId = currentFilterCategoryId,
                        searchTerm = searchTerm
                    )
                }

                handler.postDelayed(searchRunnable!!, 500)
            }
        })
    }

    private fun setupListeners() {
        binding.btnPrevious.setOnClickListener {
            showPreviousFragment()
        }

        binding.btnNext.setOnClickListener {
            val selectedServices = bookingViewModel.selectedServices.value ?: emptyList()
            showNextFragment(R.id.action_childCategorySelection_to_details)
        }

        binding.btnNext.isEnabled = true
    }

    private fun loadChildCategories(
        childServiceCategoryId: String? = null,
        searchTerm: String? = null
    ) {
        val parentId = bookingViewModel.selectedParentCategory.value?.serviceCategoryId
        parentId?.let {
            bookingViewModel.loadChildServiceCategories(
                parentId = it,
                childServiceCategoryId = childServiceCategoryId,
                searchTerm = searchTerm
            )
        }
    }

    override fun onDestroyView() {
        saveFilterState()
        super.onDestroyView()
        _binding = null
    }
}
