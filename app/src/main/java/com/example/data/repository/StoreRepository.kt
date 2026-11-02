package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.local.entity.CharacterEntity
import com.example.util.ImageStorageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class StoreInfo(
    val ver: String,
    val characterNumber: String
)

data class StoreBotItem(
    val name: String,
    val avatarUri: String,
    val bannerUri: String = "",
    val greetingBannerUri: String = "",
    val avatarFrameUri: String = "",
    val shortDescription: String,
    val appearance: String,
    val personality: String,
    val greetingMessage: String,
    val habits: String,
    val likes: String,
    val dislikes: String,
    val age: Int,
    val birthDate: String,
    val knowledgeCards: String,
    val referenceImages: String,
    val tags: List<String>,
    val voiceStyle: String,
    val isAdult: Boolean,
    val author: String,
    val rawJson: String
) {
    fun toCharacterEntity(context: Context? = null): CharacterEntity {
        // If avatar is base64, save to local disk to keep SQLite / CursorWindow fast and safe
        val finalAvatar = when {
            avatarUri.isBlank() || avatarUri.equals("null", ignoreCase = true) -> {
                "drawable://app_logo_black"
            }
            context != null && ImageStorageHelper.isBase64Image(avatarUri) -> {
                ImageStorageHelper.saveBase64ImageFile(context, avatarUri, "avatars") ?: avatarUri
            }
            else -> avatarUri
        }

        val finalBanner = when {
            context != null && bannerUri.isNotBlank() && ImageStorageHelper.isBase64Image(bannerUri) -> {
                ImageStorageHelper.saveBase64ImageFile(context, bannerUri, "banners") ?: bannerUri
            }
            else -> bannerUri
        }

        val finalFrame = when {
            context != null && avatarFrameUri.isNotBlank() && ImageStorageHelper.isBase64Image(avatarFrameUri) -> {
                ImageStorageHelper.saveBase64ImageFile(context, avatarFrameUri, "frames") ?: avatarFrameUri
            }
            else -> avatarFrameUri
        }

        // Also sanitize referenceImages if they contain huge base64 strings
        val finalRefImages = if (context != null && referenceImages.isNotBlank() && referenceImages != "[]") {
            try {
                val arr = JSONArray(referenceImages)
                val cleanArr = JSONArray()
                for (i in 0 until arr.length()) {
                    val item = arr.optString(i, "")
                    if (ImageStorageHelper.isBase64Image(item)) {
                        val savedFile = ImageStorageHelper.saveBase64ImageFile(context, item, "references")
                        cleanArr.put(savedFile ?: item)
                    } else if (item.isNotBlank()) {
                        cleanArr.put(item)
                    }
                }
                cleanArr.toString()
            } catch (_: Exception) {
                referenceImages
            }
        } else {
            referenceImages
        }

        val tagsArray = JSONArray()
        tags.forEach { tagsArray.put(it) }

        val finalGreetingBanner = when {
            context != null && greetingBannerUri.isNotBlank() && ImageStorageHelper.isBase64Image(greetingBannerUri) -> {
                ImageStorageHelper.saveBase64ImageFile(context, greetingBannerUri, "banners") ?: greetingBannerUri
            }
            else -> greetingBannerUri
        }

        return CharacterEntity(
            name = name,
            avatarUri = finalAvatar,
            bannerUri = finalBanner,
            greetingBannerUri = finalGreetingBanner,
            avatarFrameUri = finalFrame,
            shortDescription = shortDescription,
            personality = personality,
            appearance = appearance,
            greetingMessage = greetingMessage.ifBlank { "Привет!" },
            habits = habits,
            likes = likes,
            dislikes = dislikes,
            age = age,
            birthDate = birthDate,
            knowledgeCards = knowledgeCards,
            referenceImages = finalRefImages,
            tags = tagsArray.toString(),
            voiceStyle = voiceStyle,
            isAdult = isAdult,
            author = author.ifBlank { "UntitledAI" }
        )
    }
}

data class StoreCatalog(
    val info: StoreInfo,
    val characters: List<StoreBotItem>
)

