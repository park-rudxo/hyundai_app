package io.changyeoga.app

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder

data class PlaceResult(val name:String,val address:String,val latitude:Double,val longitude:Double)
object PlaceSearch {
    fun find(query:String):List<PlaceResult> {
        val base=BuildConfig.PLACE_SEARCH_URL.trimEnd('/')
        require(base.isNotEmpty())
        val url=URI.create(base+"/places?q="+URLEncoder.encode(query,"UTF-8")).toURL()
        val c=url.openConnection() as HttpURLConnection
        c.connectTimeout=10000;c.readTimeout=15000;c.instanceFollowRedirects=false
        try {
            check(c.responseCode==200)
            val json=c.inputStream.bufferedReader().use { it.readText() }
            val items=JSONObject(json).getJSONArray("places")
            return (0 until items.length()).map { i ->
                val x=items.getJSONObject(i)
                val lat=x.getDouble("latitude");val lon=x.getDouble("longitude")
                require(lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0)
                PlaceResult(x.getString("name"),x.optString("address"),lat,lon)
            }
        } finally { c.disconnect() }
    }
}
