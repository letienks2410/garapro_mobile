package com.example.garapro.ui.vehicles

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garapro.data.model.Vehicles.Brand
import com.example.garapro.data.model.Vehicles.Model
import com.example.garapro.data.model.Vehicles.CreateVehicles
import com.example.garapro.data.model.Vehicles.ModelColor
import com.example.garapro.data.model.Vehicles.UpdateVehicles
import com.example.garapro.data.model.Vehicles.Vehicle
import com.example.garapro.data.repository.ApiResponse
import com.example.garapro.data.repository.VehicleRepository
import kotlinx.coroutines.launch

class VehicleViewModel(private val repository: VehicleRepository) : ViewModel() {

    // --- LIVE DATA CHO DROPDOWN ---
    private val _brands = MutableLiveData<List<Brand>>()
    val brands: LiveData<List<Brand>> = _brands

    private val _models = MutableLiveData<List<Model>>()
    val models: LiveData<List<Model>> = _models

    private val _colors = MutableLiveData<List<ModelColor>>()
    val colors: LiveData<List<ModelColor>> = _colors

    // --- LIVE DATA CHO DANH SÁCH XE (Dữ liệu chính) ---
    private val _vehicleListStatus = MutableLiveData<ApiResponse<List<Vehicle>>>()
    val vehicleListStatus: LiveData<ApiResponse<List<Vehicle>>> = _vehicleListStatus

    private val _vehicleDetailStatus = MutableLiveData<ApiResponse<Vehicle>>()
    val vehicleDetailStatus: LiveData<ApiResponse<Vehicle>> = _vehicleDetailStatus

    // --- LIVE DATA CHO CÁC HÀNH ĐỘNG (Create/Update/Delete) ---
    private val _actionStatus = MutableLiveData<ApiResponse<Unit>>()
    val actionStatus: LiveData<ApiResponse<Unit>> = _actionStatus

    // =========================================================================

    // --- LOGIC LẤY DỮ LIỆU DROPDOWN ---
    fun fetchAllDropdownData() {
        viewModelScope.launch {
            // Tải Brands
            val brandsResult = repository.fetchBrands()
            if (brandsResult is ApiResponse.Success) {
                _brands.value = brandsResult.data
            }
        }
    }

    fun fetchVehicleDetail(vehicleId: String) {
        _vehicleDetailStatus.value = ApiResponse.Loading()
        viewModelScope.launch {
            _vehicleDetailStatus.value = repository.fetchVehicleDetail(vehicleId)
        }
    }

    fun fetchModelsByBrand(brandId: String) {
        // Xóa trạng thái cũ để báo UI đang tải dữ liệu mới
        _models.value = emptyList()
        _colors.value = emptyList()
        viewModelScope.launch {
            when (val result = repository.fetchModelsByBrand(brandId)) {
                is ApiResponse.Success -> _models.value = result.data
                is ApiResponse.Error -> _models.value = emptyList()
                else -> {}
            }
        }
    }

    fun fetchColorsByModel(modelId: String) {
        _colors.value = emptyList()
        viewModelScope.launch {
            when (val result = repository.fetchColorsByModel(modelId)) {
                is ApiResponse.Success -> _colors.value = result.data
                is ApiResponse.Error -> _colors.value = emptyList()
                else -> {}
            }
        }
    }

    // --- LOGIC LẤY DANH SÁCH XE ---
    fun fetchVehicles() {
        // Khắc phục lỗi: Chỉ định rõ kiểu dữ liệu List<Vehicle>
        _vehicleListStatus.value = ApiResponse.Loading<List<Vehicle>>()
        viewModelScope.launch {
            _vehicleListStatus.value = repository.fetchVehicles()
        }
    }

    // --- LOGIC HÀNH ĐỘNG (CRUD) ---

    fun createVehicle(request: CreateVehicles) {
        // ActionStatus chỉ cần kiểu Unit
        _actionStatus.value = ApiResponse.Loading<Unit>()
        viewModelScope.launch {
            val result = repository.createVehicle(request)
            _actionStatus.value = result
            if (result is ApiResponse.Success) {
                fetchVehicles()
            }
        }
    }

    fun deleteVehicle(vehicleId: String) {
        // ActionStatus chỉ cần kiểu Unit
        _actionStatus.value = ApiResponse.Loading<Unit>()
        viewModelScope.launch {
            val result = repository.deleteVehicle(vehicleId)
            _actionStatus.value = result
            if (result is ApiResponse.Success) {
                fetchVehicles()
            }
        }
    }

    fun updateVehicle(vehicleId: String, request: UpdateVehicles) {
        _actionStatus.value = ApiResponse.Loading()
        viewModelScope.launch {
            val result = repository.updateVehicle(vehicleId, request)
            _actionStatus.value = result
            if (result is ApiResponse.Success) {
                fetchVehicles()
                fetchVehicleDetail(vehicleId)
            }
        }
    }
}