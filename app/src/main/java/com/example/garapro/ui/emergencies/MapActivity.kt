package com.example.garapro.ui.emergencies
import com.example.garapro.ui.emergencies.EmergencyViewModel
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.VectorDrawable
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.garapro.R
import com.example.garapro.data.model.emergencies.Garage
import com.example.garapro.data.model.Vehicles.Vehicle
import com.example.garapro.data.remote.RetrofitInstance
import com.example.garapro.hubs.EmergencySignalRService

import com.google.android.gms.location.*
import com.google.android.gms.common.api.ResolvableApiException
import android.content.Intent
import android.app.Activity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.example.garapro.ui.repairRequest.VehicleAdapter
import com.example.garapro.data.model.repairRequest.Vehicle as RRVehicle
import android.widget.TextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.OnMapReadyCallback
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.sources.GeoJsonSource

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private var mapView: MapView? = null
    private var maplibreMap: MapLibreMap? = null
    private val markerPositions: MutableList<LatLng> = ArrayList()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationPermissionGranted = false

    // UI Components
    private lateinit var topAppBar: View
    private lateinit var btnBack: ImageButton
    private lateinit var tvTitle: TextView
    private lateinit var bottomSheetContainer: FrameLayout
    private lateinit var fabEmergency: FloatingActionButton
    private lateinit var fabCurrentLocation: FloatingActionButton
    private lateinit var loadingIndicator: ProgressBar

    private lateinit var emergencyBottomSheet: EmergencyBottomSheet
    // Bottom Sheet
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>

    // ViewModel
    private lateinit var viewModel: EmergencyViewModel

    // Adapter
    private lateinit var garageAdapter: GarageAdapter
    private var styleLoaded = false
    private var lastTappedLatLng: LatLng? = null
    private var selectedVehicleId: String? = null
    private var pendingIssueDescription: String? = null
    private var pendingLatLng: LatLng? = null
    private var emergencyHub: EmergencySignalRService? = null
    private val rejectedGarageIds = mutableSetOf<String>()
    private var trackingActive: Boolean = false
    private var technicianLatLng: LatLng? = null
    private var technicianName: String? = null
    private var technicianPhone: String? = null
    private var technicianArrived: Boolean = false
    private var destinationLatLng: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize MapLibre
        val mapLibre = MapLibre.getInstance(
            this,
            getString(R.string.goong_map_key),
            WellKnownTileServer.Mapbox
        )

        setContentView(R.layout.activity_map)

        // Initialize ViewModel
        viewModel = EmergencyViewModel()
        emergencyBottomSheet = EmergencyBottomSheet(this, viewModel)
        // Initialize UI Components
        initViews()
