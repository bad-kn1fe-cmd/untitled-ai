package com.example.ui.components

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object EmojiPackManager {

    const val PACK_VECTOR = "vector"
    const val PACK_SYSTEM = "system"
    const val PACK_TWEMOJI = "twemoji"
    const val PACK_NOTO = "noto"
    const val PACK_BLOB = "blob"
    const val PACK_OPENMOJI = "openmoji"

    data class PackInfo(
        val id: String,
        val nameRu: String,
        val nameEn: String,
        val descRu: String,
        val descEn: String,
        val supportsDownload: Boolean = true
    )

    val AVAILABLE_PACKS = listOf(
        PackInfo(
            id = PACK_VECTOR,
            nameRu = "Вектор (Material Icons)",
            nameEn = "Vector (Material Icons)",
            descRu = "Современные векторные иконки, адаптирующиеся под цвет темы (Рекомендуется)",
            descEn = "Modern vector icons adapting to theme color (Recommended)",
            supportsDownload = false
        ),
        PackInfo(
            id = PACK_SYSTEM,
            nameRu = "Системные",
            nameEn = "System Emoji",
            descRu = "Стандартные системные смайлики устройства",
            descEn = "Native device emojis",
            supportsDownload = false
        ),
        PackInfo(
            id = PACK_TWEMOJI,
            nameRu = "Twemoji (Discord, X)",
            nameEn = "Twemoji (Discord, X)",
            descRu = "Культовые плоские эмодзи в стиле Discord и Twitter/X",
            descEn = "Iconic flat emoji style used in Discord and Twitter/X",
            supportsDownload = true
        ),
        PackInfo(
            id = PACK_NOTO,
            nameRu = "Noto (Google, Android)",
            nameEn = "Noto (Google, Android)",
            descRu = "Оригинальные красочные эмодзи чистого Google Android",
            descEn = "Original colorful emojis from stock Google Android",
            supportsDownload = true
        ),
        PackInfo(
            id = PACK_BLOB,
            nameRu = "Blob (Android 7)",
            nameEn = "Blob (Android 7)",
            descRu = "Легендарные ретро-капельки Blob из Android Nougat",
            descEn = "Legendary Blob emojis from Android Nougat",
            supportsDownload = true
        ),
        PackInfo(
            id = PACK_OPENMOJI,
            nameRu = "OpenMoji",
            nameEn = "OpenMoji",
            descRu = "Открытый дизайнерский проект эмодзи с четкими контурами",
            descEn = "Open-source designer emoji set with crisp outlines",
            supportsDownload = true
        )
    )

    // Ключевые эмодзи приложения (интерфейс, диалоги, реакции, профили)
    val APP_EMOJIS = listOf(
        "💬", "🤖", "🎭", "🎨", "⚙️", "⚙", "🌐", "🔒", "💾", "🧠", "⚡",
        "📌", "📁", "📦", "🐾", "🐱", "🐶", "🚀", "💡", "✏️", "✏", "🗑️", "🗑",
        "📊", "🕸", "🕒", "⏳", "🔤", "📅", "🎂", "✍️", "✍", "💚", "💔",
        "👋", "📚", "📸", "🗣", "🎓", "🧪", "🛠️", "🛠", "🔌", "🏠", "🎉",
        "🌊", "🎯", "🎴", "🧊", "🌫", "👁", "📷", "🎬", "📎", "🎙", "✨",
        "⭐", "✂️", "✂", "➕", "🔍", "⚠️", "⚠", "📢", "⌨", "📖", "🖼",
        "🎃", "🧘", "🍪", "☀️", "☀", "🌸", "🍀", "⚪", "➡️", "➡", "✅",
        "✓", "🚫", "🚽", "😊", "🙂", "💖", "👤", "👥", "🏷", "🛡", "🍔",
        "♂", "♀", "🍲", "🍕", "🍟", "📹", "📥", "💪", "🏋", "🍩", "🔥",
        "📄", "📝", "🌆", "☕", "🔮", "❌", "⚔", "🛋", "🚪", "🖥", "📱", "🔄",
        "📜", "📍", "💥", "👗", "👍", "👎", "🛑", "❤️", "❤", "😂", "🥰",
        "🥺", "😭", "😎", "🤔", "👀", "💀", "👏",
        "😍", "😘", "😜", "😏", "🤤", "😳", "😱", "😈", "😇", "🤝",
        "💋", "🌹", "🍷", "🍺", "🛌", "🔪", "🔫", "💰", "👑", "💤",
        "💢", "❗", "❓", "👉", "👈", "👆", "👇", "🤷", "🤦", "💃",
        "🕺", "🎵", "🎶", "😴", "😋", "😉", "😆", "😅", "😌", "🙄"
    ).distinct()

    fun emojiToCode(emoji: String, packId: String): String {
        val list = mutableListOf<String>()
        var i = 0
        while (i < emoji.length) {
            val cp = emoji.codePointAt(i)
            val hex = Integer.toHexString(cp).lowercase()
            if (hex != "fe0f" && hex != "fe0e") {
                list.add(hex)
            }
            i += Character.charCount(cp)
        }
        val joined = list.joinToString("-")
        return when (packId) {
            PACK_OPENMOJI -> joined.uppercase()
            else -> joined
        }
    }

    fun emojiToCodeWithFe0f(emoji: String, packId: String): String {
        val list = mutableListOf<String>()
        var i = 0
        while (i < emoji.length) {
            val cp = emoji.codePointAt(i)
            val hex = Integer.toHexString(cp).lowercase()
            list.add(hex)
            i += Character.charCount(cp)
        }
        val joined = list.joinToString("-")
        return when (packId) {
            PACK_OPENMOJI -> joined.uppercase()
            else -> joined
        }
    }

    fun getCandidateUrls(packId: String, emoji: String): List<String> {
        val cleanCode = emojiToCode(emoji, packId)
        val fullCode = emojiToCodeWithFe0f(emoji, packId)
        val cleanUnder = cleanCode.replace("-", "_")
        val fullUnder = fullCode.replace("-", "_")

        return when (packId) {
            PACK_TWEMOJI -> listOf(
                "https://cdn.jsdelivr.net/gh/twitter/twemoji@14.0.2/assets/72x72/$cleanCode.png",
                "https://cdn.jsdelivr.net/gh/twitter/twemoji@14.0.2/assets/72x72/$fullCode.png"
            )
            PACK_NOTO -> listOf(
                "https://fonts.gstatic.com/s/e/notoemoji/latest/$cleanCode/128.png",
                "https://fonts.gstatic.com/s/e/notoemoji/latest/$cleanUnder/128.png",
                "https://fonts.gstatic.com/s/e/notoemoji/latest/$fullUnder/128.png"
            )
            PACK_BLOB -> listOf(
                "https://cdn.jsdelivr.net/gh/realityripple/emoji/blob/$cleanCode.png",
                "https://cdn.jsdelivr.net/gh/realityripple/emoji/blob/$fullCode.png"
            )
            PACK_OPENMOJI -> listOf(
                "https://cdn.jsdelivr.net/gh/hfg-gmuend/openmoji@14.0.0/color/72x72/${cleanCode.uppercase()}.png",
                "https://cdn.jsdelivr.net/gh/hfg-gmuend/openmoji@14.0.0/color/72x72/${fullCode.uppercase()}.png"
            )
            else -> emptyList()
        }
    }

    fun getEmojiUrl(packId: String, emoji: String): String? {
        return getCandidateUrls(packId, emoji).firstOrNull()
    }

    fun getPackDir(context: Context, packId: String): File {
        val dir = File(context.filesDir, "emoji_packs/$packId")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getLocalEmojiFile(context: Context, packId: String, emoji: String): File? {
        if (packId == PACK_VECTOR || packId == PACK_SYSTEM) return null
        val code = emojiToCode(emoji, packId).lowercase()
        val dir = File(context.filesDir, "emoji_packs/$packId")
        if (!dir.exists()) return null

        val f1 = File(dir, "$code.png")
        if (f1.exists() && f1.length() > 0) return f1

        val fullCode = emojiToCodeWithFe0f(emoji, packId).lowercase()
        val f2 = File(dir, "$fullCode.png")
        if (f2.exists() && f2.length() > 0) return f2

        return null
    }

    fun isPackDownloaded(context: Context, packId: String): Boolean {
        if (packId == PACK_VECTOR || packId == PACK_SYSTEM) return true
        val dir = File(context.filesDir, "emoji_packs/$packId")
        if (!dir.exists()) return false
        if (File(dir, ".download_complete").exists()) return true
        val count = dir.listFiles()?.count { it.isFile && it.length() > 0 && it.name.endsWith(".png") } ?: 0
        return count >= 15
    }

    fun getDownloadedCount(context: Context, packId: String): Int {
        val dir = File(context.filesDir, "emoji_packs/$packId")
        if (!dir.exists()) return 0
        return dir.listFiles()?.count { it.isFile && it.length() > 0 && it.name.endsWith(".png") } ?: 0
    }

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    private val activeDownloads = mutableSetOf<String>()

    fun downloadPack(context: Context, packId: String, force: Boolean = false, onFinished: (() -> Unit)? = null) {
        if (packId == PACK_VECTOR || packId == PACK_SYSTEM) {
            onFinished?.invoke()
            return
        }
        if (!force && isPackDownloaded(context, packId)) {
            onFinished?.invoke()
            return
        }
        synchronized(activeDownloads) {
            if (activeDownloads.contains(packId)) return
            activeDownloads.add(packId)
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val packDir = getPackDir(context, packId)
                val total = APP_EMOJIS.size

                APP_EMOJIS.forEachIndexed { index, emoji ->
                    val cleanCode = emojiToCode(emoji, packId).lowercase()
                    val targetFile = File(packDir, "$cleanCode.png")

                    if (!targetFile.exists() || targetFile.length() == 0L) {
                        val urls = getCandidateUrls(packId, emoji)
                        for (targetUrl in urls) {
                            var conn: HttpURLConnection? = null
                            try {
                                val url = URL(targetUrl)
                                conn = url.openConnection() as HttpURLConnection
                                conn.connectTimeout = 4000
                                conn.readTimeout = 4000
                                conn.setRequestProperty("User-Agent", "UntitledAI-Android")
                                if (conn.responseCode in 200..299) {
                                    val tempFile = File(packDir, "${targetFile.name}.tmp")
                                    conn.inputStream.use { input ->
                                        FileOutputStream(tempFile).use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                    if (tempFile.length() > 0) {
                                        tempFile.renameTo(targetFile)
                                        break
                                    } else {
                                        tempFile.delete()
                                    }
                                }
                            } catch (_: Exception) {
                            } finally {
                                conn?.disconnect()
                            }
                        }
                    }

                    val progress = (index + 1).toFloat() / total
                    _downloadProgress.value = _downloadProgress.value.toMutableMap().apply {
                        put(packId, progress)
                    }
                }
                try {
                    File(packDir, ".download_complete").createNewFile()
                } catch (_: Exception) {}
            } catch (_: Exception) {
            } finally {
                synchronized(activeDownloads) {
                    activeDownloads.remove(packId)
                }
                _downloadProgress.value = _downloadProgress.value.toMutableMap().apply {
                    remove(packId)
                }
                onFinished?.invoke()
            }
        }
    }
}
