package com.example.garapro.hubs

sealed class RepairOrderEvent {
    data class Updated(
        val repairOrderId: String
    ) : RepairOrderEvent()
}