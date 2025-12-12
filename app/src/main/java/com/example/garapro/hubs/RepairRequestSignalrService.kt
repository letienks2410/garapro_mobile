package com.example.garapro.hubs

import android.util.Log
import com.google.gson.JsonObject
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class RepairRequestSignalrService(
    hubUrl: String
) {

    private val hub: HubConnection =
        HubConnectionBuilder.create(hubUrl).build()

    // chỉ emit repairRequestId từ payload
    private val _events = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<String> = _events

    private var currentRepairRequestId: String? = null
    private var currentUserId: String? = null

    fun setupListeners() {

        hub.on(
            "CompletedRepairRequest",
            { json: JsonObject ->
                val rrId = json.get("repairRequestId")?.asString
                Log.d("SignalR", "CompletedRepairRequest: $rrId / $json")
                if (rrId != null) _events.tryEmit(rrId)
            },
            JsonObject::class.java
        )

        hub.on(
            "RepairUpdated",
            { json: JsonObject ->
                val rrId = json.get("repairRequestId")?.asString
                Log.d("SignalR", "RepairUpdated: $rrId / $json")
                if (rrId != null) _events.tryEmit(rrId)
            },
            JsonObject::class.java
        )
    }

    /**
     * JOIN group theo RepairRequestId: RepairRequest_{repairRequestId}
     * (dùng cho màn detail nếu cần)
     */
    fun connectAndJoinRepairRequest(repairRequestId: String) {
        currentRepairRequestId = repairRequestId

        when (hub.connectionState) {
            HubConnectionState.CONNECTED -> joinRepairRequestGroupInternal(repairRequestId)
            HubConnectionState.DISCONNECTED -> {
                hub.start().subscribe(
                    {
                        currentRepairRequestId?.let { joinRepairRequestGroupInternal(it) }
                    },
                    { err -> Log.e("SignalR", "Start error", err) }
                )
            }
            else -> Log.d("SignalR", "Waiting for connection...")
        }
    }

    private fun joinRepairRequestGroupInternal(repairRequestId: String) {
        if (hub.connectionState == HubConnectionState.CONNECTED) {
            Log.d("SignalR", "Joining RepairRequest_$repairRequestId")
            hub.send("JoinRepairRequestGroup", repairRequestId)
        }
    }

    fun leaveRepairRequestGroupAndStop() {
        val rrId = currentRepairRequestId
        if (hub.connectionState == HubConnectionState.CONNECTED && rrId != null) {
            hub.send("LeaveRepairRequestGroup", rrId)
            hub.stop()
        } else if (hub.connectionState == HubConnectionState.CONNECTED) {
            hub.stop()
        }
        currentRepairRequestId = null
    }

    /**
     * JOIN group theo UserId: RepairRequestUser_{userId}
     * (dùng cho màn danh sách Appointments)
     */
    fun connectAndJoinUser(userId: String) {
        currentUserId = userId

        when (hub.connectionState) {
            HubConnectionState.CONNECTED -> joinUserGroupInternal(userId)
            HubConnectionState.DISCONNECTED -> {
                hub.start().subscribe(
                    {
                        currentUserId?.let { joinUserGroupInternal(it) }
                    },
                    { err -> Log.e("SignalR", "Start error", err) }
                )
            }
            else -> Log.d("SignalR", "Waiting for connection...")
        }
    }

    private fun joinUserGroupInternal(userId: String) {
        if (hub.connectionState == HubConnectionState.CONNECTED) {
            Log.d("SignalR", "Joining RepairRequestUser_$userId")
            hub.send("JoinRepairRequestUserGroup", userId)
        }
    }

    fun leaveUserGroupAndStop() {
        val uid = currentUserId
        if (hub.connectionState == HubConnectionState.CONNECTED && uid != null) {
            hub.send("LeaveRepairRequestUserGroup", uid)
            hub.stop()
        } else if (hub.connectionState == HubConnectionState.CONNECTED) {
            hub.stop()
        }
        currentUserId = null
    }
}
