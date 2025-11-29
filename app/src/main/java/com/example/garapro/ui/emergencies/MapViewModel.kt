import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garapro.data.model.emergencies.Emergency
import com.example.garapro.data.model.emergencies.CreateEmergencyRequest
import com.example.garapro.data.model.emergencies.EmergencyStatus
import com.example.garapro.data.model.emergencies.Garage
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonArray
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class EmergencyViewModel : ViewModel() {
    private val repository = EmergencyRepository.getInstance()

    private val _emergencyState = MutableLiveData<EmergencyState>(EmergencyState.Idle)
    val emergencyState: LiveData<EmergencyState> = _emergencyState

    private val _nearbyGarages = MutableLiveData<List<Garage>>(emptyList())
    val nearbyGarages: LiveData<List<Garage>> = _nearbyGarages

    private val _selectedGarage = MutableLiveData<Garage?>(null)
    val selectedGarage: LiveData<Garage?> = _selectedGarage

    private val _assignedGarage = MutableLiveData<Garage?>(null)
    val assignedGarage: LiveData<Garage?> = _assignedGarage
    private var currentEmergency: Emergency? = null

    private val _routeGeoJson = MutableLiveData<String?>(null)
    val routeGeoJson: LiveData<String?> = _routeGeoJson
    private val _etaMinutes = MutableLiveData<Int?>(null)
    val etaMinutes: LiveData<Int?> = _etaMinutes
    private val _distanceMeters = MutableLiveData<Double?>(null)
    val distanceMeters: LiveData<Double?> = _distanceMeters
    private var routePollingJob: Job? = null

    


    init {
        // Setup realtime listener
        repository.setOnEmergencyAssignedListener { emergencyId, garageId ->
            viewModelScope.launch {
                // Khi technician accept đơn, cập nhật state
                if (currentEmergency?.id == emergencyId) {
                    _emergencyState.value = EmergencyState.Confirmed(currentEmergency!!)
                    Log.d("EmergencyFlow", "Technician accepted the emergency!")
                }
            }
        }
        repository.addOnEmergencyUpdatedListener { emergency ->
            viewModelScope.launch {
                if (currentEmergency?.id == emergency.id && emergency.status == EmergencyStatus.ACCEPTED) {
                    _emergencyState.value = EmergencyState.Confirmed(emergency)
                }
            }
        }
    }


    fun createEmergencyRequest(vehicleId: String, branchId: String, issueDescription: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _emergencyState.value = EmergencyState.Loading
            val req = CreateEmergencyRequest(
                vehicleId = vehicleId,
                branchId = branchId,
                issueDescription = issueDescription,
                latitude = latitude,
                longitude = longitude
            )
            val emergencyResult = repository.createEmergencyRequest(req)
            if (emergencyResult.isSuccess) {
                currentEmergency = emergencyResult.getOrNull()
                val garage = _selectedGarage.value
                if (garage != null) {
                    _assignedGarage.value = garage
                    _emergencyState.value = EmergencyState.WaitingForGarage(garage)
                } else {
                    _emergencyState.value = EmergencyState.Success(currentEmergency!!)
                }
            } else {
                _emergencyState.value = EmergencyState.Error("Tạo yêu cầu cứu hộ thất bại")
            }
        }
    }

    fun requestEmergency(userLat: Double, userLng: Double) {
        viewModelScope.launch {
            _emergencyState.value = EmergencyState.Loading
            val garagesResult = repository.findNearbyGarages(userLat, userLng)
            if (garagesResult.isSuccess) {
                _nearbyGarages.value = garagesResult.getOrDefault(emptyList())
                val temp = Emergency(id = System.currentTimeMillis().toString(), latitude = userLat, longitude = userLng)
                currentEmergency = temp
                _emergencyState.value = EmergencyState.Success(temp)
            } else {
                _emergencyState.value = EmergencyState.Error("Không tìm thấy gara gần nhất")
            }
        }
    }

    fun refreshNearbyGarages(userLat: Double, userLng: Double) {
        viewModelScope.launch {
            val garagesResult = repository.findNearbyGarages(userLat, userLng)
            if (garagesResult.isSuccess) {
                _nearbyGarages.value = garagesResult.getOrDefault(emptyList())
            }
        }
    }

    fun selectGarage(garage: Garage) {
        _selectedGarage.value = garage
    }

    fun preselectGarage(garage: Garage) {
        _selectedGarage.value = garage
        _assignedGarage.value = garage
    }

    fun clearSelectedGarage() {
        _selectedGarage.value = null
    }

    fun confirmEmergency(emergencyId: String) {
        viewModelScope.launch {
            val garage = _selectedGarage.value
            if (garage != null) {
                _emergencyState.value = EmergencyState.WaitingForGarage(garage)
                _assignedGarage.value = garage

                Log.d("EmergencyFlow", "Waiting for technician to accept...")
            }
        }
    }

    fun cancelEmergencyRequest() {
        viewModelScope.launch {
            val id = currentEmergency?.id ?: return@launch
            val result = repository.cancelEmergency(id)
            if (result.isSuccess) {
                resetState()
            } else {
                _emergencyState.value = EmergencyState.Error("Hủy yêu cầu cứu hộ thất bại")
            }
        }
    }

    fun startRoutePolling(intervalMs: Long = 10000L) {
        val id = currentEmergency?.id?.takeIf { it.isNotBlank() } ?: return
        routePollingJob?.cancel()
        routePollingJob = viewModelScope.launch {
            while (true) {
                fetchRouteOnce(id)
                delay(intervalMs)
            }
        }
    }

    fun stopRoutePolling() {
        routePollingJob?.cancel()
        routePollingJob = null
    }

    fun fetchRouteNow() {
        val id = currentEmergency?.id?.takeIf { it.isNotBlank() } ?: return
        viewModelScope.launch { fetchRouteOnce(id) }
    }

    private suspend fun fetchRouteOnce(emergencyId: String) {
        val res = repository.fetchRoute(emergencyId)
        if (res.isSuccess) {
            val route = res.getOrNull()
            val geo = route?.geometry
            _routeGeoJson.value = geo?.let { toFeatureCollection(it) }
            val ds = route?.durationSeconds
            val minutes = if (ds != null) {
                if (ds > 300) kotlin.math.round(ds / 60.0).toInt() else kotlin.math.round(ds).toInt()
            } else null
            _etaMinutes.value = minutes
            _distanceMeters.value = route?.distanceMeters
        }
    }

    private fun toFeatureCollection(geometry: JsonElement): String {
        return try {
            when {
                geometry.isJsonObject -> {
                    val obj = geometry.asJsonObject
                    val type = obj.get("type")?.asString
                    if (type == "FeatureCollection") {
                        obj.toString()
                    } else {
                        val feature = JsonObject().apply {
                            addProperty("type", "Feature")
                            add("geometry", obj)
                        }
                        JsonObject().apply {
                            addProperty("type", "FeatureCollection")
                            add("features", JsonArray().apply { add(feature) })
                        }.toString()
                    }
                }
                geometry.isJsonArray -> {
                    val coords = geometry.asJsonArray
                    val geom = JsonObject().apply {
                        addProperty("type", "LineString")
                        add("coordinates", coords)
                    }
                    val feature = JsonObject().apply {
                        addProperty("type", "Feature")
                        add("geometry", geom)
                    }
                    JsonObject().apply {
                        addProperty("type", "FeatureCollection")
                        add("features", JsonArray().apply { add(feature) })
                    }.toString()
                }
                geometry.isJsonPrimitive -> {
                    // Unsupported encoded polyline; return empty FC
                    JsonObject().apply {
                        addProperty("type", "FeatureCollection")
                        add("features", JsonArray())
                    }.toString()
                }
                else -> JsonObject().apply {
                    addProperty("type", "FeatureCollection")
                    add("features", JsonArray())
                }.toString()
            }
        } catch (_: Exception) {
            JsonObject().apply {
                addProperty("type", "FeatureCollection")
                add("features", JsonArray())
            }.toString()
        }
    }

    

    

    

    fun markApproved(emergencyId: String, branchId: String?) {
        viewModelScope.launch {
            val garage = _selectedGarage.value ?: _nearbyGarages.value?.firstOrNull { it.id == branchId }
            garage?.let { _assignedGarage.value = it }
            val idCandidate = if (emergencyId.isNotBlank()) emergencyId else (currentEmergency?.id ?: "")
            val before = currentEmergency?.id ?: ""
            currentEmergency = currentEmergency?.copy(id = idCandidate) ?: Emergency(id = idCandidate)
            android.util.Log.d("EmergencyID", "markApproved before=" + before + " incoming=" + emergencyId + " after=" + currentEmergency?.id)
            _emergencyState.value = EmergencyState.Confirmed(currentEmergency!!)
        }
    }

    fun markCreated(emergencyId: String, branchId: String?) {
        viewModelScope.launch {
            val idCandidate = if (emergencyId.isNotBlank()) emergencyId else (currentEmergency?.id ?: "")
            val before = currentEmergency?.id ?: ""
            currentEmergency = currentEmergency?.copy(id = idCandidate) ?: Emergency(id = idCandidate)
            android.util.Log.d("EmergencyID", "markCreated before=" + before + " incoming=" + emergencyId + " after=" + currentEmergency?.id)
            val garage = _selectedGarage.value ?: _nearbyGarages.value?.firstOrNull { it.id == branchId }
            garage?.let { _assignedGarage.value = it }
            val g = _assignedGarage.value
            if (g != null) {
                _emergencyState.value = EmergencyState.WaitingForGarage(g)
            }
        }
    }


    fun getCurrentEmergency(): Emergency? {
        return currentEmergency
    }

    fun resetState() {
        _emergencyState.value = EmergencyState.Idle
        _nearbyGarages.value = emptyList()
        _selectedGarage.value = null
        _assignedGarage.value = null
        currentEmergency = null
        stopRoutePolling()
        _routeGeoJson.value = null
        _etaMinutes.value = null
    }
}

sealed class EmergencyState {
    object Idle : EmergencyState()
    object Loading : EmergencyState()
    data class WaitingForGarage(val garage: Garage) : EmergencyState()
    data class Success(val emergency: Emergency) : EmergencyState()
    data class Confirmed(val emergency: Emergency) : EmergencyState()
    data class Error(val message: String) : EmergencyState()
}
