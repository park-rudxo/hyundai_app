package io.changyeoga.app

import java.util.UUID

data class Stop(val id: String = UUID.randomUUID().toString(), val name: String)
enum class ItemState { READY, LOADED, DELIVERED }
data class Luggage(val id: String = UUID.randomUUID().toString(), val stopId: String, val label: String = "사진 짐", val photo: String? = null, val state: ItemState = ItemState.READY)
data class Trip(val stops: List<Stop> = emptyList(), val items: List<Luggage> = emptyList(), val selectedStopId: String? = null, val example: Boolean = false) {
    fun forStop(id: String) = items.filter { it.stopId == id }
    fun addStop(name: String): Trip {
        val clean = name.trim().take(30)
        require(clean.isNotEmpty())
        val stop = Stop(name = clean)
        return copy(stops = stops + stop, selectedStopId = selectedStopId ?: stop.id)
    }
    fun addItem(item: Luggage): Trip {
        require(stops.any { it.id == item.stopId })
        return copy(items = items + item)
    }
    fun changeState(id: String, next: ItemState): Trip = copy(items = items.map {
        if (it.id != id) it else {
            require(!(it.state == ItemState.READY && next == ItemState.DELIVERED)) { "먼저 적재를 확인해 주세요." }
            it.copy(state = next)
        }
    })
    fun selectStop(id: String): Trip { require(stops.any { it.id == id }); return copy(selectedStopId = id) }
    fun removeItem(id: String) = copy(items = items.filterNot { it.id == id })
    companion object {
        fun example(): Trip {
            val a = Stop(id = "grandma", name = "외가")
            val b = Stop(id = "grandpa", name = "친가")
            return Trip(listOf(a,b), listOf(Luggage(id="fruit",stopId=a.id,label="과일 바구니"), Luggage(id="gift",stopId=b.id,label="선물 상자")), a.id, true)
        }
    }
}

/** Unknown/disconnected and stationary-in-D states remain locked. */
fun allowVehicleInteraction(serviceReady: Boolean, parked: Boolean?, uxRestricted: Boolean?): Boolean =
    serviceReady && parked == true && uxRestricted == false
