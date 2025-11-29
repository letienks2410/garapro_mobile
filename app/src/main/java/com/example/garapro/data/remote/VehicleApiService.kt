package com.example.garapro.data.remote

import com.example.garapro.data.model.Vehicles.Brand
import com.example.garapro.data.model.Vehicles.CreateVehicles
import com.example.garapro.data.model.Vehicles.Model
import com.example.garapro.data.model.Vehicles.ModelColor
import com.example.garapro.data.model.Vehicles.UpdateVehicles
import com.example.garapro.data.model.Vehicles.Vehicle
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface VehicleApiService {
    @GET("Vehicles/user")
    suspend fun getVehicles(): Response<List<Vehicle>>

    // 2. GET: Lấy chi tiết một xe theo ID
    @GET("Vehicles/{id}")
    suspend fun getVehicleDetail(@Path("id") vehicleId: String): Response<Vehicle>

    // 3. ADD (CREATE): Tạo xe mới
    // Khớp với POST /api/Vehicles
    @POST("Vehicles")
    suspend fun createVehicle(@Body request: CreateVehicles): Response<Void>

    // 4. UPDATE: Cập nhật thông tin xe
    @PUT("Vehicles/{id}")
    suspend fun updateVehicle(
        @Path("id") vehicleId: String,
        @Body request: UpdateVehicles
    ): Response<Void>

    // 5. DELETE: Xóa một chiếc xe theo ID
    @DELETE("Vehicles/{id}")
    suspend fun deleteVehicle(@Path("id") vehicleId: String): Response<Void>
    @GET("VehicleBrands") // Giả định endpoint
    suspend fun getBrands(): Response<List<Brand>>

    // 7. Lấy danh sách Dòng xe (Model) theo Hãng xe (BrandID)
    @GET("VehicleModels/bybrand/{brandId}")
    suspend fun getModelsByBrand(@Path("brandId") brandId: String): Response<List<Model>>

    // 8. Lấy danh sách Màu sắc (Color)
    @GET("VehicleColors/bymodel/{modelId}") // Giả định endpoint
    suspend fun getColorsByModel(@Path("modelId") modelId: String): Response<List<ModelColor>>
}