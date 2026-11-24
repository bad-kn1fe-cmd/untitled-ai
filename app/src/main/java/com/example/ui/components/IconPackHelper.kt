package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage

val LocalIconPack = compositionLocalOf { EmojiPackManager.PACK_VECTOR }

const val ICON_PACK_VECTOR = EmojiPackManager.PACK_VECTOR
const val ICON_PACK_SYSTEM = EmojiPackManager.PACK_SYSTEM
const val ICON_PACK_TWEMOJI = EmojiPackManager.PACK_TWEMOJI
const val ICON_PACK_NOTO = EmojiPackManager.PACK_NOTO
const val ICON_PACK_BLOB = EmojiPackManager.PACK_BLOB
const val ICON_PACK_OPENMOJI = EmojiPackManager.PACK_OPENMOJI

/**
 * Преобразует эмодзи в hex-код для Twemoji CDN (https://cdn.jsdelivr.net/gh/twitter/twemoji@14.0.2/assets/72x72/{code}.png)
 */
fun emojiToTwemojiCode(emoji: String): String {
    val list = mutableListOf<String>()
    var i = 0
    while (i < emoji.length) {
        val cp = emoji.codePointAt(i)
        val hex = Integer.toHexString(cp).lowercase()
        // Twemoji удаляет селектор вариации fe0f для большинства одиночных эмодзи
        if (hex != "fe0f") {
            list.add(hex)
        }
        i += Character.charCount(cp)
    }
    return list.joinToString("-")
}

/**
 * Возвращает векторную иконку Material Icons для системного эмодзи или null, если соответствия нет
 */
fun getVectorForEmojiOrNull(emoji: String): ImageVector? {
    val clean = emoji.trim()
    return when {
        clean.contains("💬") -> Icons.Default.ChatBubble
        clean.contains("🤖") -> Icons.Default.SmartToy
        clean.contains("🎭") -> Icons.Default.TheaterComedy
        clean.contains("🎨") -> Icons.Default.Palette
        clean.contains("⚙") -> Icons.Default.Settings
        clean.contains("🌐") -> Icons.Default.Language
        clean.contains("🔒") -> Icons.Default.Lock
        clean.contains("💾") -> Icons.Default.Save
        clean.contains("🧠") -> Icons.Default.Psychology
        clean.contains("⚡") -> Icons.Default.Bolt
        clean.contains("📌") -> Icons.Default.PushPin
        clean.contains("📁") -> Icons.Default.Folder
        clean.contains("📦") -> Icons.Default.Archive
        clean.contains("🐾") || clean.contains("🐱") || clean.contains("🐶") -> Icons.Default.Pets
        clean.contains("🚀") -> Icons.Default.RocketLaunch
        clean.contains("💡") -> Icons.Default.Lightbulb
        clean.contains("✏") -> Icons.Default.Edit
        clean.contains("🗑") -> Icons.Default.Delete
        clean.contains("📊") -> Icons.Default.BarChart
        clean.contains("🕸") -> Icons.Default.Hub
        clean.contains("🕒") -> Icons.Default.Schedule
        clean.contains("⏳") -> Icons.Default.HourglassEmpty
        clean.contains("🔤") -> Icons.Default.SortByAlpha
        clean.contains("📅") -> Icons.Default.CalendarToday
        clean.contains("🎂") -> Icons.Default.Cake
        clean.contains("✍") -> Icons.Default.Draw
        clean.contains("💚") -> Icons.Default.Favorite
        clean.contains("💔") -> Icons.Default.HeartBroken
        clean.contains("👋") -> Icons.Default.WavingHand
        clean.contains("📚") -> Icons.Default.MenuBook
        clean.contains("📸") -> Icons.Default.CameraAlt
        clean.contains("🗣") -> Icons.Default.RecordVoiceOver
        clean.contains("🎓") -> Icons.Default.School
        clean.contains("🧪") -> Icons.Default.Science
        clean.contains("🛠") -> Icons.Default.Build
        clean.contains("🔌") -> Icons.Default.Power
        clean.contains("🏠") -> Icons.Default.Home
        clean.contains("🎉") -> Icons.Default.Celebration
        clean.contains("🌊") -> Icons.Default.Waves
        clean.contains("🎯") -> Icons.Default.TrackChanges
        clean.contains("🎴") -> Icons.Default.Layers
        clean.contains("🧊") -> Icons.Default.ViewInAr
        clean.contains("🌫") -> Icons.Default.BlurOn
        clean.contains("👁") -> Icons.Default.Visibility
        clean.contains("📷") -> Icons.Default.PhotoCamera
        clean.contains("🎬") -> Icons.Default.Movie
        clean.contains("📎") -> Icons.Default.AttachFile
        clean.contains("🎙") -> Icons.Default.Mic
        clean.contains("✨") -> Icons.Default.AutoAwesome
        clean.contains("⭐") -> Icons.Default.Star
        clean.contains("✂") -> Icons.Default.ContentCut
        clean.contains("➕") -> Icons.Default.Add
        clean.contains("🔍") -> Icons.Default.Search
        clean.contains("⚠️") || clean.contains("⚠") -> Icons.Default.Warning
        clean.contains("📢") -> Icons.Default.Campaign
        clean.contains("⌨") -> Icons.Default.Keyboard
        clean.contains("📖") -> Icons.Default.AutoStories
        clean.contains("🖼") -> Icons.Default.Image
        clean.contains("🎃") -> Icons.Default.SportsEsports
        clean.contains("🧘") -> Icons.Default.SelfImprovement
        clean.contains("🍪") -> Icons.Default.Cookie
        clean.contains("☀️") || clean.contains("☀") -> Icons.Default.WbSunny
        clean.contains("🌸") -> Icons.Default.Spa
        clean.contains("🍀") -> Icons.Default.Yard
        clean.contains("⚪") -> Icons.Default.Circle
        clean.contains("➡️") || clean.contains("➡") -> Icons.AutoMirrored.Filled.ArrowForward
        clean.contains("✅") -> Icons.Default.CheckCircle
        clean.contains("✓") -> Icons.Default.Check
        clean.contains("🚫") -> Icons.Default.Block
        clean.contains("🚽") -> Icons.Default.Wc
        clean.contains("😊") || clean.contains("🙂") -> Icons.Default.Mood
        clean.contains("💖") -> Icons.Default.Favorite
        clean.contains("👤") -> Icons.Default.Person
        clean.contains("👥") -> Icons.Default.Groups
        clean.contains("🏷") -> Icons.Default.Label
        clean.contains("🛡") -> Icons.Default.Shield
        clean.contains("🍔") -> Icons.Default.Fastfood
        clean.contains("♂") -> Icons.Default.Male
        clean.contains("♀") -> Icons.Default.Female
        clean.contains("🍲") || clean.contains("🍕") || clean.contains("🍟") -> Icons.Default.Restaurant
        clean.contains("📹") -> Icons.Default.Videocam
        clean.contains("📥") -> Icons.Default.Inbox
        clean.contains("💪") || clean.contains("🏋") -> Icons.Default.FitnessCenter
        clean.contains("🍩") -> Icons.Default.DonutLarge
        clean.contains("🔥") -> Icons.Default.LocalFireDepartment
        clean.contains("📄") -> Icons.Default.Description
        clean.contains("🌆") -> Icons.Default.LocationCity
        clean.contains("☕") -> Icons.Default.Coffee
        clean.contains("🔮") -> Icons.Default.AutoFixHigh
        clean.contains("❌") -> Icons.Default.Close
        clean.contains("⚔") -> Icons.Default.Security
        clean.contains("🛋") -> Icons.Default.Weekend
        clean.contains("🚪") -> Icons.Default.DoorFront
        clean.contains("🖥") -> Icons.Default.Computer
        clean.contains("📱") -> Icons.Default.Smartphone
        clean.contains("🔄") -> Icons.Default.Refresh
        clean.contains("📜") -> Icons.Default.HistoryEdu
        clean.contains("📍") -> Icons.Default.Place
        clean.contains("💥") -> Icons.Default.FlashOn
        clean.contains("👗") -> Icons.Default.Checkroom
        clean.contains("👍") -> Icons.Default.ThumbUp
        clean.contains("👎") -> Icons.Default.ThumbDown
        clean.contains("🛑") -> Icons.Default.Stop
        clean.contains("❤") || clean.contains("❤️") -> Icons.Default.Favorite
        else -> null
    }
}

