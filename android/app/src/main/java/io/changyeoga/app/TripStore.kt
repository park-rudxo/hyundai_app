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
        return decode(file.readFully())
    }
    private fun decode(bytes: ByteArray): Trip {
        val j = JSONObject(bytes.toString(Charsets.UTF_8))
        require(j.getInt("version") == 1) { "지원하지 않는 저장 형식입니다." }
        val stops = j.getJSONArray("stops").let { a -> (0 until a.length()).map { a.getJSONObject(it).let { Stop(it.getString("id"),it.getString("name"),if(it.isNull("latitude")) null else it.getDouble("latitude"),if(it.isNull("longitude")) null else it.getDouble("longitude")) } } }
        val items = j.getJSONArray("items").let { a -> (0 until a.length()).map { a.getJSONObject(it).let { Luggage(it.getString("id"),it.getString("stopId"),it.getString("label"),if (it.isNull("photo")) null else it.getString("photo"),ItemState.valueOf(it.getString("state"))) } } }
        require(items.all { item -> stops.any { it.id == item.stopId } })
        require(stops.size <= 50 && items.size <= 100)
        require(stops.map { it.id }.distinct().size == stops.size && items.map { it.id }.distinct().size == items.size)
        require(stops.all { it.name.isNotBlank() && it.name.length <= 30 })
        val selected = j.optString("selected").takeIf { id -> stops.any { it.id == id } } ?: stops.firstOrNull()?.id
        return Trip(stops, items, selected, j.optBoolean("example"))
    }
    fun save(trip: Trip) {
        val bytes = encode(trip)
        val stream = file.startWrite()
        try { stream.write(bytes); file.finishWrite(stream) } catch(e: Exception) { file.failWrite(stream); throw e }
    }
    private fun encode(trip: Trip): ByteArray {
        val j = JSONObject().put("version",1).put("example",trip.example).put("selected",trip.selectedStopId)
        j.put("stops",JSONArray().apply { trip.stops.forEach { put(JSONObject().put("id",it.id).put("name",it.name).put("latitude",it.latitude ?: JSONObject.NULL).put("longitude",it.longitude ?: JSONObject.NULL)) } })
        j.put("items",JSONArray().apply { trip.items.forEach { put(JSONObject().put("id",it.id).put("stopId",it.stopId).put("label",it.label).put("photo",it.photo ?: JSONObject.NULL).put("state",it.state.name)) } })
        return j.toString().toByteArray()
    }
    fun exportTrip(trip: Trip, output: java.io.OutputStream) {
        val metadata = encode(trip)
        decode(metadata)
        val photos = trip.items.mapNotNull { it.photo }.distinct()
        require(metadata.size.toLong() + photos.sumOf { photoFile(it).length() } <= 20L * 1024 * 1024) {
            "사진과 목록은 20MB 이하만 전송할 수 있습니다."
        }
        java.util.zip.ZipOutputStream(output).use { zip ->
            zip.putNextEntry(java.util.zip.ZipEntry("trip.json")); zip.write(metadata); zip.closeEntry()
            photos.forEach { name ->
                zip.putNextEntry(java.util.zip.ZipEntry("photos/$name"))
                photoFile(name).inputStream().use { it.copyTo(zip) }; zip.closeEntry()
            }
        }
    }
    /** Fully validate before writing; imported photos get new private filenames. */
    fun importTrip(input: java.io.InputStream): Trip {
        val entries = mutableMapOf<String, ByteArray>()
        var total = 0
        java.util.zip.ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(entries.size < 101 && !entry.isDirectory && entry.name !in entries)
                require(entry.name == "trip.json" || entry.name.matches(Regex("photos/[a-f0-9-]+\\.jpg")))
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = zip.read(buffer); if(count < 0) break
                    total += count; require(total <= 20 * 1024 * 1024) { "전송 파일은 20MB 이하만 지원합니다." }
                    output.write(buffer,0,count)
                }
                entries[entry.name] = output.toByteArray()
            }
        }
        val parsed = decode(requireNotNull(entries["trip.json"]))
        val photos = parsed.items.mapNotNull { it.photo }.distinct()
        photos.forEach { name ->
            val bytes = requireNotNull(entries["photos/$name"])
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes,0,bytes.size,options)
            require(options.outWidth in 1..4096 && options.outHeight in 1..4096)
        }
        val created = mutableListOf<String>()
        try {
            val mapping = photos.associateWith { name ->
                val newName = UUID.randomUUID().toString()+".jpg"
                created.add(newName)
                photoFile(newName).parentFile!!.mkdirs()
                photoFile(newName).writeBytes(entries.getValue("photos/$name"))
                newName
            }
            val result = parsed.copy(items=parsed.items.map { it.copy(photo=it.photo?.let(mapping::getValue)) })
            save(result)
            return result
        } catch(e: Exception) { created.forEach(::deletePhoto); throw e }
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
