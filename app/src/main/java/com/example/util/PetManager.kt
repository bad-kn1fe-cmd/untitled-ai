package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class RemotePetItem(
    val id: String,
    val nameRu: String,
    val nameEn: String,
    val imageUrl: String
)

data class RemotePetCatalog(
    val ver: String = "1.0.0",
    val petsCount: Int = 5,
    val pets: List<RemotePetItem> = emptyList()
)

object PetManager {

    val PET_BOX_URLS = listOf(
        "https://raw.githubusercontent.com/bad-kn1fe-cmd/box-of-untitledAI/main/pets/pet-box.json",
        "https://github.com/bad-kn1fe-cmd/box-of-untitledAI/raw/main/pets/pet-box.json",
        "https://cdn.jsdelivr.net/gh/bad-kn1fe-cmd/box-of-untitledAI@main/pets/pet-box.json"
    )

    val DEFAULT_PETS = listOf(
        RemotePetItem("boykisser", "Бойкиссер", "Boykisser", "https://raw.githubusercontent.com/bad-kn1fe-cmd/box-of-untitledAI/main/pets/boykisser_classic.png"),
        RemotePetItem("pochita", "Почита", "Pochita", "https://raw.githubusercontent.com/bad-kn1fe-cmd/box-of-untitledAI/main/pets/pochitta_classic.png"),
        RemotePetItem("akira", "Акира", "Akira", "https://raw.githubusercontent.com/bad-kn1fe-cmd/box-of-untitledAI/main/pets/akira_classic.png"),
        RemotePetItem("mephone4", "MePhone4", "MePhone4", "https://raw.githubusercontent.com/bad-kn1fe-cmd/box-of-untitledAI/main/pets/mephone4_classic.png"),
        RemotePetItem("claude", "Клод", "Claude", "https://raw.githubusercontent.com/bad-kn1fe-cmd/box-of-untitledAI/main/pets/claude_classic.png")
    )

    private var cachedCatalog: RemotePetCatalog? = null

    fun getCachedPets(): List<RemotePetItem> {
        return cachedCatalog?.pets?.takeIf { it.isNotEmpty() } ?: DEFAULT_PETS
    }

    suspend fun fetchPetCatalog(): RemotePetCatalog = withContext(Dispatchers.IO) {
        cachedCatalog?.let { return@withContext it }

        for (targetUrl in PET_BOX_URLS) {
            var conn: HttpURLConnection? = null
            try {
                val url = URL("$targetUrl?_t=${System.currentTimeMillis()}")
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 6000
                conn.readTimeout = 6000
                conn.setRequestProperty("User-Agent", "UntitledAI-Android")

                if (conn.responseCode in 200..299) {
                    val rawText = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use {
                        it.readText()
                    }.trim()

                    val json = JSONObject(rawText)
                    val ver = json.optString("ver", "1.0.0")
                    val petsObj = json.optJSONObject("pets")

                    val items = mutableListOf<RemotePetItem>()
                    if (petsObj != null) {
                        val keys = petsObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val imageUrl = petsObj.optString(key, "").trim()
                            val (ru, en) = when (key.lowercase()) {
                                "akira" -> "Акира" to "Akira"
                                "mephone4" -> "MePhone4" to "MePhone4"
                                "claude" -> "Клод" to "Claude"
                                "pochita" -> "Почита" to "Pochita"
                                "boykisser" -> "Бойкиссер" to "Boykisser"
                                else -> key.replaceFirstChar { it.uppercase() } to key.replaceFirstChar { it.uppercase() }
                            }
                            if (imageUrl.isNotBlank()) {
                                items.add(RemotePetItem(id = key, nameRu = ru, nameEn = en, imageUrl = imageUrl))
                            }
                        }
                    }

                    if (items.isNotEmpty()) {
                        val catalog = RemotePetCatalog(ver = ver, petsCount = items.size, pets = items)
                        cachedCatalog = catalog
                        return@withContext catalog
                    }
                }
            } catch (_: Exception) {
            } finally {
                conn?.disconnect()
            }
        }

        // Fallback
        val fallback = RemotePetCatalog(pets = DEFAULT_PETS)
        cachedCatalog = fallback
        fallback
    }
}
