package com.example.garapro.ui.emergencies

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garapro.data.remote.RetrofitInstance
import kotlinx.coroutines.launch

class EmergencyListViewModel : ViewModel() {
    private val _items = MutableLiveData<List<EmergencySummary>>(emptyList())
    val items: LiveData<List<EmergencySummary>> = _items

    fun loadPending() {
        viewModelScope.launch {
            try {
                val resp = RetrofitInstance.emergencyService.getPendingEmergencies()
                if (resp.isSuccessful) {
                    val list = resp.body() ?: emptyList()
                    _items.value = list.map {
                        EmergencySummary(
                            id = it.id,
                            vehicleTitle = "Emergency ${it.id.take(8)}",
                            issue = "Roadside assistance",
                            status = it.status.name,
                            time = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(java.util.Date(it.timestamp)),
                            garageName = it.assignedGarageId ?: ""
                        )
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun loadByCustomer(customerId: String) {
        viewModelScope.launch {
            try {
                val resp = RetrofitInstance.emergencyService.getEmergenciesByCustomer(customerId)
                if (resp.isSuccessful) {
                    val list = resp.body() ?: emptyList()
                    _items.value = list.map { dto ->
                        EmergencySummary(
                            id = dto.emergencyRequestId,
                            vehicleTitle = (dto.vehicleName ?: "") + (if (!dto.vehicleName.isNullOrBlank()) "" else ""),
                            issue = dto.issueDescription ?: "",
                            status = dto.status,
                            time = dto.requestTime ?: "",
                            garageName = dto.address ?: "",
                            technicianName = dto.assignedTechnicianName,
                            technicianPhone = null
                        )
                    }
                }
            } catch (_: Exception) {}
        }
    }
}