/**
 * Возвращает векторную иконку Material Icons для системного эмодзи
 */
fun getVectorForEmoji(emoji: String): ImageVector {
    return getVectorForEmojiOrNull(emoji) ?: Icons.Default.Star
}

/**
 * Адаптивная иконка, поддерживающая 3 стиля:
 * 1) "vector" - Material Design векторная иконка
 * 2) "system" - Системный эмодзи устройства
 * 3) "twemoji" - Эмодзи Twemoji (стиль Twitter/Discord)
 */
@Composable
fun AdaptiveUiIcon(
    emoji: String,
    modifier: Modifier = Modifier,
    overrideVector: ImageVector? = null,
    contentDescription: String? = null,
    tint: Color = LocalContentColor.current,
    size: Dp = 18.dp
) {
    val pack = LocalIconPack.current
    when (pack) {
        ICON_PACK_SYSTEM -> {
            Text(
                text = emoji,
                fontSize = (size.value * 0.9f).sp,
                modifier = modifier
            )
        }
        ICON_PACK_VECTOR -> {
            val vec = overrideVector ?: getVectorForEmojiOrNull(emoji)
            if (vec != null) {
                Icon(
                    imageVector = vec,
                    contentDescription = contentDescription,
                    tint = tint,
                    modifier = modifier.size(size)
                )
            } else {
                Text(
                    text = emoji,
                    fontSize = (size.value * 0.85f).sp,
                    modifier = modifier
                )
            }
        }
        else -> { // "twemoji", "noto", "blob", "openmoji"
            val context = androidx.compose.ui.platform.LocalContext.current
            val localFile = remember(pack, emoji) {
                EmojiPackManager.getLocalEmojiFile(context, pack, emoji)
            }
            val model: Any? = if (localFile != null && localFile.exists()) {
                localFile
            } else {
                remember(pack, emoji) { EmojiPackManager.getEmojiUrl(pack, emoji) }
            }

            if (model != null) {
                SubcomposeAsyncImage(
                    model = model,
                    contentDescription = contentDescription,
                    modifier = modifier.size(size),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    loading = {
                        if (overrideVector != null) {
                            Icon(imageVector = overrideVector, contentDescription = null, tint = tint, modifier = Modifier.size(size))
                        } else {
                            Text(text = emoji, fontSize = (size.value * 0.85f).sp, modifier = Modifier.size(size))
                        }
                    },
                    error = {
                        if (overrideVector != null) {
                            Icon(imageVector = overrideVector, contentDescription = null, tint = tint, modifier = Modifier.size(size))
                        } else {
                            Text(text = emoji, fontSize = (size.value * 0.85f).sp, modifier = Modifier.size(size))
                        }
                    }
                )
            } else {
                val vec = overrideVector ?: getVectorForEmojiOrNull(emoji)
                if (vec != null) {
                    Icon(imageVector = vec, contentDescription = contentDescription, tint = tint, modifier = modifier.size(size))
                } else {
                    Text(text = emoji, fontSize = (size.value * 0.85f).sp, modifier = modifier)
                }
            }
        }
    }
}

