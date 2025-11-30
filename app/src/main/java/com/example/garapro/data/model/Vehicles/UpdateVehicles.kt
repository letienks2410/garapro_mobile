package com.example.garapro.data.model.Vehicles

import com.google.gson.annotations.SerializedName

data class UpdateVehicles(
    // Các trường bắt buộc phải có, sử dụng SerializedName để đảm bảo tên JSON khớp với Backend
    @SerializedName("BrandID") val brandID: String,
    @SerializedName("ModelID") val modelID: String,
    @SerializedName("ColorID") val colorID: String,

    // Các trường dữ liệu chính
    @SerializedName("LicensePlate") val licensePlate: String,
    @SerializedName("VIN") val vin: String,
    @SerializedName("Year") val year: Int,

    // Các trường tùy chọn (Nullable)
    @SerializedName("Odometer") val odometer: Long? = null,
    @SerializedName("NextServiceDate") val nextServiceDate: String? = null,
    @SerializedName("WarrantyStatus") val warrantyStatus: String? = null
)