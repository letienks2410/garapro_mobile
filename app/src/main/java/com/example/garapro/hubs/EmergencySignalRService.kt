package com.example.garapro.hubs

import android.util.Log
import com.google.gson.JsonObject
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class EmergencySignalRService(
    hubUrl: String
) {
    private var currentUrl: String = hubUrl
    private var hub: HubConnection = HubConnectionBuilder.create(hubUrl).build()

    private val _events = MutableSharedFlow<Pair<String, String>>(replay = 0, extraBufferCapacity = 32, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events: SharedFlow<Pair<String, String>> = _events

    fun setupListeners() {
        hub.on("EmergencyRequestTowing", { json: JsonObject ->
            _events.tryEmit("EmergencyRequestTowing" to json.toString())
        }, JsonObject::class.java)
        hub.onClosed { error ->
            Log.e("EmergencyHub", "onClosed: ${error?.message}")
            _events.tryEmit("Closed" to (error?.message ?: "unknown"))
        }
        hub.on("Connected", { connId: String ->
            _events.tryEmit("Connected" to connId)
        }, String::class.java)
        hub.on("EmergencyRequestCreated", { json: JsonObject ->
            _events.tryEmit("EmergencyRequestCreated" to json.toString())
        }, JsonObject::class.java)
        hub.on("EmergencyRequestApproved", { json: JsonObject ->
            _events.tryEmit("EmergencyRequestApproved" to json.toString())
        }, JsonObject::class.java)
        hub.on("EmergencyRequestRejected", { json: JsonObject ->
            _events.tryEmit("EmergencyRequestRejected" to json.toString())
        }, JsonObject::class.java)
        hub.on("EmergencyRequestInProgress", { json: JsonObject ->
            _events.tryEmit("EmergencyRequestInProgress" to json.toString())
        }, JsonObject::class.java)
        
        hub.on("EmergencyRequestCanceled", { json: JsonObject ->
            _events.tryEmit("EmergencyRequestCanceled" to json.toString())
        }, JsonObject::class.java)
        hub.on("EmergencyRequestExpired", { json: JsonObject ->
            _events.tryEmit("EmergencyRequestExpired" to json.toString())
        }, JsonObject::class.java)
        hub.on("TechnicianAssigned", { json: JsonObject ->
            _events.tryEmit("TechnicianAssigned" to json.toString())
        }, JsonObject::class.java)
        hub.on("TechnicianLocationUpdated", { json: JsonObject ->
            _events.tryEmit("TechnicianLocationUpdated" to json.toString())
        }, JsonObject::class.java)
        hub.on("EmergencyRequestArrived", { json: JsonObject ->
            _events.tryEmit("EmergencyRequestArrived" to json.toString())
        }, JsonObject::class.java)
        hub.on("TechnicianArrived", { json: JsonObject ->
            _events.tryEmit("TechnicianArrived" to json.toString())
        }, JsonObject::class.java)
        hub.on("JoinedCustomerGroup", { grp: String ->
            _events.tryEmit("JoinedCustomerGroup" to grp)
        }, String::class.java)
        hub.on("JoinedBranchGroup", { grp: String ->
            _events.tryEmit("JoinedBranchGroup" to grp)
        }, String::class.java)
    }

    fun start(onConnected: (() -> Unit)? = null) {
        when (hub.connectionState) {
            HubConnectionState.CONNECTED -> onConnected?.invoke()
            HubConnectionState.DISCONNECTED -> {
                Log.d("EmergencyHub", "start(): url=$currentUrl")
                hub.start().subscribe({
                    Log.d("EmergencyHub", "connected: ${hub.connectionState}")
                    onConnected?.invoke()
                }, { e ->
                    Log.e("EmergencyHub", "start error", e)
                })
            }
            else -> {}
        }
    }

    fun joinCustomerGroup(customerId: String) {
        if (hub.connectionState == HubConnectionState.CONNECTED) {
            hub.send("JoinCustomerGroup", customerId)
        } else {
            Log.w("EmergencyHub", "joinCustomerGroup while not CONNECTED")
        }
    }

    fun joinEmergencyGroup(emergencyId: String) {
        if (hub.connectionState == HubConnectionState.CONNECTED) {
            hub.send("JoinEmergencyGroup", emergencyId)
        } else {
            Log.w("EmergencyHub", "joinEmergencyGroup while not CONNECTED")
        }
    }

    fun joinBranchGroup(branchId: String) {
        if (hub.connectionState == HubConnectionState.CONNECTED) {
            hub.send("JoinBranchGroup", branchId)
        } else {
            Log.w("EmergencyHub", "joinBranchGroup while not CONNECTED")
        }
    }

    fun stop() {
        if (hub.connectionState == HubConnectionState.CONNECTED) hub.stop()
    }

    fun isConnected(): Boolean = hub.connectionState == HubConnectionState.CONNECTED

    fun reconnectWithUrl(url: String, onConnected: (() -> Unit)? = null) {
        try {
            stop()
        } catch (_: Exception) {}
        currentUrl = url
        Log.d("EmergencyHub", "reconnectWithUrl: url=$url")
        hub = HubConnectionBuilder.create(url).build()
        setupListeners()
        start(onConnected)
    }
}
