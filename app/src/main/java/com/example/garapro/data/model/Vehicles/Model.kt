package com.example.garapro.data.model.Vehicles

import com.google.gson.annotations.SerializedName

data class Model(
    @SerializedName(value = "id", alternate = ["modelID", "ModelID"])
    val id: String = "",
    @SerializedName(value = "name", alternate = ["modelName", "ModelName"])
    val name: String = ""
)