package com.example.garapro.ui.RepairProgress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garapro.data.model.RepairProgresses.FilterChipData
import com.example.garapro.data.model.RepairProgresses.FilterType
import com.example.garapro.data.model.RepairProgresses.OrderStatus
import com.example.garapro.data.model.RepairProgresses.PagedResult
import com.example.garapro.data.model.RepairProgresses.RepairOrderFilter
import com.example.garapro.data.model.RepairProgresses.RepairOrderListItem
import com.example.garapro.data.model.RepairProgresses.RepairProgressDetail
import com.example.garapro.data.model.RepairProgresses.RoType
import com.example.garapro.data.model.payments.CreatePaymentRequest
import com.example.garapro.data.model.payments.CreatePaymentResponse
import com.example.garapro.data.model.payments.PaymentStatusDto
import com.example.garapro.data.remote.RetrofitInstance
import com.example.garapro.data.repository.RepairProgress.RepairProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RepairProgressViewModel : ViewModel() {

    private val repository = RepairProgressRepository(RetrofitInstance.RepairProgressService)


    private val _repairOrders =
        MutableStateFlow<RepairProgressRepository.ApiResponse<PagedResult<RepairOrderListItem>>>(
            RepairProgressRepository.ApiResponse.Loading()
        )
    val repairOrders: StateFlow<RepairProgressRepository.ApiResponse<PagedResult<RepairOrderListItem>>> =
        _repairOrders


    private val _repairOrderDetail =
        MutableStateFlow<RepairProgressRepository.ApiResponse<RepairProgressDetail>?>(null)
    val repairOrderDetail: StateFlow<RepairProgressRepository.ApiResponse<RepairProgressDetail>?> =
        _repairOrderDetail

    private val _paymentStatus =
        MutableStateFlow<RepairProgressRepository.ApiResponse<PaymentStatusDto>?>(null)
    val paymentStatus: StateFlow<RepairProgressRepository.ApiResponse<PaymentStatusDto>?> =
        _paymentStatus

    private val _createPaymentResponse =
        MutableStateFlow<RepairProgressRepository.ApiResponse<CreatePaymentResponse>?>(null)
    val createPaymentResponse: StateFlow<RepairProgressRepository.ApiResponse<CreatePaymentResponse>?> =
        _createPaymentResponse

    private val _orderStatuses = MutableStateFlow<List<OrderStatus>>(emptyList())
    val orderStatuses: StateFlow<List<OrderStatus>> = _orderStatuses

    private val _filterState = MutableStateFlow(RepairOrderFilter())
    val filterState: StateFlow<RepairOrderFilter> = _filterState

    private val _showFilter = MutableStateFlow(false)
    val showFilter: StateFlow<Boolean> = _showFilter

    private val _filterChips = MutableStateFlow<List<FilterChipData>>(emptyList())
    val filterChips: StateFlow<List<FilterChipData>> = _filterChips

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading


    private var currentPage = 1
    private var totalPages = 1

    init {
        loadOrderStatuses()
        loadFirstPage()
    }


    fun loadFirstPage() {
        currentPage = 1
        loadRepairOrders(page = 1, isLoadMore = false)
    }

    // 🔹 Hàm load chung (hỗ trợ loadMore)
    fun loadRepairOrders(page: Int = 1, isLoadMore: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true

            // Khi loadMore thì giữ UI hiện tại, không set lại Loading() để tránh đè empty state
            if (!isLoadMore) {
                _repairOrders.value = RepairProgressRepository.ApiResponse.Loading()
            }

            val filterForPage = _filterState.value.copy(pageNumber = page)
            val result = repository.getMyRepairOrders(filterForPage)

            when (result) {
                is RepairProgressRepository.ApiResponse.Success -> {
                    val paged = result.data
                    currentPage = paged.pageNumber
                    totalPages = paged.totalPages

                    // Lấy list cũ (nếu trước đó đã có Success)
                    val oldItems =
                        ( _repairOrders.value as? RepairProgressRepository.ApiResponse.Success )
                            ?.data
                            ?.items
                            ?: emptyList()

                    // Nếu loadMore thì cộng dồn, còn không thì thay mới
                    val mergedItems = if (isLoadMore) {
                        oldItems + paged.items
                    } else {
                        paged.items
                    }

                    val mergedPagedResult = paged.copy(items = mergedItems)
                    _repairOrders.value = RepairProgressRepository.ApiResponse.Success(mergedPagedResult)

                    updateFilterChips()
                }

                is RepairProgressRepository.ApiResponse.Error -> {
                    _repairOrders.value = result
                }

                is RepairProgressRepository.ApiResponse.Loading -> {
                    // không dùng nhánh này ở đây
                }
            }

            _isLoading.value = false
        }
    }

    // 🔹 Load trang tiếp theo (dùng cho scroll cuối list)
    fun loadNextPage() {
        if (_isLoading.value) return
        if (currentPage >= totalPages) return

        loadRepairOrders(page = currentPage + 1, isLoadMore = true)
    }

    suspend fun createPaymentLinkDirect(createPaymentRequest: CreatePaymentRequest): CreatePaymentResponse? {
        return try {
            val result = repository.createPaymentLink(createPaymentRequest)
            when (result) {
                is RepairProgressRepository.ApiResponse.Success -> result.data
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getCheckoutUrl(): String? {
        return when (val response = _createPaymentResponse.value) {
            is RepairProgressRepository.ApiResponse.Success -> response.data.checkoutUrl
            else -> null
        }
    }

    fun getPaymentStatus(orderCode: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _paymentStatus.value = RepairProgressRepository.ApiResponse.Loading()
            val result = repository.getPaymentStatus(orderCode)
            _paymentStatus.value = result
            _isLoading.value = false
        }
    }

    fun clearPaymentResponse() {
        _createPaymentResponse.value = null
    }

    fun loadRepairOrderDetail(repairOrderId: String) {
        viewModelScope.launch {
            _repairOrderDetail.value = RepairProgressRepository.ApiResponse.Loading()
            val result = repository.getRepairOrderDetail(repairOrderId)
            _repairOrderDetail.value = result
        }
    }

    private fun loadOrderStatuses() {
        viewModelScope.launch {
            val result = repository.getOrderStatuses()
            if (result is RepairProgressRepository.ApiResponse.Success) {
                _orderStatuses.value = result.data
            }
        }
    }

    fun updateFilter(newFilter: RepairOrderFilter) {
        _filterState.value = newFilter.copy(pageNumber = 1)
        loadFirstPage()
    }

    fun toggleFilterVisibility() {
        _showFilter.value = !_showFilter.value
    }

    fun clearFilter() {
        _filterState.value = RepairOrderFilter()
        loadFirstPage()
    }

    fun updateStatusFilter(statusId: Int?) {
        _filterState.value = _filterState.value.copy(statusId = statusId, pageNumber = 1)
        loadFirstPage()
    }

    fun updateRoTypeFilter(roType: RoType?) {
        _filterState.value = _filterState.value.copy(roType = roType, pageNumber = 1)
        loadFirstPage()
    }

    fun updatePaidStatusFilter(paidStatus: String?) {
        _filterState.value = _filterState.value.copy(paidStatus = paidStatus, pageNumber = 1)
        loadFirstPage()
    }

    fun updateDateFilter(fromDate: String?, toDate: String?) {
        _filterState.value = _filterState.value.copy(
            fromDate = fromDate,
            toDate = toDate,
            pageNumber = 1
        )
        loadFirstPage()
    }

    fun removeFilterChip(chipId: String) {
        val chip = _filterChips.value.find { it.id == chipId }
        chip?.let {
            when (it.type) {
                FilterType.STATUS -> updateStatusFilter(null)
                FilterType.RO_TYPE -> updateRoTypeFilter(null)
                FilterType.PAID_STATUS -> updatePaidStatusFilter(null)
                FilterType.DATE -> updateDateFilter(null, null)
            }
        }
    }

    private fun updateFilterChips() {
        val chips = mutableListOf<FilterChipData>()

        _filterState.value.statusId?.let { statusId ->
            val statusName =
                _orderStatuses.value.find { it.orderStatusId == statusId }?.statusName ?: "Status"
            chips.add(
                FilterChipData(
                    "status_$statusId",
                    statusName,
                    FilterType.STATUS,
                    true
                )
            )
        }

        _filterState.value.roType?.let { roType ->
            chips.add(
                FilterChipData(
                    "roType_${roType.name}",
                    roType.name,
                    FilterType.RO_TYPE,
                    true
                )
            )
        }

        _filterState.value.paidStatus?.let { paidStatus ->
            chips.add(
                FilterChipData(
                    "paid_$paidStatus",
                    paidStatus,
                    FilterType.PAID_STATUS,
                    true
                )
            )
        }

        if (_filterState.value.fromDate != null || _filterState.value.toDate != null) {
            val dateText =
                "${_filterState.value.fromDate ?: ""} - ${_filterState.value.toDate ?: ""}"
            chips.add(FilterChipData("date_range", dateText, FilterType.DATE, true))
        }

        _filterChips.value = chips
    }
}