//        setupBottomSheet()
        setupClickListeners()
        setupObservers()

        // Initialize map view
        mapView = findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        checkLocationPermission()

        val prefs = getSharedPreferences(com.example.garapro.utils.Constants.USER_PREFERENCES, Context.MODE_PRIVATE)
        val userId = prefs.getString("user_id", null)
        val hubUrl = com.example.garapro.utils.Constants.BASE_URL_SIGNALR + "/api/emergencyrequesthub"
        emergencyHub = EmergencySignalRService(hubUrl).apply {
            setupListeners()
            start {
                userId?.let { joinCustomerGroup(it) }
            }
        }
    }

    private fun initViews() {
        topAppBar = findViewById(R.id.topAppBar)
        btnBack = findViewById(R.id.btnBack)
        tvTitle = findViewById(R.id.tvTitle)
        bottomSheetContainer = findViewById(R.id.bottomSheetContainer)
        fabEmergency = findViewById(R.id.fabEmergency)
        fabCurrentLocation = findViewById(R.id.fabCurrentLocation)
        loadingIndicator = findViewById(R.id.loadingIndicator)

        // Hide top app bar initially
        topAppBar.visibility = View.GONE
    }

    private fun setupBottomSheet() {
        // Inflate bottom sheet content
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_emergency_choose_garage, null)
        bottomSheetContainer.addView(bottomSheetView)

        // Get BottomSheetBehavior từ layout
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetContainer)

        // Cấu hình behavior
        bottomSheetBehavior.apply {
            state = BottomSheetBehavior.STATE_HIDDEN
            isFitToContents = false
            halfExpandedRatio = 0.5f
            expandedOffset = 100
            isHideable = true

            addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    when (newState) {
                        BottomSheetBehavior.STATE_EXPANDED -> {
                            // Bottom sheet mở hoàn toàn
                        }
                        BottomSheetBehavior.STATE_COLLAPSED -> {
                            // Bottom sheet đóng
                        }
                        BottomSheetBehavior.STATE_HIDDEN -> {
                            // Ẩn hoàn toàn
                            hideEmergencyUI()
                        }
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    // Xử lý khi đang kéo
                }
            })
        }

        // Setup RecyclerView
        val rvGarages = bottomSheetView.findViewById<RecyclerView>(R.id.rvGarages)
        garageAdapter = GarageAdapter { garage ->
            viewModel.selectGarage(garage)
        }
        rvGarages.layoutManager = LinearLayoutManager(this)
        rvGarages.adapter = garageAdapter

        // Setup confirm button
        val btnConfirm = bottomSheetView.findViewById<Button>(R.id.btnConfirm)
        btnConfirm.setOnClickListener {
            val emergency = viewModel.getCurrentEmergency()
            emergency?.let {
                topAppBar.visibility = View.GONE
                viewModel.confirmEmergency(it.id)
            }
        }
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            val garage = viewModel.assignedGarage.value ?: emergencyBottomSheet.lastSelectedGarage()
            if (trackingActive && garage != null) {
                topAppBar.visibility = View.GONE
                emergencyBottomSheet.showTracking(garage, null)
                return@setOnClickListener
            }
            if (garage != null) {
                val tech = technicianLatLng
                if (tech != null) {
                    val destLat = viewModel.getCurrentEmergency()?.latitude ?: pendingLatLng?.latitude
                    val destLng = viewModel.getCurrentEmergency()?.longitude ?: pendingLatLng?.longitude
                    if (destLat != null && destLng != null) {
                        val d = haversineMeters(tech.latitude, tech.longitude, destLat, destLng)
                        if (d <= ARRIVAL_THRESHOLD_METERS) {
                            emergencyBottomSheet.setOnCloseClickListener { finish() }
                            emergencyBottomSheet.showArrived(garage, technicianName, technicianPhone)
                        } else {
                            emergencyBottomSheet.showAccepted(garage, null)
                        }
                    } else {
                        emergencyBottomSheet.showAccepted(garage, null)
                    }
                } else {
                    emergencyBottomSheet.showAccepted(garage, null)
                }
            } else {
                hideEmergencyUI()
            }
        }

        fabEmergency.setOnClickListener {
            requestEmergency()
        }

        fabCurrentLocation.setOnClickListener {
            getCurrentLocation()
        }
    }

    private fun setupObservers() {
        viewModel.emergencyState.observe(this) { state ->
            when (state) {
                is EmergencyState.Loading -> {
                    showLoading(true)
                }
                is EmergencyState.Success -> {
                    showLoading(false)
                    showEmergencyUI()
                    viewModel.getCurrentEmergency()?.id?.takeIf { it.isNotBlank() }?.let { saveLastEmergencyId(it) }
                }

                is EmergencyState.WaitingForGarage -> {
                    Log.d("EmergencyState", "🟢 WaitingForGarage triggered for ${state.garage.name}")
                    showLoading(false)
                    topAppBar.visibility = View.GONE
                    mapView?.post {
                        emergencyBottomSheet?.showWaitingForGarage(state.garage)
                    }
                    viewModel.getCurrentEmergency()?.id?.takeIf { it.isNotBlank() }?.let { saveLastEmergencyId(it) }
                }

                is EmergencyState.Confirmed -> {
                    showLoading(false)
                    val garage = viewModel.assignedGarage.value ?: emergencyBottomSheet.lastSelectedGarage()
                    if (garage != null) {
                        emergencyBottomSheet.showAcceptedWaitingForTechnician(garage)
                    }
                    Toast.makeText(this, "Garage accepted! Waiting for technician assignment", Toast.LENGTH_SHORT).show()
                    viewModel.getCurrentEmergency()?.id?.takeIf { it.isNotBlank() }?.let { saveLastEmergencyId(it) }
                }

                is EmergencyState.Error -> {
                    showLoading(false)
                    val msg = state.message
                    val lower = msg.lowercase()
                    val isActiveEmergency = lower.contains("existing emergency") || lower.contains("active emergency") || lower.contains("đang được xử lý") || lower.contains("đang xử lí") || lower.contains("đã có đơn") || lower.contains("too many requests") || lower.contains("429")
                    val isRejected = lower.contains("rejected") || lower.contains("declined") || lower.contains("từ chối")
                    if (isActiveEmergency) {
                    val friendly = "You already have an active emergency request. Please follow the current request or cancel it before creating a new one."
                    com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                        .setTitle("Request already exists")
                        .setMessage(friendly)
                        .setNegativeButton("Close", null)
                        .setPositiveButton("Follow") { _, _ ->
                            val garage = viewModel.assignedGarage.value ?: emergencyBottomSheet.lastSelectedGarage()
                            val minutes: Int? = null
                            if (garage != null) emergencyBottomSheet.showAccepted(garage, minutes) else showEmergencyUI()
                        }
                        .show()
                    } else if (isRejected) {
                        val garage = emergencyBottomSheet.lastSelectedGarage()
                        if (garage != null) {
                            emergencyBottomSheet.showRejected(garage, msg)
                        } else {
                        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                            .setTitle("Garage rejected")
                            .setMessage("The garage cannot accept your request. Please choose another garage.")
                            .setNegativeButton("Close", null)
                            .setPositiveButton("Choose another garage") { _, _ -> showEmergencyUI() }
                            .show()
                        }
                    } else {
                        val friendly = if (lower.contains("timeout") || lower.contains("timed out") || lower.contains("sockettimeout")) {
                            "Network timeout. Please check your internet connection and try again."
                        } else msg
                        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                            .setTitle("Action Failed")
                            .setMessage(friendly)
                            .setNegativeButton("Close", null)
                            .setPositiveButton("Retry") { _, _ ->
                                val latLng = pendingLatLng
                                if (latLng != null) proceedFetchNearbyAndShow() else getCurrentLocationForEmergency()
                            }
                            .show()
                    }
                    emergencyBottomSheet.dismiss()
                }
                else -> {}
            }
        }

        viewModel.nearbyGarages.observe(this) { garages ->
            val filtered = garages.filter { it.id !in rejectedGarageIds }
            if (emergencyBottomSheet.isShowing()) {
                emergencyBottomSheet.updateGarages(filtered)
            }
        }

        viewModel.selectedGarage.observe(this) { garage ->
            if (emergencyBottomSheet.isShowing()) {
                emergencyBottomSheet.updateSelectedGarage(garage)
            }
            garage?.let { emergencyHub?.joinBranchGroup(it.id) }
        }

        viewModel.routeGeoJson.observe(this) { fc ->
            val style = maplibreMap?.style
            val src = style?.getSourceAs<GeoJsonSource>("route-source")
            if (fc != null) src?.setGeoJson(fc)
            try {
                if (fc != null && style != null) {
                    val obj = com.google.gson.JsonParser.parseString(fc).asJsonObject
                    val features = obj.getAsJsonArray("features")
                    if (features != null && features.size() > 0) {
                        val first = features.get(0).asJsonObject
                        val geom = first.getAsJsonObject("geometry")
                        val type = geom.get("type")?.asString
                        if (type != null && type.equals("LineString", true)) {
                            val coords = geom.getAsJsonArray("coordinates")
                            if (coords != null && coords.size() > 0) {
                                val p = coords.get(0).asJsonArray
                                val lng = p.get(0).asDouble
                                val lat = p.get(1).asDouble
                                val pointFc = JsonObject().apply {
                                    addProperty("type", "FeatureCollection")
                                    add("features", JsonArray().apply {
                                        add(JsonObject().apply {
                                            addProperty("type", "Feature")
                                            add("geometry", JsonObject().apply {
                                                addProperty("type", "Point")
                                                add("coordinates", JsonArray().apply {
                                                    add(lng)
                                                    add(lat)
                                                })
                                            })
                                        })
                                    })
                                }
                                style.getSourceAs<GeoJsonSource>("route-start-source")?.setGeoJson(pointFc.toString())
                                try {
                                    val last = coords.get(coords.size()-1).asJsonArray
                                    val lastLng = last.get(0).asDouble
                                    val lastLat = last.get(1).asDouble
                                    destinationLatLng = LatLng(lastLat, lastLng)
                                } catch (_: Exception) {}
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        viewModel.etaMinutes.observe(this) { m ->
            if (m != null) emergencyBottomSheet.updateTrackingEta(m)
        }

        viewModel.distanceMeters.observe(this) { d ->
            // arrival UI is triggered only by realtime TechnicianLocationUpdated; no route-based fallback
        }

        lifecycleScope.launchWhenStarted {
            emergencyHub?.events?.collect { (event, payload) ->
                val lower = event.lowercase()
                if (lower.contains("created")) {
                    try {
                        val obj = com.google.gson.JsonParser.parseString(payload).asJsonObject
                        val eid = arrayOf("EmergencyRequestId", "emergencyRequestId", "EmergencyId", "EmergenciesId", "RequestId", "Id").firstNotNullOfOrNull { k ->
                            if (obj.has(k)) try { obj.get(k).asString } catch (_: Exception) { null } else null
                        }
                        val branchId = arrayOf("BranchId", "branchId", "BranchID", "GarageId").firstNotNullOfOrNull { k ->
                            if (obj.has(k)) try { obj.get(k).asString } catch (_: Exception) { null } else null
                        }
                        android.util.Log.d("EmergencyID", "Raw created payload=" + payload)
                        android.util.Log.d("EmergencyID", "SignalR created payload id=" + (eid ?: "") + " branch=" + (branchId ?: ""))
                        if (!eid.isNullOrBlank()) viewModel.markCreated(eid, branchId)
                    } catch (_: Exception) {}
                }
                if (lower.contains("approved")) {
                    try {
                        val obj = com.google.gson.JsonParser.parseString(payload).asJsonObject
                        val eid = arrayOf("EmergencyRequestId", "emergencyRequestId", "EmergencyId", "EmergenciesId", "RequestId", "Id").firstNotNullOfOrNull { k ->
                            if (obj.has(k)) try { obj.get(k).asString } catch (_: Exception) { null } else null
                        }
                        val branchId = arrayOf("BranchId", "branchId", "BranchID", "GarageId").firstNotNullOfOrNull { k ->
                            if (obj.has(k)) try { obj.get(k).asString } catch (_: Exception) { null } else null
                        }
                        android.util.Log.d("EmergencyID", "Raw approved payload=" + payload)
                        android.util.Log.d("EmergencyID", "SignalR approved payload id=" + (eid ?: "") + " branch=" + (branchId ?: ""))
                        viewModel.markApproved(eid ?: viewModel.getCurrentEmergency()?.id ?: "", branchId)
                    } catch (_: Exception) {
                        val curId = viewModel.getCurrentEmergency()?.id ?: ""
                        viewModel.markApproved(curId, null)
                    }
                } else if (lower.contains("rejected")) {
                    try {
                        val obj = com.google.gson.JsonParser.parseString(payload).asJsonObject
                        val reason = if (obj.has("RejectReason")) obj.get("RejectReason").asString else obj.get("Message")?.asString
                        val branchId = if (obj.has("BranchId")) obj.get("BranchId").asString else null
                        branchId?.let { rejectedGarageIds.add(it) }
                        val garage = viewModel.assignedGarage.value ?: emergencyBottomSheet.lastSelectedGarage()
                        emergencyBottomSheet.setOnChooseAnotherListener {
                            viewModel.clearSelectedGarage()
                            val latLng = pendingLatLng ?: lastTappedLatLng
                            latLng?.let { viewModel.refreshNearbyGarages(it.latitude, it.longitude) }
                            showEmergencyUI()
                        }
                        if (garage != null) emergencyBottomSheet.showRejected(garage, reason)
                    Toast.makeText(this@MapActivity, "Garage rejected", Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {
                        emergencyBottomSheet.lastSelectedGarage()?.id?.let { rejectedGarageIds.add(it) }
                        val garage = emergencyBottomSheet.lastSelectedGarage()
                        emergencyBottomSheet.setOnChooseAnotherListener {
                            viewModel.clearSelectedGarage()
                            val latLng = pendingLatLng ?: lastTappedLatLng
                            latLng?.let { viewModel.refreshNearbyGarages(it.latitude, it.longitude) }
                            showEmergencyUI()
                        }
                        if (garage != null) emergencyBottomSheet.showRejected(garage, null)
                    }
                } else if (lower.contains("joinedcustomergroup")) {
                    Toast.makeText(this@MapActivity, "Joined: $payload", Toast.LENGTH_SHORT).show()
                } else if (lower.contains("joinedbranchgroup")) {
                    Toast.makeText(this@MapActivity, "Joined: $payload", Toast.LENGTH_SHORT).show()
                } else if (lower.contains("expired")) {
                    val garage = emergencyBottomSheet.lastSelectedGarage()
                    emergencyBottomSheet.setOnChooseAnotherListener {
                        viewModel.clearSelectedGarage()
                        val latLng = pendingLatLng ?: lastTappedLatLng
                        latLng?.let { viewModel.refreshNearbyGarages(it.latitude, it.longitude) }
                        showEmergencyUI()
                    }
                    garage?.let { emergencyBottomSheet.showExpired(it) }
                    Toast.makeText(this@MapActivity, "Request response time has expired", Toast.LENGTH_LONG).show()
                } else if (lower.contains("canceled")) {
                    var autoCanceledAt: String? = null
                    var branchId: String? = null
                    try {
                        val obj = com.google.gson.JsonParser.parseString(payload).asJsonObject
                        if (obj.has("AutoCanceledAt")) autoCanceledAt = obj.get("AutoCanceledAt").asString
                        if (obj.has("BranchId")) branchId = obj.get("BranchId").asString
                    } catch (_: Exception) {}
                    if (!autoCanceledAt.isNullOrBlank()) {
                        branchId?.let { rejectedGarageIds.add(it) }
                        val garage = viewModel.assignedGarage.value ?: emergencyBottomSheet.lastSelectedGarage()
                        emergencyBottomSheet.setOnChooseAnotherListener {
                            viewModel.clearSelectedGarage()
                            val latLng = pendingLatLng ?: lastTappedLatLng
                            latLng?.let { viewModel.refreshNearbyGarages(it.latitude, it.longitude) }
                            showEmergencyUI()
                        }
                        garage?.let { emergencyBottomSheet.showExpired(it) }
                        Toast.makeText(this@MapActivity, "Request response time has expired", Toast.LENGTH_LONG).show()
                    } else {
                        viewModel.resetState()
                        Toast.makeText(this@MapActivity, "Request canceled", Toast.LENGTH_LONG).show()
                    }
                }
                if (lower.contains("technicianlocationupdated")) {
                    //check vi tri
                    try {
                        val obj = com.google.gson.JsonParser.parseString(payload).asJsonObject
                        android.util.Log.d("TechRT", "Payload:" + payload)
                        val lat = when {
                            obj.has("latitude") -> obj.get("latitude").asDouble
                            obj.has("lat") -> obj.get("lat").asDouble
                            else -> Double.NaN
                        }
                        val lng = when {
                            obj.has("longitude") -> obj.get("longitude").asDouble
                            obj.has("lng") -> obj.get("lng").asDouble
                            else -> Double.NaN
                        }
                        android.util.Log.d("TechRT", "lat=" + lat + ", lng=" + lng)
                        if (!lat.isNaN() && !lng.isNaN()) {
                            val point = LatLng(lat, lng)
                            technicianLatLng = point
                            val style = maplibreMap?.style
                            val src = style?.getSourceAs<GeoJsonSource>("technician-source")
                            val feature = JsonObject().apply {
                                addProperty("type", "Feature")
                                add("geometry", JsonObject().apply {
                                    addProperty("type", "Point")
                                    add("coordinates", JsonArray().apply {
                                        add(lng)
                                        add(lat)
                                    })
                                })
                            }
                            val fc = JsonObject().apply {
                                addProperty("type", "FeatureCollection")
                                add("features", JsonArray().apply { add(feature) })
                            }
                            src?.setGeoJson(fc.toString())
                            android.util.Log.d("TechRT", "GeoJSON set, marker should move")
                            emergencyBottomSheet.updateTrackingSkeleton(false)
                            if (trackingActive) moveCameraToLocation(point)

                            checkArrivalAndUpdateUI(point)

                            val eta = try { if (obj.has("etaMinutes")) obj.get("etaMinutes").asInt else null } catch (_: Exception) { null }
                            val distanceKm = try { if (obj.has("distanceKm")) obj.get("distanceKm").asDouble else null } catch (_: Exception) { null }
                            if ((eta != null && eta <= 0) || (distanceKm != null && distanceKm <= 0.05)) {
                                val garage = viewModel.assignedGarage.value ?: emergencyBottomSheet.lastSelectedGarage()
                                if (garage != null) {
                                    technicianArrived = true
                                    trackingActive = false
                                    emergencyBottomSheet.setOnCloseClickListener { finish() }
                                    emergencyBottomSheet.showArrived(garage, technicianName, technicianPhone)
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
                //check inprogress
                if (lower.contains("emergencyrequestinprogress") || lower.contains("inprogress")) {
                    try {
                        val obj = com.google.gson.JsonParser.parseString(payload).asJsonObject
                        val eid = arrayOf("EmergencyRequestId", "emergencyRequestId", "EmergencyId", "EmergenciesId", "RequestId", "Id").firstNotNullOfOrNull { k ->
                            if (obj.has(k)) try { obj.get(k).asString } catch (_: Exception) { null } else null
                        }
                        if (!eid.isNullOrBlank()) {
                            saveLastEmergencyId(eid)
                        }
                        val name = when {
                            obj.has("TechnicianName") -> obj.get("TechnicianName").asString
                            obj.has("Name") -> obj.get("Name").asString
                            else -> null
                        }
                        val phone = when {
                            obj.has("TechnicianPhone") -> obj.get("TechnicianPhone").asString
                            obj.has("Phone") -> obj.get("Phone").asString
                            else -> null
                        }
                        technicianName = name
                        technicianPhone = phone
                        if (!technicianArrived) emergencyBottomSheet.updateTrackingTechnician(name, phone)
                        val garage = viewModel.assignedGarage.value ?: emergencyBottomSheet.lastSelectedGarage()
                        val minutes: Int? = null
                        if (garage != null) {
                            emergencyBottomSheet.setOnTrackClickListener {
                                enableTrackingUI()
                                emergencyBottomSheet.showTracking(garage, minutes)
                                emergencyBottomSheet.setOnViewMapClickListener {
                                    topAppBar.visibility = View.VISIBLE
                                    tvTitle.text = "Tracking technician"
                                    enableTrackingUI()
                                    val id2 = viewModel.getCurrentEmergency()?.id
                                    if (!id2.isNullOrBlank()) viewModel.fetchRouteNow()
                                }
                                val id = viewModel.getCurrentEmergency()?.id
                                if (!id.isNullOrBlank()) {
                                    viewModel.fetchRouteNow()
                                }
                            }
                            emergencyBottomSheet.showAccepted(garage, minutes)
                        }
                    } catch (_: Exception) {}
                }
                if (lower.contains("technicianassigned")) {
                    try {
                        val obj = com.google.gson.JsonParser.parseString(payload).asJsonObject
                        val name = when {
                            obj.has("TechnicianName") -> obj.get("TechnicianName").asString
                            obj.has("Name") -> obj.get("Name").asString
                            else -> null
                        }
                        val phone = when {
                            obj.has("TechnicianPhone") -> obj.get("TechnicianPhone").asString
                            obj.has("Phone") -> obj.get("Phone").asString
                            else -> null
                        }
                        technicianName = name
                        technicianPhone = phone
                        if (!technicianArrived) emergencyBottomSheet.updateTrackingTechnician(name, phone)
                        val garage = viewModel.assignedGarage.value ?: emergencyBottomSheet.lastSelectedGarage()
                        val minutes: Int? = null
                        if (garage != null) {
                            emergencyBottomSheet.setOnTrackClickListener {
                                enableTrackingUI()
                                emergencyBottomSheet.showTracking(garage, minutes)
                                emergencyBottomSheet.setOnViewMapClickListener {
                                    topAppBar.visibility = View.VISIBLE
                                    tvTitle.text = "Tracking technician"
                                    enableTrackingUI()
                                    val id2 = viewModel.getCurrentEmergency()?.id
                                    if (!id2.isNullOrBlank()) viewModel.fetchRouteNow()
                                }
                                val id = viewModel.getCurrentEmergency()?.id
                                if (!id.isNullOrBlank()) {
                                    viewModel.fetchRouteNow()
                                }
                            }
                            emergencyBottomSheet.showAccepted(garage, minutes)
                        }
                        viewModel.getCurrentEmergency()?.id?.takeIf { it.isNotBlank() }?.let { saveLastEmergencyId(it) }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    

    private fun requestEmergency() {
        if (!locationPermissionGranted) {
            checkLocationPermission()
            return
        }

        getCurrentLocationForEmergency()
    }

    private fun getCurrentLocationForEmergency() {
        if (!checkLocationPermission()) return
        ensureLocationSettingsEnabled {
            fetchAccurateLocation { latLng ->
                if (latLng != null) {
                    if (!isVietnamLocation(latLng)) {
                        val fallback = lastTappedLatLng
                        if (fallback != null && isVietnamLocation(fallback)) {
                            showLoading(true)
                            viewModel.requestEmergency(fallback.latitude, fallback.longitude)
                            addMarkerAtPosition(fallback, "Assistance location")
                            moveCameraToLocation(fallback)
                        } else {
                            MaterialAlertDialogBuilder(this)
                                .setTitle("Unable to determine location in Vietnam")
                                .setMessage("Please enable GPS or tap the map to choose an assistance location in Vietnam.")
                                .setPositiveButton("Close", null)
                                .show()
                        }
                        return@fetchAccurateLocation
                    }
                    pendingLatLng = latLng
                    showLoading(true)
                    addMarkerAtPosition(latLng, "Assistance location")
                    moveCameraToLocation(latLng)
                    lifecycleScope.launchWhenStarted {
                        try {
                            val resp = withContext(Dispatchers.IO) { RetrofitInstance.vehicleService.getVehicles() }
                            val vehicles = if (resp.isSuccessful) (resp.body() ?: emptyList()) else emptyList()
                            if (vehicles.isEmpty()) {
                                Toast.makeText(this@MapActivity, "Please add a vehicle first", Toast.LENGTH_LONG).show()
                                showLoading(false)
                                return@launchWhenStarted
                            }
                            if (vehicles.size == 1) {
                                selectedVehicleId = vehicles.first().vehicleID
                                showIssueDescriptionSheet { desc ->
                                    pendingIssueDescription = desc
                                    proceedFetchNearbyAndShow()
                                }
                            } else {
                                showVehicleSelectionSheet(vehicles) { chosenId ->
                                    selectedVehicleId = chosenId
                                    showIssueDescriptionSheet { desc ->
                                        pendingIssueDescription = desc
                                        proceedFetchNearbyAndShow()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@MapActivity, "Failed to load vehicles: ${e.message}", Toast.LENGTH_LONG).show()
                            showLoading(false)
                        }
                    }
                } else {
                    Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun proceedFetchNearbyAndShow() {
        val latLng = pendingLatLng ?: return
        viewModel.requestEmergency(latLng.latitude, latLng.longitude)
    }

    private fun showEmergencyUI() {
        topAppBar.visibility = View.VISIBLE
        fabEmergency.visibility = View.GONE

        // Hiển thị bottom sheet với danh sách gara
        val allGarages = viewModel.nearbyGarages.value ?: emptyList()
        var filtered = allGarages.filter { it.id !in rejectedGarageIds }
        if (filtered.isEmpty()) {
            val latLng = pendingLatLng ?: lastTappedLatLng
            latLng?.let { viewModel.refreshNearbyGarages(it.latitude, it.longitude) }
            if (allGarages.isNotEmpty()) filtered = allGarages
        }
        emergencyBottomSheet.show(
            garages = filtered,
            selectedGarage = viewModel.selectedGarage.value,
            onConfirm = {
                val emergency = viewModel.getCurrentEmergency()
                val garage = viewModel.selectedGarage.value
                if (emergency == null || garage == null) return@show
                val vehicleId = selectedVehicleId
                val issue = pendingIssueDescription
                if (vehicleId.isNullOrBlank()) {
                    Toast.makeText(this, "Please select a vehicle first", Toast.LENGTH_LONG).show()
                    return@show
                }
                if (issue.isNullOrBlank()) {
                    showIssueDescriptionSheet { desc ->
                        pendingIssueDescription = desc
                        topAppBar.visibility = View.GONE
                        viewModel.createEmergencyRequest(vehicleId, garage.id, desc, emergency.latitude, emergency.longitude)
                    }
                } else {
                    topAppBar.visibility = View.GONE
                    viewModel.createEmergencyRequest(vehicleId, garage.id, issue, emergency.latitude, emergency.longitude)
                }
            }
            
        )
    }

    private fun saveLastEmergencyId(id: String) {
        val prefs = getSharedPreferences(com.example.garapro.utils.Constants.USER_PREFERENCES, Context.MODE_PRIVATE)
        prefs.edit().putString("last_emergency_id", id).apply()
    }

    private fun recoverExistingEmergency() {
        val prefs = getSharedPreferences(com.example.garapro.utils.Constants.USER_PREFERENCES, Context.MODE_PRIVATE)
        val id = prefs.getString("last_emergency_id", null) ?: return
        lifecycleScope.launchWhenStarted {
            try {
                val resp = withContext(Dispatchers.IO) { com.example.garapro.data.remote.RetrofitInstance.emergencyService.getEmergencyById(id) }
                if (resp.isSuccessful) {
                    val emergency = resp.body()
                    if (emergency != null) {
                        viewModel.rehydrateEmergency(emergency)
                        when (emergency.status) {
                            com.example.garapro.data.model.emergencies.EmergencyStatus.ACCEPTED -> {
                                topAppBar.visibility = View.GONE
                                val g = viewModel.assignedGarage.value ?: emergencyBottomSheet.lastSelectedGarage()
                                if (g != null) emergencyBottomSheet.showAcceptedWaitingForTechnician(g)
                                else {
                                    emergencyBottomSheet.showAcceptedWaitingForTechnician(
                                        com.example.garapro.data.model.emergencies.Garage(id = emergency.assignedGarageId ?: "", name = "Garage", latitude = 0.0, longitude = 0.0, address = "", phone = "", isAvailable = true, price = 0.0, rating = 0f, distance = 0.0)
                                    )
                                }
                            }
                            com.example.garapro.data.model.emergencies.EmergencyStatus.IN_PROGRESS -> {
                                val g = viewModel.assignedGarage.value ?: emergencyBottomSheet.lastSelectedGarage()
                                val minutes: Int? = null
                                if (g != null) emergencyBottomSheet.showAccepted(g, minutes)
                                else emergencyBottomSheet.showAccepted(
                                    com.example.garapro.data.model.emergencies.Garage(id = emergency.assignedGarageId ?: "", name = "Garage", latitude = 0.0, longitude = 0.0, address = "", phone = "", isAvailable = true, price = 0.0, rating = 0f, distance = 0.0),
                                    minutes
                                )
                                enableTrackingUI()
                                viewModel.fetchRouteNow()
                            }
                            else -> {}
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun showVehicleSelectionSheet(vehicles: List<Vehicle>, onSelected: (String) -> Unit) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.fragment_vehicle_selection, null)
        dialog.setContentView(view)
        val rv = view.findViewById<RecyclerView>(R.id.rvVehicles)
        val tvSelected = view.findViewById<TextView>(R.id.tvSelectedVehicle)
        val btnNext = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnNext)
        var chosenId: String? = null
        val rrVehicles = vehicles.map { v ->
            RRVehicle(
                vehicleID = v.vehicleID,
                brandID = v.brandID,
                userID = "",
                modelID = v.modelID ?: "",
                colorID = v.colorID,
                licensePlate = v.licensePlate ?: "",
                vin = v.vin ?: "",
                year = v.year ?: 0,
                odometer = (v.odometer ?: 0L).toInt(),
                lastServiceDate = null,
                nextServiceDate = null,
                warrantyStatus = "",
                brandName = v.brandName ?: "",
                modelName = v.modelName ?: "",
                colorName = v.colorName ?: ""
            )
        }
        val adapter = VehicleAdapter(rrVehicles) { v ->
            chosenId = v.vehicleID
            tvSelected.text = "${v.brandName} ${v.modelName} - ${v.licensePlate}"
            btnNext.isEnabled = true
            btnNext.setBackgroundColor(android.graphics.Color.BLACK)
        }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
        adapter.updateData(rrVehicles)
        btnNext.setOnClickListener {
            val id = chosenId
            if (!id.isNullOrBlank()) {
                dialog.dismiss()
                onSelected(id)
            }
        }
        dialog.show()
    }

    private fun showIssueDescriptionSheet(onDone: (String) -> Unit) {
        val input = android.widget.EditText(this)
        input.setText("Flat tire")
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Issue description")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Continue") { _, _ ->
                val desc = input.text?.toString()?.trim().takeIf { !it.isNullOrBlank() } ?: "Roadside assistance"
                onDone(desc)
            }
            .show()
    }

    private fun hideEmergencyUI() {
        topAppBar.visibility = View.GONE
        fabEmergency.visibility = View.VISIBLE
        emergencyBottomSheet.dismiss()
        viewModel.resetState()
        rejectedGarageIds.clear()
    }



    private fun showLoading(show: Boolean) {
        loadingIndicator.visibility = if (show) View.VISIBLE else View.GONE
    }

    // Các hàm cũ giữ nguyên từ đây trở xuống...
    override fun onMapReady(@NonNull map: MapLibreMap) {
        this.maplibreMap = map
        loadMapStyle()
    }

    private fun loadMapStyle() {
        styleLoaded = false
        val goongStyleUrl = "https://tiles.goong.io/assets/goong_map_web.json?api_key=" + getString(R.string.goong_map_key)
        maplibreMap?.setStyle(Style.Builder().fromUri(goongStyleUrl), object : Style.OnStyleLoaded {
            override fun onStyleLoaded(@NonNull style: Style) {
                styleLoaded = true
                setupMap()
                addSampleMarkers(style)
                addRouteLayer(style)
                addTechnicianLayer(style)
                setupMapListeners()
                Toast.makeText(this@MapActivity, "Map loaded", Toast.LENGTH_SHORT).show()
            }
        })

        Handler(Looper.getMainLooper()).postDelayed({
            if (!styleLoaded) {
                val fallback = "https://demotiles.maplibre.org/style.json"
                maplibreMap?.setStyle(Style.Builder().fromUri(fallback), object : Style.OnStyleLoaded {
                    override fun onStyleLoaded(@NonNull style: Style) {
                        styleLoaded = true
                        setupMap()
                        addSampleMarkers(style)
                        addRouteLayer(style)
                        addTechnicianLayer(style)
                        setupMapListeners()
                        Toast.makeText(this@MapActivity, "Fallback style loaded", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }, 4000)
    }

    private fun setupMap() {
        val hanoi = LatLng(21.0295797, 105.8524247)
        val position = CameraPosition.Builder()
            .target(hanoi)
            .zoom(12.0)
            .tilt(0.0)
            .build()
        maplibreMap?.setCameraPosition(position)
    }

    private fun addSampleMarkers(style: Style) {
        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_location)
        if (drawable is VectorDrawable) {
            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            style.addImage("custom-marker", bitmap)
            style.addImage("tech-marker", bitmap)
        } else {
            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_location)
            style.addImage("custom-marker", bitmap)
            style.addImage("tech-marker", bitmap)
        }

        val featureCollection = JsonObject().apply {
            addProperty("type", "FeatureCollection")
            add("features", JsonArray())
        }

        style.addSource(GeoJsonSource("marker-source", featureCollection.toString()))
        style.addLayer(
            SymbolLayer("marker-layer", "marker-source").withProperties(
                PropertyFactory.iconImage("custom-marker"),
                PropertyFactory.iconSize(1.0f),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true)
            )
        )
    }

    private fun addTechnicianLayer(style: Style) {
        val empty = JsonObject().apply {
            addProperty("type", "FeatureCollection")
            add("features", JsonArray())
        }
        style.addSource(GeoJsonSource("technician-source", empty.toString()))
        style.addLayer(
            CircleLayer("technician-layer", "technician-source").withProperties(
                PropertyFactory.circleRadius(6f),
                PropertyFactory.circleColor("#2962FF"),
                PropertyFactory.circleStrokeColor("#FFFFFF"),
                PropertyFactory.circleStrokeWidth(2f),
                PropertyFactory.circleOpacity(0.95f)
            )
        )
    }

    private fun addRouteLayer(style: Style) {
        val empty = JsonObject().apply {
            addProperty("type", "FeatureCollection")
            add("features", JsonArray())
        }
        style.addSource(GeoJsonSource("route-source", empty.toString()))
        style.addLayer(
            LineLayer("route-layer", "route-source").withProperties(
                PropertyFactory.lineColor("#2563EB"),
                PropertyFactory.lineWidth(5.0f)
            )
        )
        style.addSource(GeoJsonSource("route-start-source", empty.toString()))
        style.addLayer(
            SymbolLayer("route-start-layer", "route-start-source").withProperties(
                PropertyFactory.iconImage("custom-marker"),
                PropertyFactory.iconSize(1.0f),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true)
            )
        )
    }

    private fun enableTrackingUI() {
        trackingActive = true
        val tech = technicianLatLng
        if (tech != null) moveCameraToLocation(tech)
        emergencyBottomSheet.updateTrackingSkeleton(tech == null)
        Toast.makeText(this, "Đang theo dõi kỹ thuật viên", Toast.LENGTH_SHORT).show()
    }

    private fun openExternalMap(garage: com.example.garapro.data.model.emergencies.Garage?) {
        val tech = technicianLatLng
        if (tech != null) {
            try {
                val uri = android.net.Uri.parse("google.navigation:q=${tech.latitude},${tech.longitude}")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
                return
            } catch (_: Exception) {}
            try {
                val uri = android.net.Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${tech.latitude},${tech.longitude}")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
                return
            } catch (_: Exception) {}
        }
        val addr = garage?.address
        if (!addr.isNullOrBlank()) {
            try {
                val uri = android.net.Uri.parse("geo:0,0?q=" + java.net.URLEncoder.encode(addr, "UTF-8"))
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
                return
            } catch (_: Exception) {}
            try {
                val uri = android.net.Uri.parse("https://www.google.com/maps/search/?api=1&query=" + java.net.URLEncoder.encode(addr, "UTF-8"))
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
                return
            } catch (_: Exception) {}
        }
        val lat = garage?.latitude ?: pendingLatLng?.latitude
        val lng = garage?.longitude ?: pendingLatLng?.longitude
        if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
            try {
                val uri = android.net.Uri.parse("google.navigation:q=${lat},${lng}")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
                return
            } catch (_: Exception) {}
            try {
                val uri = android.net.Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${lat},${lng}")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
                return
            } catch (_: Exception) {}
        }
        try {
            val uri = android.net.Uri.parse("https://www.google.com/maps/dir/?api=1&destination=21.0295797,105.8524247")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "Không thể mở bản đồ", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupMapListeners() {
        maplibreMap?.addOnMapClickListener { point ->
            Toast.makeText(
                this@MapActivity,
                "Clicked: ${point.latitude}, ${point.longitude}",
                Toast.LENGTH_SHORT
            ).show()
            addMarkerAtPosition(point)
            lastTappedLatLng = point
            true
        }

        maplibreMap?.addOnCameraMoveListener {
            maplibreMap?.cameraPosition?.let { position ->
                Log.d("MapMove", "Camera: ${position.target}, Zoom: ${position.zoom}")
            }
        }
    }

    private fun checkArrivalAndUpdateUI(tech: LatLng) {
        val dest = destinationLatLng
            ?: (viewModel.getCurrentEmergency()?.let { LatLng(it.latitude, it.longitude) })
            ?: pendingLatLng
            ?: return
        val destLat = dest.latitude
        val destLng = dest.longitude
        if (destLat == 0.0 && destLng == 0.0) return
        val d = haversineMeters(tech.latitude, tech.longitude, destLat, destLng)
        if (d <= ARRIVAL_THRESHOLD_METERS) {
            viewModel.stopRoutePolling()
            val garage = viewModel.assignedGarage.value ?: emergencyBottomSheet.lastSelectedGarage()
            if (garage != null) {
                technicianArrived = true
                trackingActive = false
                emergencyBottomSheet.setOnCloseClickListener { finish() }
                emergencyBottomSheet.showArrived(garage, technicianName, technicianPhone)
                
                Toast.makeText(this, "Technician arrived", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }

    private fun addMarkerAtPosition(position: LatLng, title: String = "Location") {
        if (title == "Current location" || title == "Assistance location") {
            markerPositions.clear()
        }
        markerPositions.add(position)
        val featuresArray = JsonArray()
        markerPositions.forEachIndexed { i, latLng ->
            val markerTitle = if (i == markerPositions.size - 1 && title == "Current location") {
                title
            } else {
                "Location ${i + 1}"
            }
            val feature = JsonObject().apply {
                addProperty("type", "Feature")
                add("geometry", JsonObject().apply {
                    addProperty("type", "Point")
                    add("coordinates", JsonArray().apply {
                        add(latLng.longitude)
                        add(latLng.latitude)
                    })
                })
                add("properties", JsonObject().apply {
                    addProperty("title", markerTitle)
                })
            }
            featuresArray.add(feature)
        }
        maplibreMap?.getStyle()?.getSourceAs<GeoJsonSource>("marker-source")
            ?.setGeoJson(featureCollection(featuresArray))
        Toast.makeText(this, "Marker added: $title", Toast.LENGTH_SHORT).show()
    }

    private fun checkLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fine) {
            locationPermissionGranted = true
            return true
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return false
        }
    }

    private fun moveCameraToLocation(latLng: LatLng) {
        val position = CameraPosition.Builder()
            .target(latLng)
            .zoom(17.0)
            .tilt(0.0)
            .build()
        maplibreMap?.cameraPosition = position
    }

    private fun isVietnamLocation(latLng: LatLng): Boolean {
        val lat = latLng.latitude
        val lng = latLng.longitude
        return lat in 8.0..24.0 && lng in 102.0..110.0
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true
                getCurrentLocation()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        if (!locationPermissionGranted) {
            checkLocationPermission()
            return
        }
        ensureLocationSettingsEnabled {
            try {
                fetchAccurateLocation { latLng ->
                    latLng?.let {
                        moveCameraToLocation(it)
                        addMarkerAtPosition(it, "Vị trí hiện tại")
                    }
                }
            } catch (e: SecurityException) {
                Log.e("Location", "Security exception: ${e.message}")
                Toast.makeText(this, "Không thể truy cập vị trí: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchAccurateLocation(callback: (LatLng?) -> Unit) {
        val cts = com.google.android.gms.tasks.CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { loc ->
                if (loc != null && loc.accuracy <= 100f) {
                    Toast.makeText(this, "GPS: ${loc.latitude}, ${loc.longitude} (±${loc.accuracy}m)", Toast.LENGTH_SHORT).show()
                    callback(LatLng(loc.latitude, loc.longitude))
                } else {
                    val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
                        .setMinUpdateIntervalMillis(1000L)
                        .build()
                    val cb = object : LocationCallback() {
                        override fun onLocationResult(result: LocationResult) {
                            val l = result.lastLocation
                            if (l != null && l.accuracy <= 100f) {
                                fusedLocationClient.removeLocationUpdates(this)
                                Toast.makeText(this@MapActivity, "GPS: ${l.latitude}, ${l.longitude} (±${l.accuracy}m)", Toast.LENGTH_SHORT).show()
                                callback(LatLng(l.latitude, l.longitude))
                            }
                        }
                    }
                    fusedLocationClient.requestLocationUpdates(req, cb, Looper.getMainLooper())
                    Handler(Looper.getMainLooper()).postDelayed({
                        fusedLocationClient.removeLocationUpdates(cb)
                        callback(null)
                    }, 15000L)
                }
            }
            .addOnFailureListener { _ -> callback(null) }
    }

    private fun featureCollection(featuresArray: JsonArray): String {
        return JsonObject().apply {
            addProperty("type", "FeatureCollection")
            add("features", featuresArray)
        }.toString()
    }

    private fun ensureLocationSettingsEnabled(onReady: () -> Unit) {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L).build()
        val builder = com.google.android.gms.location.LocationSettingsRequest.Builder()
            .addLocationRequest(request)
            .setAlwaysShow(true)
        val client = LocationServices.getSettingsClient(this)
        client.checkLocationSettings(builder.build())
            .addOnSuccessListener { onReady() }
            .addOnFailureListener { e ->
                if (e is ResolvableApiException) {
                    try {
                        e.startResolutionForResult(this, REQUEST_CHECK_SETTINGS)
                    } catch (_: Exception) {}
                } else {
                    Toast.makeText(this, "Vui lòng bật GPS để lấy vị trí hiện tại", Toast.LENGTH_LONG).show()
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                getCurrentLocation()
            } else {
                Toast.makeText(this, "GPS chưa được bật", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val REQUEST_CHECK_SETTINGS = 2002
        private const val ARRIVAL_THRESHOLD_METERS = 5.0
    }

    // Lifecycle methods
    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
        val prefs = getSharedPreferences(com.example.garapro.utils.Constants.USER_PREFERENCES, Context.MODE_PRIVATE)
        val id = prefs.getString("last_emergency_id", null) ?: return
        lifecycleScope.launchWhenStarted {
            try {
                val resp = withContext(Dispatchers.IO) { com.example.garapro.data.remote.RetrofitInstance.emergencyService.getEmergencyById(id) }
                if (resp.isSuccessful) {
                    val emergency = resp.body()
                    if (emergency != null) {
                        viewModel.rehydrateEmergency(emergency)
                        when (emergency.status) {
                            com.example.garapro.data.model.emergencies.EmergencyStatus.ACCEPTED -> {
                                val g = viewModel.assignedGarage.value ?: emergencyBottomSheet.lastSelectedGarage()
                                if (g != null) emergencyBottomSheet.showAcceptedWaitingForTechnician(g)
                                else {
                                    emergencyBottomSheet.showAcceptedWaitingForTechnician(
                                        com.example.garapro.data.model.emergencies.Garage(id = emergency.assignedGarageId ?: "", name = "Garage", latitude = 0.0, longitude = 0.0, address = "", phone = "", isAvailable = true, price = 0.0, rating = 0f, distance = 0.0)
                                    )
                                }
                            }
                            com.example.garapro.data.model.emergencies.EmergencyStatus.IN_PROGRESS -> {
                                val g = viewModel.assignedGarage.value ?: emergencyBottomSheet.lastSelectedGarage()
                                val minutes: Int? = null
                                if (g != null) emergencyBottomSheet.showAccepted(g, minutes)
                                else emergencyBottomSheet.showAccepted(
                                    com.example.garapro.data.model.emergencies.Garage(id = emergency.assignedGarageId ?: "", name = "Garage", latitude = 0.0, longitude = 0.0, address = "", phone = "", isAvailable = true, price = 0.0, rating = 0f, distance = 0.0),
                                    minutes
                                )
                                enableTrackingUI()
                                viewModel.fetchRouteNow()
                            }
                            else -> {}
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onSaveInstanceState(@NonNull outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }
}

// Extension function
fun Double.formatPrice(): String = "%,.0f đ".format(this)
