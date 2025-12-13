package com.example.garapro.data.model.emergencies

import com.google.gson.annotations.SerializedName


class DirectionResponse {
    @SerializedName("routes")
    val routes: MutableList<Route?>? = null

    @SerializedName("status")
    private val status: String? = null

    class Route {
        @SerializedName("overview_polyline")
        val overviewPolyline: OverviewPolyline? = null

        @SerializedName("legs")
        val legs: MutableList<Leg?>? = null

        @SerializedName("distance")
        private val distance: Distance? = null

        @SerializedName("duration")
        private val duration: Duration? = null
    }

    class OverviewPolyline {
        @SerializedName("points")
        val points: String? = null
    }

    class Leg {
        @SerializedName("distance")
        val distance: Distance? = null

        @SerializedName("duration")
        private val duration: Duration? = null

        @SerializedName("steps")
         val steps: MutableList<Step?>? = null
    }

    data class LatLngJson(
        @SerializedName("lat") val lat: Double,
        @SerializedName("lng") val lng: Double
    )

    data class Polyline(
        @SerializedName("points") val points: String? = null
    )

    data class Step(
        @SerializedName("html_instructions")
        val instructions: String? = null,

        @SerializedName("distance")
        val distance: Distance? = null,

        @SerializedName("duration")
        val duration: Duration? = null,

        @SerializedName("start_location")
        val startLocation: LatLngJson? = null,

        @SerializedName("end_location")
        val endLocation: LatLngJson? = null,

        // JSON bạn gửi: "maneuver": "" (string)
        @SerializedName("maneuver")
        val maneuver: String? = null,

        @SerializedName("polyline")
        val polyline: Polyline? = null
    )

    class Distance {
        @SerializedName("text")
        val text: String? = null

        @SerializedName("value")
        private val value = 0
    }

    class Duration {
        @SerializedName("text")
        val text: String? = null

        @SerializedName("value")
        private val value = 0
    }
}