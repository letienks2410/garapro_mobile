package com.example.garapro.data.repository

import com.example.garapro.data.model.Vehicles.Brand
import com.example.garapro.data.model.Vehicles.CreateVehicles
import com.example.garapro.data.model.Vehicles.Model
import com.example.garapro.data.model.Vehicles.ModelColor
import com.example.garapro.data.model.Vehicles.UpdateVehicles
import com.example.garapro.data.model.Vehicles.Vehicle
import com.example.garapro.data.remote.VehicleApiService

sealed class ApiResponse<T> {
    data class Success<T>(val data: T) : ApiResponse<T>()
    data class Error<T>(val message: String) : ApiResponse<T>()
    class Loading<T> : ApiResponse<T>()
}

class VehicleRepository(private val apiService: VehicleApiService) {

    suspend fun fetchVehicles(): ApiResponse<List<Vehicle>> {
        return try {
            val response = apiService.getVehicles()
            if (response.isSuccessful && response.body() != null) {
                ApiResponse.Success(response.body()!!)
            } else {
                ApiResponse.Error("Failed to load vehicles: ${response.code()}")
            }
        } catch (e: Exception) {
            ApiResponse.Error(e.message ?: "Network error while loading vehicles")
        }
    }

    suspend fun fetchVehicleDetail(vehicleId: String): ApiResponse<Vehicle> {
        return try {
            val response = apiService.getVehicleDetail(vehicleId)
            if (response.isSuccessful && response.body() != null) {
                ApiResponse.Success(response.body()!!)
            } else {
                ApiResponse.Error("Vehicle detail not found: ${response.code()}")
            }
        } catch (e: Exception) {
            ApiResponse.Error(e.message ?: "Network error while loading vehicle detail")
        }
    }

    suspend fun createVehicle(request: CreateVehicles): ApiResponse<Unit> {
        return try {
            val response = apiService.createVehicle(request)
            if (response.isSuccessful) {
                ApiResponse.Success(Unit)
            } else {
                val err = try { response.errorBody()?.string() } catch (_: Exception) { null }
                ApiResponse.Error(
                    if (!err.isNullOrBlank()) "Create vehicle failed ${response.code()}: $err" else "Create vehicle failed: ${response.code()}"
                )
            }
        } catch (e: Exception) {
            ApiResponse.Error(e.message ?: "Network error while creating vehicle")
        }
    }

    suspend fun updateVehicle(vehicleId: String, request: UpdateVehicles): ApiResponse<Unit> {
        return try {
            val response = apiService.updateVehicle(vehicleId, request)
            if (response.isSuccessful) {
                ApiResponse.Success(Unit)
            } else {
                val err = try { response.errorBody()?.string() } catch (_: Exception) { null }
                ApiResponse.Error(
                    if (!err.isNullOrBlank()) "Update vehicle failed ${response.code()}: $err" else "Update vehicle failed: ${response.code()}"
                )
            }
        } catch (e: Exception) {
            ApiResponse.Error(e.message ?: "Network error while updating vehicle")
        }
    }

    suspend fun deleteVehicle(vehicleId: String): ApiResponse<Unit> {
        return try {
            val response = apiService.deleteVehicle(vehicleId)
            if (response.isSuccessful) {
                ApiResponse.Success(Unit)
            } else {
                ApiResponse.Error("Delete vehicle failed: ${response.code()}")
            }
        } catch (e: Exception) {
            ApiResponse.Error(e.message ?: "Network error while deleting vehicle")
        }
    }

    suspend fun fetchBrands(): ApiResponse<List<Brand>> {
        return try {
            val response = apiService.getBrands()
            if (response.isSuccessful && response.body() != null) {
                ApiResponse.Success(response.body()!!)
            } else {
                ApiResponse.Error("Failed to load brands: ${response.code()}")
            }
        } catch (e: Exception) {
            ApiResponse.Error(e.message ?: "Network error while loading brands")
        }
    }

    suspend fun fetchModelsByBrand(brandId: String): ApiResponse<List<Model>> {
        return try {
            val response = apiService.getModelsByBrand(brandId)
            if (response.isSuccessful && response.body() != null) {
                ApiResponse.Success(response.body()!!)
            } else {
                ApiResponse.Error("Failed to load models: ${response.code()}")
            }
        } catch (e: Exception) {
            ApiResponse.Error(e.message ?: "Network error while loading models")
        }
    }

    suspend fun fetchColorsByModel(modelId: String): ApiResponse<List<ModelColor>> {
        return try {
            val response = apiService.getColorsByModel(modelId)
            if (response.isSuccessful && response.body() != null) {
                ApiResponse.Success(response.body()!!)
            } else {
                ApiResponse.Error("Failed to load colors: ${response.code()}")
            }
        } catch (e: Exception) {
            ApiResponse.Error(e.message ?: "Network error while loading colors")
        }
    }
}