package com.example.priv.data.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Item de mídia contido diretamente dentro de uma Cápsula do Tempo.
 * Permite fotos, vídeos, notas de texto, colagens de recorte e músicas/áudios.
 */
data class CapsuleMediaItem(
    val id: String = UUID.randomUUID().toString(),
    val type: String, // "IMAGE", "VIDEO", "TEXT", "AUDIO", "COLLAGE"
    val title: String = "",
    val contentUri: String = "",
    val textContent: String = "",
    val extraMetadata: String = "" // Ex: nome do artista, duração, emoji
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("type", type)
        json.put("title", title)
        json.put("contentUri", contentUri)
        json.put("textContent", textContent)
        json.put("extraMetadata", extraMetadata)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): CapsuleMediaItem {
            return CapsuleMediaItem(
                id = json.optString("id", UUID.randomUUID().toString()),
                type = json.optString("type", "TEXT"),
                title = json.optString("title", ""),
                contentUri = json.optString("contentUri", ""),
                textContent = json.optString("textContent", ""),
                extraMetadata = json.optString("extraMetadata", "")
            )
        }

        fun listToJsonString(items: List<CapsuleMediaItem>): String {
            val array = JSONArray()
            items.forEach { array.put(it.toJson()) }
            return array.toString()
        }

        fun listFromJsonString(jsonString: String): List<CapsuleMediaItem> {
            if (jsonString.isBlank()) return emptyList()
            return try {
                val array = JSONArray(jsonString)
                val list = mutableListOf<CapsuleMediaItem>()
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i)
                    if (obj != null) {
                        list.add(fromJson(obj))
                    }
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
