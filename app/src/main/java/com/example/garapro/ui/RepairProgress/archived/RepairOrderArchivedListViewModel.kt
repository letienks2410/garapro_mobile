package com.example.garapro.ui.RepairProgress.archived

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garapro.data.model.RepairProgresses.PagedResult
import com.example.garapro.data.model.RepairProgresses.RepairOrderArchivedFilter
import com.example.garapro.data.model.RepairProgresses.RepairOrderArchivedListItem
import com.example.garapro.data.model.RepairProgresses.RoType
import com.example.garapro.data.repository.RepairProgress.RepairProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RepairOrderArchivedListViewModel(
    private val repository: RepairProgressRepository
) : ViewModel() {

    private val _ordersState =
        MutableLiveData<RepairProgressRepository.ApiResponse<PagedResult<RepairOrderArchivedListItem>>>()
    val ordersState: LiveData<RepairProgressRepository.ApiResponse<PagedResult<RepairOrderArchivedListItem>>>
        get() = _ordersState

    // filter hiện tại
    private val _filterState = MutableLiveData(RepairOrderArchivedFilter())
    val filterState: LiveData<RepairOrderArchivedFilter> get() = _filterState

    private var currentFilter = RepairOrderArchivedFilter()

    // 🔹 Biến phục vụ phân trang
    private var currentPage = 1
    private var totalPages = 1
    private val _isLoadingPage = MutableStateFlow(false)
    val isLoadingPage: StateFlow<Boolean> = _isLoadingPage

    /**
     * Hàm cũ: mặc định load lại từ trang 1 (dùng cho lần đầu / refresh / đổi filter)
     */
    fun loadOrders(filter: RepairOrderArchivedFilter? = null) {
        loadPage(page = 1, isLoadMore = false, filterOverride = filter)
    }

    /**
     * 🔹 Hàm dùng chung cho cả load trang đầu và loadMore
     */
    private fun loadPage(
        page: Int,
        isLoadMore: Boolean,
        filterOverride: RepairOrderArchivedFilter? = null
    ) {
        // chặn nếu đang load page
        if (_isLoadingPage.value) return

        val newFilter = (filterOverride ?: currentFilter).copy(pageNumber = page)
        currentFilter = newFilter
        _filterState.value = newFilter

        viewModelScope.launch {
            _isLoadingPage.value = true

            // chỉ show loading UI khi load trang đầu
            if (!isLoadMore) {
                _ordersState.value = RepairProgressRepository.ApiResponse.Loading()
            }

            val result = repository.getArchivedRepairOrders(newFilter)

            when (result) {
                is RepairProgressRepository.ApiResponse.Success -> {
                    val paged = result.data
                    currentPage = paged.pageNumber
                    totalPages = paged.totalPages

                    val newItems = paged.items ?: emptyList()

                    val mergedItems = if (isLoadMore) {
                        val oldItems =
                            (ordersState.value as? RepairProgressRepository.ApiResponse.Success)
                                ?.data
                                ?.items
                                ?: emptyList()
                        oldItems + newItems
                    } else {
                        newItems
                    }

                    val mergedPaged = paged.copy(items = mergedItems)
                    _ordersState.value = RepairProgressRepository.ApiResponse.Success(mergedPaged)
                }

                is RepairProgressRepository.ApiResponse.Error -> {
                    _ordersState.value = result
                }

                is RepairProgressRepository.ApiResponse.Loading -> {
                    // không dùng nhánh này ở đây
                }
            }

            _isLoadingPage.value = false
        }
    }

    /**
     * 🔹 Gọi khi kéo xuống cuối danh sách
     */
    fun loadNextPage() {
        if (_isLoadingPage.value) return
        if (currentPage >= totalPages) return

        loadPage(page = currentPage + 1, isLoadMore = true)
    }

    fun refresh() {
        // refresh luôn từ page 1
        loadOrders(currentFilter)
    }

    fun clearFilter() {
        val default = RepairOrderArchivedFilter()
        currentFilter = default
        _filterState.value = default
        loadOrders(default)
    }

    fun updateRoTypeFilter(roType: RoType?) {
        val updated = currentFilter.copy(
            roType = roType,
            pageNumber = 1
        )
        loadOrders(updated)
    }

    fun updatePaidStatusFilter(paidStatus: String?) {
        val updated = currentFilter.copy(
            paidStatus = paidStatus,
            pageNumber = 1
        )
        loadOrders(updated)
    }

    fun updateDateRangeFilter(fromDate: String?, toDate: String?) {
        val updated = currentFilter.copy(
            fromDate = fromDate,
            toDate = toDate,
            pageNumber = 1
        )
        loadOrders(updated)
    }
}
