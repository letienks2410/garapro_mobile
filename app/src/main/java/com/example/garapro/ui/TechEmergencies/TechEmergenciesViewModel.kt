package com.example.garapro.ui.TechEmergencies

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garapro.data.model.techEmergencies.EmergencyDetailDto
import com.example.garapro.data.model.techEmergencies.EmergencyForTechnicianDto
import com.example.garapro.data.repository.emergencies.TechnicianEmergencyRepository
import kotlinx.coroutines.launch

class TechEmergenciesViewModel (
    private val repo: TechnicianEmergencyRepository = TechnicianEmergencyRepository()
) : ViewModel() {

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    private val _updateResult = MutableLiveData<Boolean>()
    val updateResult: LiveData<Boolean> get() = _updateResult
    private val _current = MutableLiveData<EmergencyForTechnicianDto?>()
    val current: LiveData<EmergencyForTechnicianDto?> get() = _current

    private val _list = MutableLiveData<List<EmergencyForTechnicianDto>>()
    val list: LiveData<List<EmergencyForTechnicianDto>> get() = _list

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    private val _detail = MutableLiveData<EmergencyDetailDto?>()
    val detail: LiveData<EmergencyDetailDto?> get() = _detail
    private val _detailLoading = MutableLiveData<Boolean>()
    val detailLoading: LiveData<Boolean> get() = _detailLoading

    fun loadDetail(id: String) {
        viewModelScope.launch {
            _detailLoading.value = true
            _detail.value = null

            val data = repo.getEmergencyDetail(id)

            _detailLoading.value = false
            if (data == null) {
                _error.value = "Cannot load detail"
            } else {
                _detail.value = data
            }
        }
    }
    fun loadData() {
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null

                val response = repo.getMyEmergencies()

                _loading.value = false

                if (response == null) {
                    _error.value = "Cannot load data"
                    _list.value = emptyList()
                    _current.value = null
                    return@launch
                }

                if (response.current != null) {
                    _current.value = response.current
                    // list vẫn set để tránh null
                    _list.value = response.list ?: emptyList()
                } else {
                    _current.value = null
                    _list.value = response.list ?: emptyList()
                }

            } catch (e: Exception) {
                _loading.value = false
                _error.value = e.message
                _list.value = emptyList()
                _current.value = null
            }
        }
    }
    fun updateStatus(id: String, status: Int, reason: String? = null) {
        viewModelScope.launch {
            val ok = repo.updateEmergencyStatus(id, status, reason)
            _updateResult.postValue(ok)
        }
    }
}