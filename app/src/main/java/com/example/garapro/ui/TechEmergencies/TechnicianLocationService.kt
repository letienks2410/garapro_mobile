package com.example.garapro.ui.TechEmergencies

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.garapro.R
import com.example.garapro.data.model.techEmergencies.EmergencyStatus
import com.example.garapro.data.model.techEmergencies.TechnicianLocationBody
import com.example.garapro.data.remote.RetrofitInstance
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TechnicianLocationService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START_LOCATION"
        const val ACTION_STOP = "ACTION_STOP_LOCATION"

        private const val CHANNEL_ID = "tech_location_channel"
        private const val NOTI_ID = 2001
    }

    // ===== data =====
    private lateinit var fusedLocation: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    private var emergencyId: String? = null
    private var latitude = 0.0
    private var longitude = 0.0
    private var branchName = ""
    private var branchLatitude = 0.0
    private var branchLongitude = 0.0
    private var status = 0
    private var customerPhone = ""

    // throttle
    private var lastLocationSent: Location? = null
    private var lastSendTime = 0L
    private val MIN_TIME_MS = 10_000L
    private val MIN_DISTANCE_M = 20f

    override fun onCreate() {
        super.onCreate()
        fusedLocation = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                emergencyId = intent.getStringExtra("emergencyId")
                latitude = intent.getDoubleExtra("latitude", 0.0)
                longitude = intent.getDoubleExtra("longitude", 0.0)
                branchName = intent.getStringExtra("branchName") ?: ""
                branchLatitude = intent.getDoubleExtra("branchLatitude", 0.0)
                branchLongitude = intent.getDoubleExtra("branchLongitude", 0.0)
                status = intent.getIntExtra("status", 0)
                customerPhone = intent.getStringExtra("customerPhone") ?: ""

                startForeground(NOTI_ID, buildNotification())
                startLocationUpdates()
            }

            ACTION_STOP -> {
                stopLocationUpdates()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ================= NOTIFICATION =================

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MapDirectionDemoActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

            putExtra("emergencyId", emergencyId)
            putExtra("latitude", latitude)
            putExtra("longitude", longitude)
            putExtra("branchName", branchName)
            putExtra("branchLatitude", branchLatitude)
            putExtra("branchLongitude", branchLongitude)
            putExtra("status", status)
            putExtra("customerPhone", customerPhone)
        }

        val openPending = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, TechnicianLocationService::class.java).apply {
            action = ACTION_STOP
        }

        val stopPending = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = when (EmergencyStatus.fromInt(status)) {
            EmergencyStatus.InProgress ->
                "Đang di chuyển tới khách hàng"
            EmergencyStatus.Towing ->
                "Đang kéo xe về garage"
            else ->
                "Đang gửi vị trí kỹ thuật viên"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_location)
            .setContentTitle("GARAPRO – Navigation")
            .setContentText(contentText)
            .setOngoing(true)
            .setContentIntent(openPending)
            .addAction(0, "Dừng", stopPending)
            .build()
    }

    private fun updateNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTI_ID, buildNotification())
    }

    private fun createNotificationChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Technician Tracking",
            NotificationManager.IMPORTANCE_LOW
        )
        nm.createNotificationChannel(channel)
    }

    // ================= LOCATION =================

    @Suppress("MissingPermission")
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            500L
        )
            .setMinUpdateIntervalMillis(300L)
            .setMaxUpdateDelayMillis(0L)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                maybeSendLocation(loc)
            }
        }

        fusedLocation.requestLocationUpdates(
            request,
            locationCallback!!,
            Looper.getMainLooper()
        )
    }

    private fun stopLocationUpdates() {
        locationCallback?.let { fusedLocation.removeLocationUpdates(it) }
        locationCallback = null
    }

    // ================= API =================

    private fun maybeSendLocation(loc: Location) {
        val id = emergencyId ?: return
        val now = System.currentTimeMillis()
        val last = lastLocationSent

        if (last != null) {
            if (last.distanceTo(loc) < MIN_DISTANCE_M &&
                now - lastSendTime < MIN_TIME_MS
            ) return
        }

        lastLocationSent = Location(loc)
        lastSendTime = now

        val body = TechnicianLocationBody(
            emergencyRequestId = id,
            latitude = loc.latitude,
            longitude = loc.longitude,
            speedKmh = if (loc.hasSpeed()) loc.speed * 3.6 else null,
            bearing = if (loc.hasBearing()) loc.bearing.toDouble() else null,
            recomputeRoute = true
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                RetrofitInstance.techEmergencyService.updateLocation(body)
            } catch (_: Exception) {}
        }
    }

    override fun onDestroy() {
        stopLocationUpdates()
        super.onDestroy()
    }
}