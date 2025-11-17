package com.example.garapro.hubs

import android.util.Log
import com.google.gson.JsonObject
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class RepairOrderSignalRService(
    hubUrl: String  // ví dụ: "https://your-api.com/repairHub"
) {

    private val hub: HubConnection =
        HubConnectionBuilder
            .create(hubUrl)
            .build()

    // chỉ emit RepairOrderId
    private val _events = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<String> = _events

    private var currentRepairOrderId: String? = null

    fun setupListeners() {

        // RepairCreated
        hub.on(
            "RepairCreated",
            { json: JsonObject ->
                val roId = json.get("repairOrderId")?.asString
                Log.d("SignalR", "RepairCreated payload: $json, RO=$roId")
                if (roId != null) {
                    _events.tryEmit(roId)
                }
            },
            JsonObject::class.java
        )

        // RepairUpdated
        hub.on(
            "RepairUpdated",
            { json: JsonObject ->
                val roId = json.get("repairOrderId")?.asString
                Log.d("SignalR", "RepairUpdated payload: $json, RO=$roId")
                if (roId != null) {
                    _events.tryEmit(roId)
                }
            },
            JsonObject::class.java
        )
    }

    /**
     * Gọi 1 lần: nó sẽ connect và join đúng group RepairOrder_{repairOrderId}
     */
    fun connectAndJoin(repairOrderId: String) {
        currentRepairOrderId = repairOrderId

        when (hub.connectionState) {
            HubConnectionState.CONNECTED -> {
                Log.d("SignalR", "Already connected, joining group...")
                joinGroupInternal(repairOrderId)
            }
            HubConnectionState.DISCONNECTED -> {
                Log.d("SignalR", "Starting connection...")
                hub.start().subscribe(
                    {
                        Log.d("SignalR", "RepairHub connected")
                        currentRepairOrderId?.let { id ->
                            joinGroupInternal(id)
                        }
                    },
                    { error ->
                        Log.e("SignalR", "Error starting RepairHub", error)
                    }
                )
            }
            else -> {
                Log.d("SignalR", "Connection state=${hub.connectionState}, will join after connected")
            }
        }
    }

    private fun joinGroupInternal(repairOrderId: String) {
        if (hub.connectionState == HubConnectionState.CONNECTED) {
            Log.d("SignalR", "Joining group RepairOrder_$repairOrderId")
            hub.send("JoinRepairOrderGroup", repairOrderId)
        } else {
            Log.w("SignalR", "joinGroupInternal while not CONNECTED, state=${hub.connectionState}")
        }
    }

    fun leaveGroupAndStop() {
        val roId = currentRepairOrderId
        if (hub.connectionState == HubConnectionState.CONNECTED && roId != null) {
            Log.d("SignalR", "Leaving group RepairOrder_$roId")
            hub.send("LeaveRepairOrderGroup", roId)
            hub.stop()
        } else if (hub.connectionState == HubConnectionState.CONNECTED) {
            hub.stop()
        }
        currentRepairOrderId = null
    }
}

