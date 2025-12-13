package com.example.garapro.ui.emergencies

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
    private var lastCreatedId: String? = null

    


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
    fun fetchRouteNowFor(emergencyId: String) {
        if (emergencyId.isBlank()) return
        viewModelScope.launch { fetchRouteOnce(emergencyId) }
    }
    fun startRoutePollingFor(emergencyId: String, intervalMs: Long = 10000L) {
        if (emergencyId.isBlank()) return
        routePollingJob?.cancel()
        routePollingJob = viewModelScope.launch {
            while (true) {
                fetchRouteOnce(emergencyId)
                delay(intervalMs)
            }
        }
    }

    private suspend fun fetchRouteOnce(emergencyId: String) {
        val res = repository.fetchRoute(emergencyId)
        if (res.isSuccess) {
            val route = res.getOrNull()
            val geo = route?.geometry
            try {
                val fc = geo?.let { toFeatureCollection(it) }
                _routeGeoJson.value = fc
                android.util.Log.d("Route", "route fetched: geometryType=" + (geo?.let { if (it.isJsonObject) "Object" else if (it.isJsonArray) "Array" else if (it.isJsonPrimitive) "Primitive" else "Unknown" } ?: "null") +", fcNull=" + (fc == null))
            } catch (e: Exception) {
                android.util.Log.w("Route", "failed to convert geometry: " + e.message)
                _routeGeoJson.value = null
            }
            val ds = route?.durationSeconds
            val minutes = if (ds != null) {
                if (ds > 300) kotlin.math.round(ds / 60.0).toInt() else kotlin.math.round(ds).toInt()
            } else null
            _etaMinutes.value = minutes
            _distanceMeters.value = route?.distanceMeters
        } else {
            android.util.Log.w("Route", "fetch failed: " + (res.exceptionOrNull()?.message ?: "unknown"))
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
                    val prim = geometry.asJsonPrimitive
                    if (prim.isString) {
                        val decoded = decodePolyline(prim.asString)
                        val coords = JsonArray().apply {
                            decoded.forEach { pair ->
                                add(JsonArray().apply {
                                    add(pair[0]) // lng
                                    add(pair[1]) // lat
                                })
                            }
                        }
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
                    } else {
                        JsonObject().apply {
                            addProperty("type", "FeatureCollection")
                            add("features", JsonArray())
                        }.toString()
                    }
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

    private fun decodePolyline(encoded: String): MutableList<List<Double>> {
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        val path: MutableList<List<Double>> = mutableListOf()
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20 && index < len)
            val dlat = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20 && index < len)
            val dlng = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
            lng += dlng

            val latD = lat / 1E5
            val lngD = lng / 1E5
            path.add(listOf(lngD, latD))
        }
        return path
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

            val branchGarage = _nearbyGarages.value?.firstOrNull { it.id == branchId }
            val currentAssigned = _assignedGarage.value ?: _selectedGarage.value
            val chosenGarage = currentAssigned ?: branchGarage
            chosenGarage?.let { _assignedGarage.value = it }

            val state = _emergencyState.value
            val isSameId = lastCreatedId == idCandidate
            val waitingSameGarage = state is EmergencyState.WaitingForGarage && chosenGarage != null && state.garage.id == chosenGarage.id

            if (!(isSameId && waitingSameGarage)) {
                val g = _assignedGarage.value
                if (g != null) {
                    _emergencyState.value = EmergencyState.WaitingForGarage(g)
                }
            }
            lastCreatedId = idCandidate
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

    fun rehydrateEmergency(emergency: Emergency) {
        currentEmergency = emergency
        when (emergency.status) {
            EmergencyStatus.PENDING -> {
                _emergencyState.value = EmergencyState.Success(emergency)
            }
            EmergencyStatus.ACCEPTED -> {
                _emergencyState.value = EmergencyState.Confirmed(emergency)
            }
            EmergencyStatus.IN_PROGRESS -> {
                _emergencyState.value = EmergencyState.Confirmed(emergency)
            }
            EmergencyStatus.COMPLETED -> {
                _emergencyState.value = EmergencyState.Success(emergency)
            }
            EmergencyStatus.CANCELLED -> {
                _emergencyState.value = EmergencyState.Error("Emergency canceled")
            }
        }
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