/**
 * Строка с адаптивной иконкой слева и текстом справа
 */
@Composable
fun AdaptiveIconText(
    emoji: String,
    text: String,
    modifier: Modifier = Modifier,
    overrideVector: ImageVector? = null,
    tint: Color = LocalContentColor.current,
    iconSize: Dp = 18.dp,
    spacing: Dp = 8.dp,
    textStyle: TextStyle = LocalTextStyle.current,
    textColor: Color = Color.Unspecified,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    fontWeight: androidx.compose.ui.text.font.FontWeight? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: androidx.compose.ui.text.style.TextOverflow = androidx.compose.ui.text.style.TextOverflow.Clip
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        AdaptiveUiIcon(
            emoji = emoji,
            overrideVector = overrideVector,
            tint = tint,
            size = iconSize
        )
        Text(
            text = text,
            style = textStyle,
            color = textColor,
            fontSize = fontSize,
            fontWeight = fontWeight,
            maxLines = maxLines,
            overflow = overflow
        )
    }
}

private val LEADING_EMOJI_REGEX = Regex(
    "^((?:[\\uD83C-\\uD83E][\\uDC00-\\uDFFF]|[\\u2300-\\u23FF]|[\\u2600-\\u27BF]|[\\u2B50\\u2190-\\u21FF\\uFE0E-\\uFE0F\\u200D])+)\\s*(.*)$"
)

fun extractLeadingEmoji(text: String): Pair<String, String>? {
    val trimmed = text.trim()
    val match = LEADING_EMOJI_REGEX.find(trimmed) ?: return null
    val emoji = match.groupValues[1].trim()
    val remainder = match.groupValues[2].trim()
    if (emoji.isEmpty() || remainder.isEmpty()) return null
    return emoji to remainder
}

/**
 * Очищает ведущие эмодзи из строки, если отображается векторная иконка
 */
fun stripLeadingEmojis(text: String): String {
    val trimmed = text.trim()
    val match = LEADING_EMOJI_REGEX.find(trimmed) ?: return trimmed
    return match.groupValues[2].trim()
}

/**
 * Умная строка: если в начале текста есть эмодзи, отображает адаптивную иконку (Вектор / Твемодзи / Системный)
 * и текст рядом. Если эмодзи нет, выводит обычный Text.
 */
@Composable
fun AdaptiveIconText(
    text: String,
    modifier: Modifier = Modifier,
    overrideVector: ImageVector? = null,
    tint: Color = LocalContentColor.current,
    iconSize: Dp = 18.dp,
    spacing: Dp = 8.dp,
    textStyle: TextStyle = LocalTextStyle.current,
    textColor: Color = Color.Unspecified,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    fontWeight: androidx.compose.ui.text.font.FontWeight? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: androidx.compose.ui.text.style.TextOverflow = androidx.compose.ui.text.style.TextOverflow.Clip
) {
    val pack = LocalIconPack.current
    val parsed = remember(text) { extractLeadingEmoji(text) }

    if (parsed == null || pack == ICON_PACK_SYSTEM) {
        Text(
            text = text,
            modifier = modifier,
            style = textStyle,
            color = textColor,
            fontSize = fontSize,
            fontWeight = fontWeight,
            maxLines = maxLines,
            overflow = overflow
        )
    } else {
        AdaptiveIconText(
            emoji = parsed.first,
            text = parsed.second,
            modifier = modifier,
            overrideVector = overrideVector,
            tint = tint,
            iconSize = iconSize,
            spacing = spacing,
            textStyle = textStyle,
            textColor = textColor,
            fontSize = fontSize,
            fontWeight = fontWeight,
            maxLines = maxLines,
            overflow = overflow
        )
    }
}

