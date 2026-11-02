package com.example.data.repository

import com.example.data.local.entity.CharacterEntity
import org.json.JSONArray
import org.json.JSONObject

object CharacterSerializer {

    /**
     * Serializes a CharacterEntity to a portable JSON string.
     */
    fun exportToJson(character: CharacterEntity): String {
        val root = JSONObject()
        root.put("spec", "untitled_ai_character_card")
        root.put("spec_version", "1.0")

        val data = JSONObject()
        data.put("name", character.name)
        data.put("avatar_uri", character.avatarUri)
        data.put("short_description", character.shortDescription)
        data.put("appearance", character.appearance)
        data.put("personality", character.personality)
        data.put("greeting_message", character.greetingMessage)
        data.put("habits", character.habits)
        data.put("likes", character.likes)
        data.put("dislikes", character.dislikes)
        data.put("age", character.age)
        data.put("birth_date", character.birthDate)
        data.put("knowledge_cards", try { JSONArray(character.knowledgeCards) } catch (_: Exception) { JSONArray() })
        data.put("reference_images", try { JSONArray(character.referenceImages) } catch (_: Exception) { JSONArray() })
        data.put("tags", try { JSONArray(character.tags) } catch (_: Exception) { JSONArray() })
        data.put("voice_style", character.voiceStyle)
        data.put("is_adult", character.isAdult)
        data.put("banner_uri", character.bannerUri)
        data.put("greeting_banner_uri", character.greetingBannerUri)
        data.put("communication_formality", character.communicationFormality)
        data.put("starting_relationship", character.startingRelationship)
        data.put("custom_relationship", character.customRelationship)
        data.put("initial_relationship_title", character.initialRelationshipTitle)
        data.put("initial_relationship_percent", character.initialRelationshipPercent)
        if (character.relationshipBoardJson.isNotBlank()) {
            data.put("relationship_board", try { JSONObject(character.relationshipBoardJson) } catch (_: Exception) { character.relationshipBoardJson })
        }

        val frameBase64 = when {
            character.avatarFrameUri.isBlank() -> ""
            com.example.util.ImageStorageHelper.isBase64Image(character.avatarFrameUri) -> character.avatarFrameUri
            character.avatarFrameUri.startsWith("file://") -> com.example.util.ImageStorageHelper.fileUriToPngBase64(character.avatarFrameUri) ?: character.avatarFrameUri
            else -> character.avatarFrameUri
        }
        data.put("avatar_frame", frameBase64)
        data.put("avatar_frame_base64", frameBase64)

        data.put("by", character.author.ifBlank { "UntitledAI" })

        root.put("data", data)
        return root.toString(2)
    }

