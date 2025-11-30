package com.example.garapro.hubs

import android.util.Log
import com.google.gson.JsonObject
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class JobSignalRService(
    hubUrl: String
) {

    private val hub: HubConnection =
        HubConnectionBuilder.create(hubUrl).build()

    // Chỉ emit repairOrderId, Fragment muốn làm gì tuỳ ý
    private val _events = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<String> = _events

    private var currentRepairOrderId: String? = null

    fun setupListeners() {
        hub.on("JobStatusUpdated", { json: JsonObject ->
            Log.d("SignalR", "JobStatusUpdated payload: $json")


            val roId = json.get("repairOrderId")?.asString
            if (roId != null) {
                _events.tryEmit(roId)
            }
        }, JsonObject::class.java)
    }

    fun start(onConnected: (() -> Unit)? = null) {
        when (hub.connectionState) {
            HubConnectionState.CONNECTED -> {
                Log.d("SignalR", "JobHub already connected")
                onConnected?.invoke()
            }
            HubConnectionState.DISCONNECTED -> {
                Log.d("SignalR", "JobHub starting connection...")
                hub.start().subscribe({
                    Log.d("SignalR", "JobHub connected")
                    onConnected?.invoke()
                }, { error ->
                    Log.e("SignalR", "Error starting JobHub", error)
                })
            }
            else -> {
                Log.d("SignalR", "JobHub state=${hub.connectionState}")
            }
        }
    }
    fun connectAndJoinRepairOrder(repairOrderId: String) {
        currentRepairOrderId = repairOrderId

        when (hub.connectionState) {
            HubConnectionState.CONNECTED -> {
                joinRepairOrderGroupInternal(repairOrderId)
            }
            HubConnectionState.DISCONNECTED -> {
                hub.start().subscribe({
                    Log.d("SignalR", "JobHub connected")
                    currentRepairOrderId?.let { joinRepairOrderGroupInternal(it) }
                }, { error ->
                    Log.e("SignalR", "Error starting JobHub", error)
                })
            }
            else -> {
                Log.d("SignalR", "JobHub state=${hub.connectionState}")
            }
        }
    }

    fun joinRepairOrderGroup(repairOrderId: String) {
        if (hub.connectionState == HubConnectionState.CONNECTED) {
            Log.d("SignalR", "Joining RepairOrder_$repairOrderId on JobHub")
            hub.send("JoinRepairOrderGroup", repairOrderId)
        } else {
            Log.w("SignalR", "joinRepairOrderGroup called while not CONNECTED, state=${hub.connectionState}")
        }
    }
    private fun joinRepairOrderGroupInternal(repairOrderId: String) {
        if (hub.connectionState == HubConnectionState.CONNECTED) {
            Log.d("SignalR", "Joining RepairOrder_$repairOrderId on JobHub")
            hub.send("JoinRepairOrderGroup", repairOrderId)
        } else {
            Log.w("SignalR", "joinRepairOrderGroupInternal while not CONNECTED")
        }
    }
    fun joinRepairOrderUserGroup(userId: String) {
        if (hub.connectionState == HubConnectionState.CONNECTED) {
            Log.d("SignalR", "Joining RepairOrder_$userId (User group)")
            hub.send("JoinRepairOrderUserGroup", userId)
        } else {
            Log.w("SignalR", "joinRepairOrderUserGroup called while not CONNECTED")
        }
    }
    fun leaveRepairOrderUserGroup(userId: String) {
        if (hub.connectionState == HubConnectionState.CONNECTED) {
            Log.d("SignalR", "Leaving RepairOrder_$userId (User group)")
            hub.send("LeaveRepairOrderUserGroup", userId)
        }
    }

    fun leaveRepairOrderGroupAndStop() {
        val roId = currentRepairOrderId
        if (hub.connectionState == HubConnectionState.CONNECTED && roId != null) {
            hub.send("LeaveRepairOrderGroup", roId)
            hub.stop()
        } else if (hub.connectionState == HubConnectionState.CONNECTED) {
            hub.stop()
        }
        currentRepairOrderId = null
    }

    fun stop() {
        if (hub.connectionState == HubConnectionState.CONNECTED) {
            hub.stop()
        }
    }
}