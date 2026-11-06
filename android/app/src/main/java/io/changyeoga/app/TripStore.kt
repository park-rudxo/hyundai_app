package io.changyeoga.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class TripStore(private val context: Context) {
    private val file = AtomicFile(File(context.filesDir, "trip.json"))
    fun load(): Trip {
        if (!file.baseFile.exists()) return Trip()
        val j = JSONObject(file.readFully().toString(Charsets.UTF_8))
        require(j.getInt("version") == 1) { "지원하지 않는 저장 형식입니다." }
        val stops = j.getJSONArray("stops").let { a -> (0 until a.length()).map { a.getJSONObject(it).let { Stop(it.getString("id"),it.getString("name")) } } }
        val items = j.getJSONArray("items").let { a -> (0 until a.length()).map { a.getJSONObject(it).let { Luggage(it.getString("id"),it.getString("stopId"),it.getString("label"),if (it.isNull("photo")) null else it.getString("photo"),ItemState.valueOf(it.getString("state"))) } } }
        require(items.all { item -> stops.any { it.id == item.stopId } })
        val selected = j.optString("selected").takeIf { id -> stops.any { it.id == id } } ?: stops.firstOrNull()?.id
        return Trip(stops, items, selected, j.optBoolean("example"))
    }
    fun save(trip: Trip) {
        val j = JSONObject().put("version",1).put("example",trip.example).put("selected",trip.selectedStopId)
        j.put("stops",JSONArray().apply { trip.stops.forEach { put(JSONObject().put("id",it.id).put("name",it.name)) } })
        j.put("items",JSONArray().apply { trip.items.forEach { put(JSONObject().put("id",it.id).put("stopId",it.stopId).put("label",it.label).put("photo",it.photo ?: JSONObject.NULL).put("state",it.state.name)) } })
        val stream = file.startWrite()
        try { stream.write(j.toString().toByteArray()); file.finishWrite(stream) } catch(e: Exception) { file.failWrite(stream); throw e }
    }
    fun savePhoto(bitmap: Bitmap): String {
        val folder = File(context.filesDir,"photos").apply { mkdirs() }
        val name = UUID.randomUUID().toString()+".jpg"
        File(folder,name).outputStream().use { require(bitmap.compress(Bitmap.CompressFormat.JPEG,85,it)) }
        return name
    }
    fun importPhoto(uri: Uri): String {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it,null,options) }
        require(options.outWidth > 0 && options.outHeight > 0) { "읽을 수 없는 사진입니다." }
        var sample = 1
        while (maxOf(options.outWidth,options.outHeight)/sample > 1200) sample *= 2
        val bitmap = context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply { inSampleSize = sample }) } ?: error("사진을 열 수 없습니다.")
        return try { savePhoto(bitmap) } finally { bitmap.recycle() }
    }
    fun photoFile(name: String): File { require(name.matches(Regex("[a-f0-9-]+\\.jpg"))); return File(context.filesDir,"photos/$name") }
    fun deletePhoto(name: String?) { if(name != null) photoFile(name).delete() }
}