    /**
     * Imports a CharacterEntity from JSON string.
     */
    fun importFromJson(jsonString: String, context: android.content.Context? = null): Result<CharacterEntity> {
        return try {
            val root = JSONObject(jsonString)
            val data = if (root.has("data") && root.optJSONObject("data") != null) {
                root.getJSONObject("data")
            } else {
                root // fallback to flat JSON
            }

            val name = data.optString("name", data.optString("char_name", "")).trim()
            if (name.isBlank()) {
                return Result.failure(IllegalArgumentException("Поле 'name' персонажа обязательно"))
            }

            val age = data.optInt("age", 18).coerceIn(1, 9999)
            val isAdult = if (data.has("is_adult")) data.optBoolean("is_adult", age >= 18) else (age >= 18)

            val knowledgeCardsStr = when {
                data.has("knowledge_cards") -> {
                    val v = data.get("knowledge_cards")
                    if (v is JSONArray) v.toString() else v.toString()
                }
                else -> "[]"
            }

            val rawRefImages = when {
                data.has("reference_images") -> {
                    val v = data.get("reference_images")
                    if (v is JSONArray) v.toString() else v.toString()
                }
                else -> "[]"
            }

            val referenceImagesStr = if (context != null && rawRefImages.isNotBlank() && rawRefImages != "[]") {
                try {
                    val arr = JSONArray(rawRefImages)
                    val cleanArr = JSONArray()
                    for (i in 0 until arr.length()) {
                        val item = arr.optString(i, "")
                        if (com.example.util.ImageStorageHelper.isBase64Image(item)) {
                            val saved = com.example.util.ImageStorageHelper.saveBase64ImageFile(context, item, "references")
                            cleanArr.put(saved ?: item)
                        } else if (item.isNotBlank()) {
                            cleanArr.put(item)
                        }
                    }
                    cleanArr.toString()
                } catch (_: Exception) {
                    rawRefImages
                }
            } else {
                rawRefImages
            }

            val tagsStr = when {
                data.has("tags") -> {
                    val v = data.get("tags")
                    if (v is JSONArray) {
                        v.toString()
                    } else if (v is String) {
                        try {
                            JSONArray(v).toString()
                        } catch (_: Exception) {
                            val arr = JSONArray()
                            v.split(",", ";").map { it.trim() }.filter { it.isNotBlank() }.forEach { arr.put(it) }
                            arr.toString()
                        }
                    } else {
                        "[]"
                    }
                }
                else -> "[]"
            }

            val author = data.optString("by", data.optString("author", data.optString("creator", "UntitledAI"))).ifBlank { "UntitledAI" }
            val rawAvatarUri = data.optString("avatar_uri", data.optString("avatar", "")).trim()
            val finalAvatarUri = when {
                rawAvatarUri.isBlank() || rawAvatarUri.equals("null", ignoreCase = true) -> {
                    "drawable://app_logo_black"
                }
                context != null && com.example.util.ImageStorageHelper.isBase64Image(rawAvatarUri) -> {
                    com.example.util.ImageStorageHelper.saveBase64ImageFile(context, rawAvatarUri, "avatars") ?: rawAvatarUri
                }
                else -> {
                    rawAvatarUri
                }
            }

            val rawBannerUri = data.optString("banner_uri", "").trim()
            val finalBannerUri = when {
                context != null && com.example.util.ImageStorageHelper.isBase64Image(rawBannerUri) -> {
                    com.example.util.ImageStorageHelper.saveBase64ImageFile(context, rawBannerUri, "banners") ?: rawBannerUri
                }
                else -> rawBannerUri
            }

            val rawGreetingBannerUri = data.optString("greeting_banner_uri", "").trim()
            val finalGreetingBannerUri = when {
                context != null && com.example.util.ImageStorageHelper.isBase64Image(rawGreetingBannerUri) -> {
                    com.example.util.ImageStorageHelper.saveBase64ImageFile(context, rawGreetingBannerUri, "banners") ?: rawGreetingBannerUri
                }
                else -> rawGreetingBannerUri
            }

            val rawFrameUri = data.optString("avatar_frame_base64", data.optString("avatar_frame", "")).trim()
            val finalFrameUri = when {
                rawFrameUri.isBlank() || rawFrameUri.equals("null", ignoreCase = true) -> ""
                context != null && com.example.util.ImageStorageHelper.isBase64Image(rawFrameUri) -> {
                    com.example.util.ImageStorageHelper.saveBase64ImageFile(context, rawFrameUri, "frames") ?: rawFrameUri
                }
                else -> rawFrameUri
            }

            val shortDesc = data.optString("short_description", data.optString("description", "")).trim()
            var personality = data.optString("personality", "").trim()
            val scenario = data.optString("scenario", "").trim()
            if (scenario.isNotBlank() && !personality.contains(scenario)) {
                personality = if (personality.isBlank()) scenario else "$personality\n\n[Сценарий]: $scenario"
            }

            val greeting = data.optString("greeting_message", data.optString("first_mes", "Привет!")).trim()

            val character = CharacterEntity(
                name = name,
                avatarUri = finalAvatarUri,
                shortDescription = shortDesc,
                appearance = data.optString("appearance", ""),
                personality = personality,
                greetingMessage = greeting.ifBlank { "Привет!" },
                habits = data.optString("habits", ""),
                likes = data.optString("likes", ""),
                dislikes = data.optString("dislikes", ""),
                age = age,
                birthDate = data.optString("birth_date", ""),
                knowledgeCards = knowledgeCardsStr,
                referenceImages = referenceImagesStr,
                tags = tagsStr,
                voiceStyle = data.optString("voice_style", "natural"),
                isAdult = isAdult,
                startingRelationship = data.optString("starting_relationship", "strangers"),
                customRelationship = data.optString("custom_relationship", ""),
                initialRelationshipTitle = data.optString("initial_relationship_title", ""),
                initialRelationshipPercent = data.optInt("initial_relationship_percent", -1),
                bannerUri = finalBannerUri,
                communicationFormality = data.optString("communication_formality", "adaptive"),
                avatarFrameUri = finalFrameUri,
                greetingBannerUri = finalGreetingBannerUri,
                author = author,
                relationshipBoardJson = when {
                    data.has("relationship_board") -> {
                        val b = data.get("relationship_board")
                        if (b is JSONObject) b.toString() else b.toString()
                    }
                    else -> data.optString("relationship_board_json", "")
                }
            )
            Result.success(character)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
