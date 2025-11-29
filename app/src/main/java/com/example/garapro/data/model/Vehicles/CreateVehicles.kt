package com.example.garapro.data.model.Vehicles

import com.google.gson.annotations.SerializedName

data class CreateVehicles(
    @SerializedName("BrandID") val brandID: String,
    @SerializedName("UserID") val userID: String,
    @SerializedName("ModelID") val modelID: String,
    @SerializedName("ColorID") val colorID: String,
    @SerializedName("LicensePlate") val licensePlate: String,
    @SerializedName("VIN") val vin: String,
    @SerializedName("Year") val year: Int,
    @SerializedName("Odometer") val odometer: Long? = null
)