class StoreRepository(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private fun getFreshUrls(): List<String> {
        val ts = System.currentTimeMillis()
        return listOf(
            "https://raw.githubusercontent.com/bad-kn1fe-cmd/box-of-untitledAI/main/box.json?nocache=$ts",
            "https://raw.githubusercontent.com/bad-kn1fe-cmd/box-of-untitledAI/master/box.json?nocache=$ts",
            "https://cdn.jsdelivr.net/gh/bad-kn1fe-cmd/box-of-untitledAI@main/box.json",
            "https://github.com/bad-kn1fe-cmd/box-of-untitledAI/raw/main/box.json"
        )
    }

    fun isOnline(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }

    suspend fun checkRepositoryConnection(): Boolean = withContext(Dispatchers.IO) {
        if (!isOnline()) return@withContext false
        val probeClient = httpClient.newBuilder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .build()
        val probeUrls = listOf(
            "https://raw.githubusercontent.com/bad-kn1fe-cmd/box-of-untitledAI/main/box.json",
            "https://cdn.jsdelivr.net/gh/bad-kn1fe-cmd/box-of-untitledAI@main/box.json",
            "https://github.com/bad-kn1fe-cmd/box-of-untitledAI"
        )
        for (url in probeUrls) {
            try {
                val req = Request.Builder()
                    .url(url)
                    .head()
                    .header("User-Agent", "UntitledAI-Store/1.0")
                    .build()
                val ok = probeClient.newCall(req).execute().use { it.isSuccessful || it.code in 200..399 }
                if (ok) return@withContext true
            } catch (_: Exception) {
                try {
                    val getReq = Request.Builder()
                        .url(url)
                        .header("User-Agent", "UntitledAI-Store/1.0")
                        .build()
                    val ok = probeClient.newCall(getReq).execute().use { it.isSuccessful }
                    if (ok) return@withContext true
                } catch (_: Exception) {}
            }
        }
        false
    }

    suspend fun fetchCatalog(): Result<StoreCatalog> = withContext(Dispatchers.IO) {
        var jsonString: String? = null
        var lastError: Exception? = null

        val urls = getFreshUrls()
        for (url in urls) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "UntitledAI-Store/1.0")
                    .header("Accept", "application/json")
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .header("Pragma", "no-cache")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            jsonString = body
                            return@use
                        }
                    } else {
                        lastError = IOException("HTTP ${response.code}: ${response.message}")
                    }
                }
                if (jsonString != null) break
            } catch (e: Exception) {
                lastError = e
            }
        }

        if (jsonString == null) {
            return@withContext Result.failure(lastError ?: IOException("Не удалось загрузить каталог ботов"))
        }

        try {
            val catalog = parseCatalog(jsonString!!)
            Result.success(catalog)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun sanitizeJson(raw: String): String {
        var clean = raw.trim()
        // Strip UTF-8 BOM if present
        if (clean.startsWith("\uFEFF")) {
            clean = clean.substring(1)
        }

        // 1. Fix missing values where key is followed by newline and next key: "avatar_uri": \n "short_description"
        clean = clean.replace(Regex("\"([a-zA-Z0-9_]+)\"\\s*:\\s*(?=[\\r\\n]+\\s*\"[a-zA-Z0-9_]+\")"), "\"$1\": \"\",")

        // 2. Fix missing value where key has no value before newline: "avatar_uri": \n
        clean = clean.replace(Regex("\"([a-zA-Z0-9_]+)\"\\s*:\\s*(?=[\\r\\n]+)"), "\"$1\": \"\",")

        // 3. Fix unclosed card boundary: e.g. "by": "UntitledAI" }, \n { "spec": "untitled_ai_character_card"
        // where "data": { ... } was closed with only one } before the next card in characters_list
        clean = clean.replace(Regex("(\\}\\s*,\\s*[\\r\\n]+\\s*\\{\\s*\"spec\"\\s*:\\s*\"untitled_ai_character_card\")"), "}$1")

        // 4. Fix extra closing braces at the end if someone added triple braces: }}} ]} -> }} ]}
        clean = clean.replace(Regex("\\}\\s*\\}\\s*\\}\\s*\\]\\s*\\}"), "}} ]}")

        // 5. Strip trailing commas before } or ]
        clean = clean.replace(Regex(",\\s*([}\\]])"), "$1")

        return clean
    }

    private fun parseCatalog(rawJson: String): StoreCatalog {
        val cleanJson = sanitizeJson(rawJson)
        try {
            val root = JSONObject(cleanJson)

            val infoObj = root.optJSONObject("info")
            val ver = infoObj?.optString("ver", "1.0.0") ?: "1.0.0"
            val characterNumber = infoObj?.optString("character_number", "") ?: ""

            val listArray = root.optJSONArray("characters_list") ?: JSONArray()
            val items = mutableListOf<StoreBotItem>()

            for (i in 0 until listArray.length()) {
                val cardObj = listArray.optJSONObject(i) ?: continue
                val dataObj = if (cardObj.has("data")) cardObj.optJSONObject("data") else cardObj
                if (dataObj == null) continue

                val item = extractStoreBotItem(dataObj, cardObj.toString())
                if (item != null) {
                    items.add(item)
                }
            }

            val finalNumber = if (characterNumber.isNotBlank()) characterNumber else items.size.toString()
            val info = StoreInfo(ver = ver, characterNumber = finalNumber)
            return StoreCatalog(info = info, characters = items)
        } catch (_: Exception) {
            // Fallback: robust regex parsing if JSONObject throws on edge cases
            return parseCatalogFallback(rawJson)
        }
    }

    private fun extractStoreBotItem(dataObj: JSONObject, rawJson: String): StoreBotItem? {
        val name = dataObj.optString("name", "").trim()
        if (name.isBlank()) return null

        val avatarUri = dataObj.optString("avatar_uri", "").trim()
        val shortDesc = dataObj.optString("short_description", "").trim()
        val appearance = dataObj.optString("appearance", "").trim()
        val personality = dataObj.optString("personality", "").trim()
        val greeting = dataObj.optString("greeting_message", "").trim()
        val habits = dataObj.optString("habits", "").trim()
        val likes = dataObj.optString("likes", "").trim()
        val dislikes = dataObj.optString("dislikes", "").trim()
        val age = dataObj.optInt("age", 18)
        val birthDate = dataObj.optString("birth_date", "").trim()

        val knowledgeCardsStr = when {
            dataObj.has("knowledge_cards") -> {
                val v = dataObj.get("knowledge_cards")
                if (v is JSONArray) v.toString() else v.toString()
            }
            else -> "[]"
        }

        val referenceImagesStr = when {
            dataObj.has("reference_images") -> {
                val v = dataObj.get("reference_images")
                if (v is JSONArray) v.toString() else v.toString()
            }
            else -> "[]"
        }

        val tagsList = mutableListOf<String>()
        if (dataObj.has("tags")) {
            val tagsArr = dataObj.optJSONArray("tags")
            if (tagsArr != null) {
                for (t in 0 until tagsArr.length()) {
                    val tag = tagsArr.optString(t, "").trim()
                    if (tag.isNotBlank()) tagsList.add(tag)
                }
            }
        }

        val voiceStyle = dataObj.optString("voice_style", "natural")
        val isAdult = if (dataObj.has("is_adult")) dataObj.optBoolean("is_adult", age >= 18) else (age >= 18)
        val author = dataObj.optString("by", dataObj.optString("author", "UntitledAI")).trim().ifBlank { "UntitledAI" }
        val bannerUri = dataObj.optString("banner_uri", dataObj.optString("banner_url", dataObj.optString("banner", ""))).trim()
        val greetingBannerUri = dataObj.optString("greeting_banner_uri", dataObj.optString("greeting_banner_url", dataObj.optString("greeting_banner", ""))).trim()
        val avatarFrameUri = dataObj.optString("avatar_frame_base64", dataObj.optString("avatar_frame", "")).trim()

        return StoreBotItem(
            name = name,
            avatarUri = avatarUri,
            bannerUri = bannerUri,
            greetingBannerUri = greetingBannerUri,
            avatarFrameUri = avatarFrameUri,
            shortDescription = shortDesc,
            appearance = appearance,
            personality = personality,
            greetingMessage = greeting,
            habits = habits,
            likes = likes,
            dislikes = dislikes,
            age = age,
            birthDate = birthDate,
            knowledgeCards = knowledgeCardsStr,
            referenceImages = referenceImagesStr,
            tags = tagsList,
            voiceStyle = voiceStyle,
            isAdult = isAdult,
            author = author,
            rawJson = rawJson
        )
    }

    private fun parseCatalogFallback(rawJson: String): StoreCatalog {
        val items = mutableListOf<StoreBotItem>()
        val clean = sanitizeJson(rawJson)

        // Try extracting character cards blocks by "spec" or "name"
        val cardPattern = Pattern.compile("\\{[^{}]*?\"spec\"\\s*:\\s*\"untitled_ai_character_card\".*?\\}\\s*\\}", Pattern.DOTALL)
        val matcher = cardPattern.matcher(clean)

        while (matcher.find()) {
            val cardText = matcher.group()
            try {
                val cardObj = JSONObject(cardText)
                val dataObj = if (cardObj.has("data")) cardObj.optJSONObject("data") else cardObj
                if (dataObj != null) {
                    val item = extractStoreBotItem(dataObj, cardText)
                    if (item != null) items.add(item)
                }
            } catch (_: Exception) {}
        }

        return StoreCatalog(
            info = StoreInfo(ver = "1.0.0", characterNumber = items.size.toString()),
            characters = items
        )
    }
}
