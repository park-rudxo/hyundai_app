package io.changyeoga.app

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder

data class PlaceResult(val name:String,val address:String,val latitude:Double,val longitude:Double)
object PlaceSearch {
    private val cache = android.util.LruCache<String, List<PlaceResult>>(30)
    fun find(query:String):List<PlaceResult> {
        cache.get(query)?.let { return it }
        val base=BuildConfig.PLACE_SEARCH_URL.trimEnd('/')
        require(base.isNotEmpty())
        val url=URI.create(base+"/places?q="+URLEncoder.encode(query,"UTF-8")).toURL()
        val c=url.openConnection() as HttpURLConnection
        c.connectTimeout=10000;c.readTimeout=15000;c.instanceFollowRedirects=false
        try {
            check(c.responseCode==200)
            val json=c.inputStream.bufferedReader().use { it.readText() }
            val items=JSONObject(json).getJSONArray("places")
            val places = (0 until items.length()).map { i ->
                val x=items.getJSONObject(i)
                val lat=x.getDouble("latitude");val lon=x.getDouble("longitude")
                require(lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0)
                PlaceResult(x.getString("name"),x.optString("address"),lat,lon)
            }
            fun normalized(value:String)=value.replace(Regex("\\s+"), "").lowercase()
            val key=normalized(query)
            val ranked=places.sortedBy { when {
                normalized(it.name)==key -> 0
                normalized(it.name).startsWith(key) -> 1
                normalized(it.name).contains(key) -> 2
                else -> 3
            } }
            cache.put(query,ranked)
            return ranked
        } finally { c.disconnect() }
    }
}
