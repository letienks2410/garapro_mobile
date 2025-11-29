package com.example.garapro.ui.emergencies

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.garapro.R
import com.example.garapro.data.model.emergencies.DirectionResponse
import com.example.garapro.data.remote.GoongClient
import com.google.android.gms.location.*
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.OnMapReadyCallback
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import java.util.Locale

class MapDirectionDemoActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private var map: MapLibreMap? = null
    private lateinit var tvInstruction: TextView
    private lateinit var btnToggleNav: Button

    private lateinit var fusedLocation: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    // Vị trí hiện tại
    private var currentLocation: LatLng? = null

    // ĐH FPT Đà Nẵng
    private val fptDanang = LatLng(15.75, 108.33)

    // Route data
    private var routePoints: List<LatLng> = emptyList()
    private var steps: List<DirectionResponse.Step> = emptyList()
    private var currentStepIndex = 0
    private var currentRoutePointIndex = 0

    // Navigation state
    private var isNavigating = false
    private var hasRoute = false

    private var lastLocationForRouting: LatLng? = null
    private var lastRerouteTime = 0L

    private val MIN_DISTANCE_TO_REROUTE = 40f      // m
    private val MIN_TIME_TO_REROUTE_MS = 10_000L   // 10s
    private val SNAP_TO_ROUTE_THRESHOLD = 30f      // m

    // Camera
    private var lastGpsPos: LatLng? = null

    // TTS
    private var tts: TextToSpeech? = null

    companion object {
        private const val PERMISSION_LOCATION = 1999
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ⭐ MUST: gọi trước layout
        MapLibre.getInstance(
            this,
            getString(R.string.goong_map_key),
            WellKnownTileServer.Mapbox
        )

        // ⭐ Inflate layout chứa MapView sau khi init MapLibre
        setContentView(R.layout.activity_map_direction_demo)

        // ⭐ findViewById
        mapView = findViewById(R.id.mapView)
        tvInstruction = findViewById(R.id.tvInstruction)
        btnToggleNav = findViewById(R.id.btnToggleNav)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        fusedLocation = LocationServices.getFusedLocationProviderClient(this)

        setupTTS()
        setupButton()
        checkPermission()
    }

    private fun setupButton() {
        btnToggleNav.setOnClickListener {
            isNavigating = !isNavigating

            if (isNavigating) {
                btnToggleNav.text = "Stop NAV"
                tvInstruction.text = "Đang tính đường đến FPT Đà Nẵng..."

                // Reset route tracking
                hasRoute = false
                lastLocationForRouting = null
                lastRerouteTime = 0L

                // Nếu có GPS rồi → gọi route ngay
                currentLocation?.let { loc ->
                    hasRoute = true
                    lastLocationForRouting = loc
                    lastRerouteTime = System.currentTimeMillis()
                    getDirectionRoute(loc)
                }

            } else {
                btnToggleNav.text = "Start NAV"
                tvInstruction.text = "Đã dừng điều hướng"
                tts?.speak("Đã dừng điều hướng", TextToSpeech.QUEUE_FLUSH, null, "NAV_STOP")
            }
        }
    }

    private fun setupTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("vi", "VN")
            }
        }
    }
    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_LOCATION
            )
        } else {
            startLocationUpdates()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_LOCATION &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
        } else {
            Toast.makeText(this, "Cần quyền vị trí để dẫn đường", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMapReady(map: MapLibreMap) {
        this.map = map

        val styleUrl =
            "https://tiles.goong.io/assets/goong_map_web.json?api_key=${getString(R.string.goong_map_key)}"

        map.setStyle(styleUrl) { style ->
            initRouteLayer(style)
            initCarMarker(style)

            // Tạm zoom tới FPT Đà Nẵng
            val camera = CameraPosition.Builder()
                .target(fptDanang)
                .zoom(13.0)
                .build()
            map.cameraPosition = camera
        }
    }

    private fun initRouteLayer(style: Style) {
        style.addSource(GeoJsonSource("route-source", FeatureCollection.fromFeatures(arrayOf())))
        style.addLayer(
            LineLayer("route-layer", "route-source").withProperties(
                PropertyFactory.lineWidth(7f),
                PropertyFactory.lineColor("#ff0000"),
                PropertyFactory.lineJoin("round"),
                PropertyFactory.lineCap("round")
            )
        )
    }

    private fun initCarMarker(style: Style) {
        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_car)
        if (drawable == null) {
            Log.e("Map", "ic_car drawable is null")
            return
        }

        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth.takeIf { it > 0 } ?: 64,
            drawable.intrinsicHeight.takeIf { it > 0 } ?: 64,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        style.addImage("car-icon", bitmap)

        // source xe
        style.addSource(
            GeoJsonSource(
                "car-source",
                Point.fromLngLat(fptDanang.longitude, fptDanang.latitude)
            )
        )

        style.addLayer(
            SymbolLayer("car-layer", "car-source").withProperties(
                PropertyFactory.iconImage("car-icon"),
                PropertyFactory.iconSize(0.8f),
                PropertyFactory.iconIgnorePlacement(true),
                PropertyFactory.iconAllowOverlap(true)
            )
        )
    }

    private fun getDirectionRoute(origin: LatLng) {
        val originStr = "${origin.latitude},${origin.longitude}"
        val destStr = "${fptDanang.latitude},${fptDanang.longitude}"

        GoongClient.getApiService().getDirection(
            originStr, destStr, "car", getString(R.string.goong_api_key)
        )?.enqueue(object : retrofit2.Callback<DirectionResponse?> {
            override fun onResponse(
                call: retrofit2.Call<DirectionResponse?>,
                response: retrofit2.Response<DirectionResponse?>
            ) {
                if (!response.isSuccessful || response.body() == null) {
                    Toast.makeText(this@MapDirectionDemoActivity, "Lỗi Direction", Toast.LENGTH_SHORT).show()
                    return
                }

                val direction = response.body()!!
                val route = direction.routes?.firstOrNull()
                val poly = route?.overviewPolyline?.points ?: return

                val leg = route.legs?.firstOrNull()
                steps = leg?.steps?.filterNotNull() ?: emptyList()
                currentStepIndex = 0
                currentRoutePointIndex = 0

                updateInstruction()
                drawPolyline(poly)
            }

            override fun onFailure(call: retrofit2.Call<DirectionResponse?>, t: Throwable) {
                Toast.makeText(this@MapDirectionDemoActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun speak(text: String) {
        if (isNavigating) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "NAV_TTS")
        }
    }

    private fun plainTextFromHtml(html: String?): String {
        if (html.isNullOrEmpty()) return ""
        return android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_LEGACY).toString()
    }

    private fun updateInstruction() {
        if (currentStepIndex >= steps.size) {
            tvInstruction.text = "Đã đến nơi 🎉"
            speak("Bạn đã đến nơi")
            return
        }

        val step = steps[currentStepIndex]
        val instrText = plainTextFromHtml(step.instructions)
        val distanceText = step.distance?.text ?: ""

        val display = "Trong $distanceText, $instrText"
        tvInstruction.text = display
        speak(display)
    }

    // decode polyline
    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            poly.add(LatLng(lat / 1E5, lng / 1E5))
        }

        return poly
    }

    private fun drawPolyline(encoded: String) {
        routePoints = decodePolyline(encoded)
        currentRoutePointIndex = 0

        val points = routePoints.map { Point.fromLngLat(it.longitude, it.latitude) }
        val line = LineString.fromLngLats(points)
        val feature = Feature.fromGeometry(line)

        map?.getStyle()?.getSourceAs<GeoJsonSource>("route-source")
            ?.setGeoJson(FeatureCollection.fromFeature(feature))

        val bounds = LatLngBounds.Builder()
        routePoints.forEach { bounds.include(it) }

        map?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 100))
    }

    private fun updateRemainingRoute() {
        if (routePoints.isEmpty()) return
        val remaining = routePoints.drop(currentRoutePointIndex)
        if (remaining.isEmpty()) return

        val points = remaining.map { Point.fromLngLat(it.longitude, it.latitude) }
        val line = LineString.fromLngLats(points)
        val feature = Feature.fromGeometry(line)

        map?.getStyle()?.getSourceAs<GeoJsonSource>("route-source")
            ?.setGeoJson(FeatureCollection.fromFeature(feature))
    }

    private fun distanceBetween(a: LatLng, b: LatLng): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            a.latitude, a.longitude,
            b.latitude, b.longitude,
            results
        )
        return results[0]
    }

    // Trả về Triple(điểm gần nhất trên route, khoảng cách, index)
    private fun closestPointOnRouteWithIndex(pos: LatLng): Triple<LatLng, Float, Int>? {
        if (routePoints.isEmpty()) return null

        var minDist = Float.MAX_VALUE
        var closestIndex = -1

        routePoints.forEachIndexed { index, p ->
            val d = distanceBetween(pos, p)
            if (d < minDist) {
                minDist = d
                closestIndex = index
            }
        }

        if (closestIndex == -1) return null
        return Triple(routePoints[closestIndex], minDist, closestIndex)
    }

    private fun updateCarMarker(pos: LatLng) {
        map?.getStyle()?.getSourceAs<GeoJsonSource>("car-source")
            ?.setGeoJson(Point.fromLngLat(pos.longitude, pos.latitude))
    }

    private fun updateCamera(pos: LatLng, bearing: Float?) {
        val finalBearing = bearing ?: lastGpsPos?.let { prev ->
            val results = FloatArray(2)
            android.location.Location.distanceBetween(
                prev.latitude, prev.longitude,
                pos.latitude, pos.longitude,
                results
            )
            results[1]
        } ?: 0f

        val camera = CameraUpdateFactory.newCameraPosition(
            CameraPosition.Builder()
                .target(pos)
                .zoom(17.0)
                .tilt(45.0)
                .bearing(finalBearing.toDouble())
                .build()
        )
        map?.animateCamera(camera)
    }

    private fun checkNextStep(pos: LatLng) {
        if (currentStepIndex >= steps.size) return

        // đơn giản: khi tới gần 1 điểm trên route tương ứng thì tăng step
        val snapInfo = closestPointOnRouteWithIndex(pos) ?: return
        val (_, distToRoute, indexOnRoute) = snapInfo

        if (distToRoute < 25f && indexOnRoute > currentRoutePointIndex + 5) {
            currentStepIndex++
            currentRoutePointIndex = indexOnRoute
            updateInstruction()
            updateRemainingRoute()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 2000
        ).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val rawPos = LatLng(loc.latitude, loc.longitude)
                currentLocation = rawPos
                val bearing = if (loc.hasBearing()) loc.bearing else null

                // Nếu đang NAV mà chưa có route -> gọi route
                if (isNavigating && !hasRoute && map != null) {
                    hasRoute = true
                    lastLocationForRouting = rawPos
                    lastRerouteTime = System.currentTimeMillis()
                    getDirectionRoute(rawPos)
                }

                var displayPos = rawPos

                if (hasRoute && routePoints.isNotEmpty()) {
                    val snapInfo = closestPointOnRouteWithIndex(rawPos)
                    if (snapInfo != null) {
                        val (snapPoint, snapDist, snapIndex) = snapInfo
                        if (snapDist <= SNAP_TO_ROUTE_THRESHOLD) {
                            displayPos = snapPoint

                            if (snapIndex > currentRoutePointIndex) {
                                currentRoutePointIndex = snapIndex
                                updateRemainingRoute()
                            }
                        } else if (isNavigating) {
                            val moved =
                                lastLocationForRouting?.let { distanceBetween(rawPos, it) }
                                    ?: Float.MAX_VALUE
                            val now = System.currentTimeMillis()
                            if (moved > MIN_DISTANCE_TO_REROUTE &&
                                now - lastRerouteTime > MIN_TIME_TO_REROUTE_MS
                            ) {
                                lastLocationForRouting = rawPos
                                lastRerouteTime = now
                                getDirectionRoute(rawPos)
                                Log.d("Nav", "Reroute vì lệch route, moved=$moved")
                            }
                        }
                    }
                }

                // update marker
                updateCarMarker(displayPos)

                // chỉ follow camera + step nếu đang NAV
                if (isNavigating) {
                    updateCamera(displayPos, bearing)
                    checkNextStep(displayPos)
                }

                lastGpsPos = rawPos
            }
        }

        fusedLocation.requestLocationUpdates(
            request, locationCallback, Looper.getMainLooper()
        )
    }

    // Lifecycle
    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()

        if (::locationCallback.isInitialized) {
            try {
                fusedLocation.removeLocationUpdates(locationCallback)
            } catch (_: Exception) {
            }
        }

        tts?.stop()
        tts?.shutdown()
    }
}
