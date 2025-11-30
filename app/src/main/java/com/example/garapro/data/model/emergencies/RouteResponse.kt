package com.example.garapro.data.model.emergencies

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class RouteResponse(
    val geometry: JsonElement,
    @SerializedName(value = "distanceMeters", alternate = ["distanceKm"]) val distanceMeters: Double?,
    @SerializedName(value = "durationSeconds", alternate = ["durationMinutes"]) val durationSeconds: Double?
)
