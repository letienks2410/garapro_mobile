package com.example.garapro.ui.RepairProgress.archived

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garapro.data.model.RepairProgresses.CarPickupStatus
import com.example.garapro.data.model.RepairProgresses.RepairOrderArchivedDetail
import com.example.garapro.data.remote.RepairProgressApiService
import com.example.garapro.data.remote.RetrofitInstance
import com.example.garapro.data.repository.RepairProgress.RepairProgressRepository
import kotlinx.coroutines.launch

class RepairOrderArchivedDetailViewModel(
    private val repository: RepairProgressRepository
) : ViewModel() {

    private val _detailState =
        MutableLiveData<RepairProgressRepository.ApiResponse<RepairOrderArchivedDetail>>()
    val detailState: LiveData<RepairProgressRepository.ApiResponse<RepairOrderArchivedDetail>>
        get() = _detailState

    private val _updateStatusState =
        MutableLiveData<RepairProgressRepository.ApiResponse<Unit>>()
    val updateStatusState: LiveData<RepairProgressRepository.ApiResponse<Unit>>
        get() = _updateStatusState

    fun loadDetail(repairOrderId: String) {
        viewModelScope.launch {
            _detailState.value = RepairProgressRepository.ApiResponse.Loading()
            _detailState.value = repository.getArchivedRepairOrderDetail(repairOrderId)
        }
    }

    fun updateCarPickupStatus(repairOrderId: String, status: CarPickupStatus) {
        viewModelScope.launch {
            _updateStatusState.value = RepairProgressRepository.ApiResponse.Loading()
            _updateStatusState.value = repository.updateCarPickupStatus(repairOrderId, status)
        }
    }
}