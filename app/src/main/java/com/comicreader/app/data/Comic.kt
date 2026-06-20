package com.comicreader.app.data

import org.json.JSONObject

data class Comic(
    val id: String,
    val title: String,
    val coverPath: String? = null,
    val pageCount: Int = 0,
    val lastReadPage: Int = 0,
    val addedAt: Long = System.currentTimeMillis(),
    val directoryPath: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("coverPath", coverPath ?: "")
        put("pageCount", pageCount)
        put("lastReadPage", lastReadPage)
        put("addedAt", addedAt)
        put("directoryPath", directoryPath)
    }

    companion object {
        fun fromJson(json: JSONObject): Comic = Comic(
            id = json.getString("id"),
            title = json.getString("title"),
            coverPath = json.optString("coverPath", null)?.ifBlank { null },
            pageCount = json.optInt("pageCount", 0),
            lastReadPage = json.optInt("lastReadPage", 0),
            addedAt = json.optLong("addedAt", System.currentTimeMillis()),
            directoryPath = json.getString("directoryPath")
        )
    }
}
