package com.example.garapro.data.model.Vehicles

import com.google.gson.annotations.SerializedName

data class ModelColor(
    @SerializedName(value = "id", alternate = ["colorID", "ColorID"])
    val id: String = "",
    @SerializedName(value = "name", alternate = ["colorName", "ColorName"])
    val name: String = ""
)