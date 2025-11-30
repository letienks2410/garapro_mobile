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
    private val hub: HubConnection = HubConnectionBuilder.create(hubUrl).build()

    private val _events = MutableSharedFlow<Pair<String, String>>(replay = 0, extraBufferCapacity = 32, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events: SharedFlow<Pair<String, String>> = _events

    fun setupListeners() {
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
                hub.start().subscribe({ onConnected?.invoke() }, { e -> Log.e("EmergencyHub", "start error", e) })
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
}