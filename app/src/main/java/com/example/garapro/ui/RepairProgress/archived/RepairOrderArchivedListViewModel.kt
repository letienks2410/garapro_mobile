package com.example.garapro.ui.RepairProgress.archived

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garapro.data.model.RepairProgresses.PagedResult
import com.example.garapro.data.model.RepairProgresses.RepairOrderArchivedFilter
import com.example.garapro.data.model.RepairProgresses.RepairOrderArchivedListItem
import com.example.garapro.data.model.RepairProgresses.RoType
import com.example.garapro.data.remote.RepairProgressApiService
import com.example.garapro.data.remote.RetrofitInstance
import com.example.garapro.data.repository.RepairProgress.RepairProgressRepository
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

    fun loadOrders(filter: RepairOrderArchivedFilter? = null) {
        val newFilter = filter ?: currentFilter
        currentFilter = newFilter
        _filterState.value = newFilter

        viewModelScope.launch {
            _ordersState.value = RepairProgressRepository.ApiResponse.Loading()
            val result = repository.getArchivedRepairOrders(newFilter)
            _ordersState.value = result
        }
    }

    fun refresh() {
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

