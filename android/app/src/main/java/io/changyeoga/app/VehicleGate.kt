package io.changyeoga.app

import android.car.Car
import android.car.VehicleGear
import android.car.VehiclePropertyIds
import android.car.drivingstate.CarUxRestrictionsManager
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.os.Handler
import android.os.Looper

class VehicleGate(context: Context, private val changed: (Boolean,String) -> Unit) {
    private val handler = Handler(Looper.getMainLooper())
    private var car: Car? = null
    private var ux: CarUxRestrictionsManager? = null
    private var properties: CarPropertyManager? = null
    private var ready = false
    private var stopped = false
    private val poll = object: Runnable { override fun run() { refresh(); if(!stopped) handler.postDelayed(this,500) } }
    init {
        changed(false,"차량 상태 확인 중")
        try {
            car = Car.createCar(context,handler,0) { c, connected ->
                if (!stopped) {
                    ready = connected
                    if(connected) {
                        try {
                            ux = c.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager
                            properties = c.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
                            ux?.registerListener { refresh() }
                        } catch (_: Exception) { ready = false }
                    }
                    refresh()
                }
            }
            handler.post(poll)
        } catch (_: Exception) { changed(false,"차량 서비스 연결 불가 · 조작 잠금") }
    }
    private fun refresh() {
        if(stopped) return
        try {
            val parked = properties?.getIntProperty(VehiclePropertyIds.GEAR_SELECTION,0)?.let { it == VehicleGear.GEAR_PARK }
            val restricted = ux?.currentCarUxRestrictions?.isRequiresDistractionOptimization
            val allowed = allowVehicleInteraction(ready,parked,restricted)
            changed(allowed,if(allowed) "P단 확인 · 차량 신호 연결" else "주차 상태 미확인 또는 주행 제한 · 조작 잠금")
        } catch (_: Exception) { changed(false,"차량 신호를 읽을 수 없음 · 조작 잠금") }
    }
    fun close() { stopped=true; handler.removeCallbacks(poll); runCatching { ux?.unregisterListener() }; runCatching { car?.disconnect() } }
}
