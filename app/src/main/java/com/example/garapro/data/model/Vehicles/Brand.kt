package com.example.garapro.data.model.Vehicles

import com.google.gson.annotations.SerializedName

data class Brand(
    @SerializedName(value = "id", alternate = ["brandID", "BrandID"])
    val id: String = "",
    @SerializedName(value = "name", alternate = ["brandName", "BrandName"])
    val name: String = ""
)