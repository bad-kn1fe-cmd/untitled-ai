package com.example.util

import org.json.JSONArray
import org.json.JSONObject

sealed class ChatMediaItem {
    data class Photo(
        val base64Data: String
    ) : ChatMediaItem()

    data class Video(
        val durationSec: Int,
        val thumbnailBase64: String,
        val frameBase64List: List<String>
    ) : ChatMediaItem()
}

object ChatMediaHelper {
    const val MAX_MEDIA_COUNT = 10

    fun serialize(items: List<ChatMediaItem>): String? {
        if (items.isEmpty()) return null
        if (items.size == 1) {
            return when (val first = items.first()) {
                is ChatMediaItem.Photo -> first.base64Data
                is ChatMediaItem.Video -> "video_frames:" + first.frameBase64List.joinToString("||")
            }
        }
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            when (item) {
                is ChatMediaItem.Photo -> {
                    obj.put("type", "photo")
                    obj.put("url", item.base64Data)
                }
                is ChatMediaItem.Video -> {
                    obj.put("type", "video")
                    obj.put("durationSec", item.durationSec)
                    obj.put("thumbnail", item.thumbnailBase64)
                    val framesArray = JSONArray()
                    for (f in item.frameBase64List) {
                        framesArray.put(f)
                    }
                    obj.put("frames", framesArray)
                }
            }
            array.put(obj)
        }
        return "media_bundle:" + array.toString()
    }

    fun deserialize(raw: String?): List<ChatMediaItem> {
        if (raw.isNullOrBlank()) return emptyList()
        if (raw.startsWith("media_bundle:")) {
            val jsonStr = raw.removePrefix("media_bundle:")
            return try {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<ChatMediaItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val type = obj.optString("type", "photo")
                    if (type == "video") {
                        val duration = obj.optInt("durationSec", 0)
                        val thumb = obj.optString("thumbnail", "")
                        val framesArray = obj.optJSONArray("frames")
                        val frames = mutableListOf<String>()
                        if (framesArray != null) {
                            for (j in 0 until framesArray.length()) {
                                frames.add(framesArray.getString(j))
                            }
                        }
                        list.add(
                            ChatMediaItem.Video(
                                durationSec = duration,
                                thumbnailBase64 = thumb.ifBlank { frames.firstOrNull() ?: "" },
                                frameBase64List = frames
                            )
                        )
                    } else {
                        val url = obj.optString("url", "")
                        if (url.isNotBlank()) {
                            list.add(ChatMediaItem.Photo(url))
                        }
                    }
                }
                list
            } catch (_: Exception) {
                emptyList()
            }
        } else if (raw.startsWith("video_frames:")) {
            val frames = raw.removePrefix("video_frames:").split("||").filter { it.isNotBlank() }
            return listOf(
                ChatMediaItem.Video(
                    durationSec = 0,
                    thumbnailBase64 = frames.firstOrNull() ?: "",
                    frameBase64List = frames
                )
            )
        } else {
            return listOf(ChatMediaItem.Photo(raw))
        }
    }
}
