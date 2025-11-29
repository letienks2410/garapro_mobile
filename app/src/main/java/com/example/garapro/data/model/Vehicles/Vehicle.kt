package com.example.garapro.data.model.Vehicles
import com.google.gson.annotations.SerializedName

data class Vehicle(
    // Dữ liệu bắt buộc (ID)
    @SerializedName("vehicleID") val vehicleID: String,

    // Dữ liệu hiển thị (CÓ THỂ NULL)
    @SerializedName("brandName") val brandName: String?,
    @SerializedName("modelName") val modelName: String?,
    @SerializedName("colorName") val colorName: String?,
    @SerializedName("vin") val vin: String?,
    @SerializedName("year") val year: Int?,
    @SerializedName("licensePlate") val licensePlate: String?,
    @SerializedName("odometer") val odometer: Long?,

    // Các ID dùng cho logic (Required)
    @SerializedName("brandID") val brandID: String,
    @SerializedName("modelID") val modelID: String?,
    @SerializedName("colorID") val colorID: String

)
