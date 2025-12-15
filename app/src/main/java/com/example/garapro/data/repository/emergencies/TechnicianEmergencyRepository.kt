package com.example.garapro.data.repository.emergencies

import android.util.Log
import com.example.garapro.data.model.techEmergencies.EmergencyDetailDto
import com.example.garapro.data.model.techEmergencies.TechnicianEmergencyResponse
import com.example.garapro.data.model.techEmergencies.TechnicianLocationBody
import com.example.garapro.data.model.techEmergencies.UpdateEmergencyStatusRequest
import com.example.garapro.data.remote.RetrofitInstance
import retrofit2.Response


class TechnicianEmergencyRepository {

    private val api = RetrofitInstance.techEmergencyService

    /**
     * Lấy emergency hiện tại + list của tech
     */
    suspend fun getMyEmergencies(): TechnicianEmergencyResponse? {
        return try {
            val response = api.getMyEmergencies()
            if (response.isSuccessful) {
                Log.d("TechEmergencyRepo", "getMyEmergencies G: ${response.body()}")

                response.body()

            } else {
                Log.e("TechEmergencyRepo", "getMyEmergencies failed: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e("TechEmergencyRepo", "getMyEmergencies error: ${e.message}", e)
            null
        }
    }

    suspend fun updateEmergencyStatus(
        id: String?,
        status: Int,
        reason: String?
    ): Boolean {

        return try {
            val body = UpdateEmergencyStatusRequest(status, reason)
            val result = api.updateStatus(id, body)
            result.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    suspend fun getEmergencyDetail(id: String): EmergencyDetailDto? {
        return try {
            val res = api.getEmergencyDetail(id)
            if (res.isSuccessful) res.body() else null
        } catch (e: Exception) {
            null
        }
    }

//    suspend fun updateLocation(latitude: Double, longitude: Double): Boolean {
//        return try {
//            val body = TechnicianLocationBody(latitude = latitude, longitude = longitude)
//            val response = api.updateLocation(body)
//            if (response.isSuccessful) {
//                true
//            } else {
//                Log.e("TechEmergencyRepo", "updateLocation failed: ${response.code()}")
//                false
//            }
//        } catch (e: Exception) {
//            Log.e("TechEmergencyRepo", "updateLocation error: ${e.message}", e)
//            false
//        }
//    }
}