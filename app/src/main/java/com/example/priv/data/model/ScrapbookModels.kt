package com.example.priv.data.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class ScrapbookItemType {
    PHOTO, NOTE, STICKER, ACTION_SLOT
}

data class ScrapbookPageItem(
    val id: String = UUID.randomUUID().toString(),
    val type: ScrapbookItemType = ScrapbookItemType.PHOTO,
    val uriOrContent: String = "",
    val title: String = "",
    val rotationDeg: Float = 0f,
    val noteColorHex: String = "#FFFDF5",
    val authorOrPerson: String = "",
    val actionPrompt: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("type", type.name)
            put("uriOrContent", uriOrContent)
            put("title", title)
            put("rotationDeg", rotationDeg.toDouble())
            put("noteColorHex", noteColorHex)
            put("authorOrPerson", authorOrPerson)
            put("actionPrompt", actionPrompt)
            put("createdAt", createdAt)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): ScrapbookPageItem {
            return ScrapbookPageItem(
                id = json.optString("id", UUID.randomUUID().toString()),
                type = try {
                    ScrapbookItemType.valueOf(json.optString("type", "PHOTO"))
                } catch (e: Exception) {
                    ScrapbookItemType.PHOTO
                },
                uriOrContent = json.optString("uriOrContent", ""),
                title = json.optString("title", ""),
                rotationDeg = json.optDouble("rotationDeg", 0.0).toFloat(),
                noteColorHex = json.optString("noteColorHex", "#FFFDF5"),
                authorOrPerson = json.optString("authorOrPerson", ""),
                actionPrompt = json.optString("actionPrompt", ""),
                createdAt = json.optLong("createdAt", System.currentTimeMillis())
            )
        }

        fun parseList(jsonString: String?): List<ScrapbookPageItem> {
            if (jsonString.isNullOrBlank() || jsonString == "[]") return emptyList()
            return try {
                val array = JSONArray(jsonString)
                val list = mutableListOf<ScrapbookPageItem>()
                for (i in 0 until array.length()) {
                    list.add(fromJson(array.getJSONObject(i)))
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }

        fun serializeList(items: List<ScrapbookPageItem>): String {
            val array = JSONArray()
            items.forEach { array.put(it.toJson()) }
            return array.toString()
        }
    }
}
