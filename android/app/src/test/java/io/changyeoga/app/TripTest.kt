package io.changyeoga.app

import org.junit.Assert.*
import org.junit.Test

class TripTest {
    @Test fun arrivalDoesNotDeliverOrShowOtherStopItems() {
        val trip=Trip.example().changeState("fruit",ItemState.LOADED).selectStop("grandpa")
        assertEquals(listOf("gift"),trip.forStop(trip.selectedStopId!!).map{it.id})
        assertEquals(ItemState.LOADED,trip.items.first{it.id=="fruit"}.state)
    }
    @Test(expected=IllegalArgumentException::class) fun cannotDeliverBeforeLoading() { Trip.example().changeState("fruit",ItemState.DELIVERED) }
    @Test fun handoverAndUndoAreIndependentPerItem() {
        val t=Trip.example().changeState("fruit",ItemState.LOADED).changeState("fruit",ItemState.DELIVERED)
        assertEquals(ItemState.READY,t.items.last().state)
        assertEquals(ItemState.LOADED,t.changeState("fruit",ItemState.LOADED).items.first().state)
    }
    @Test fun missingSignalsAndDrivingFailClosed() {
        assertFalse(allowVehicleInteraction(true,null,false))
        assertFalse(allowVehicleInteraction(true,true,null))
        assertFalse(allowVehicleInteraction(false,true,false))
        assertFalse(allowVehicleInteraction(true,false,false))
        assertFalse(allowVehicleInteraction(true,true,true))
        assertTrue(allowVehicleInteraction(true,true,false))
    }
    @Test(expected=IllegalArgumentException::class) fun rejectsOrphanItem() { Trip().addItem(Luggage(stopId="missing")) }
    @Test fun blankAndLongStopNames() {
        assertTrue(runCatching{Trip().addStop("  ")}.isFailure)
        assertEquals(30,Trip().addStop("가".repeat(40)).stops.single().name.length)
    }
}
