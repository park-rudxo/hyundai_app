package io.changyeoga.app

import kotlin.math.*

data class Position(val latitude: Double, val longitude: Double, val accuracy: Float, val elapsedMs: Long)
fun distanceMeters(a: Double, b: Double, c: Double, d: Double): Double {
    val lat = Math.toRadians(c-a); val lon = Math.toRadians(d-b)
    val h = sin(lat/2).pow(2) + cos(Math.toRadians(a))*cos(Math.toRadians(c))*sin(lon/2).pow(2)
    return 6371000 * 2 * asin(sqrt(h.coerceIn(0.0,1.0)))
}
fun Position.usable(now: Long) = latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0 && accuracy.isFinite() && accuracy in 0f..50f && now-elapsedMs in 0..15000

/** Near a saved stop for 20 seconds; a suggestion, never proof of parking or delivery. */
class ArrivalDetector {
    private var candidate: String? = null
    private var since = 0L
    private var last = -1L
    private val announced = mutableSetOf<String>()
    fun update(stops: List<Stop>, fix: Position, now: Long): Stop? {
        if(!fix.usable(now)) { candidate=null; return null }
        if(fix.elapsedMs <= last) return null
        if(last >= 0 && fix.elapsedMs-last > 15000) candidate=null
        last=fix.elapsedMs
        val distances=stops.filter { it.latitude!=null }.map { it to distanceMeters(fix.latitude,fix.longitude,it.latitude!!,it.longitude!!) }
        distances.filter { it.second>500 }.forEach { announced.remove(it.first.id) }
        val nearby=distances.filter { it.second+fix.accuracy<=150 }.sortedBy { it.second }
        // Adjacent destinations need manual selection rather than a guess.
        val stop=nearby.firstOrNull()?.first?.takeIf { nearby.size==1 || nearby[1].second-nearby[0].second>50 }
        if(stop==null) { candidate=null; return null }
        if(candidate!=stop.id) { candidate=stop.id; since=fix.elapsedMs }
        return if(fix.elapsedMs-since>=20000 && announced.add(stop.id)) stop else null
    }
}

sealed interface VoiceAction {
    data object Read : VoiceAction
    data object Prepare : VoiceAction
    data class Arrive(val id: String) : VoiceAction
    data object Unknown : VoiceAction
}
fun parseVoice(text: String, stops: List<Stop>): VoiceAction {
    fun clean(s: String)=s.replace(Regex("[\\s.!?。]"),"")
    val command=clean(text)
    if(command in listOf("짐알려줘","짐읽어줘","뭐챙겨야해")) return VoiceAction.Read
    if(command=="출발준비") return VoiceAction.Prepare
    val matches=stops.filter { command==clean(it.name)+"도착" || command==clean(it.name)+"에도착" }
    return matches.singleOrNull()?.let { VoiceAction.Arrive(it.id) } ?: VoiceAction.Unknown
}
fun luggageSummary(trip: Trip, id: String): String {
    val name=trip.stops.firstOrNull { it.id==id }?.name ?: return "방문지를 선택해 주세요."
    val items=trip.forStop(id)
    return "$name. 차에 실은 짐 ${items.count { it.state==ItemState.LOADED }}개, 아직 적재 확인 전 ${items.count { it.state==ItemState.READY }}개, 전달 완료 ${items.count { it.state==ItemState.DELIVERED }}개입니다."
}
