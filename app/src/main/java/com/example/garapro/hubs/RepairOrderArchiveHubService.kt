package com.example.garapro.hubs

import android.util.Log
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class RepairOrderArchiveHubService(hubUrl: String) {

    private val hub: HubConnection =
        HubConnectionBuilder.create(hubUrl).build()

    private val _events = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<String> = _events

    private var userId: String? = null

    fun setupListeners() {
        hub.on(
            "RepairOrderArchived",
            { roId: String ->
                Log.d("ArchiveHub", "RepairOrderArchived: $roId")
                _events.tryEmit(roId)
            },
            String::class.java
        )
    }

    fun connectAndJoin(userId: String) {
        this.userId = userId

        if (hub.connectionState == HubConnectionState.DISCONNECTED) {
            hub.start().subscribe({
                Log.d("ArchiveHub", "Connected")
                hub.send("JoinArchiveGroup", userId)
            }, {
                Log.e("ArchiveHub", "Connection error", it)
            })
        }
    }

    fun leaveAndStop() {
        val uid = userId ?: return
        if (hub.connectionState == HubConnectionState.CONNECTED) {
            hub.send("LeaveArchiveGroup", uid)
        }
        hub.stop()
    }
}