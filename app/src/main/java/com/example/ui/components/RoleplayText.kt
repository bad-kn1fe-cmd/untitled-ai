package com.example.ui.components

import androidx.collection.LruCache
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.engine.RoleplayMarkdownParser

data class ParsedRoleplayText(
    val annotated: AnnotatedString,
    val emojis: Set<String>
)

private val annotatedMarkdownCache = LruCache<String, ParsedRoleplayText>(450)

@Composable
fun RoleplayText(
    text: String,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    baseFontSizeSp: Int = 15,
    isRoleplayFormatting: Boolean = true,
    textAlign: androidx.compose.ui.text.style.TextAlign = androidx.compose.ui.text.style.TextAlign.Unspecified
) {
    val pack = LocalIconPack.current

    val parsed = remember(text, accentColor, textColor, baseFontSizeSp, isRoleplayFormatting, pack) {
        val cacheKey = "${text.hashCode()}_${text.length}_${accentColor.toArgb()}_${textColor.toArgb()}_${baseFontSizeSp}_${isRoleplayFormatting}_$pack"
        synchronized(annotatedMarkdownCache) {
            annotatedMarkdownCache.get(cacheKey)
        } ?: run {
            val emojis = mutableSetOf<String>()
            val parsedAnnotated = if (isRoleplayFormatting) {
                RoleplayMarkdownParser.parseToAnnotatedString(
                    rawText = text,
                    accentColor = accentColor,
                    textColor = textColor,
                    baseFontSizeSp = baseFontSizeSp,
                    iconPack = pack,
                    collectedEmojis = emojis
                )
            } else {
                parseStandardMarkdown(
                    rawText = text,
                    textColor = textColor,
                    accentColor = accentColor,
                    baseFontSizeSp = baseFontSizeSp,
                    iconPack = pack,
                    collectedEmojis = emojis
                )
            }
            val res = ParsedRoleplayText(parsedAnnotated, emojis)
            synchronized(annotatedMarkdownCache) {
                annotatedMarkdownCache.put(cacheKey, res)
            }
            res
        }
    }

    val emojiSizeSp = (baseFontSizeSp * 1.25f).sp
    val emojiSizeDp = (baseFontSizeSp * 1.25f).dp

    val isEmojiImagePack = pack == EmojiPackManager.PACK_TWEMOJI ||
            pack == EmojiPackManager.PACK_NOTO ||
            pack == EmojiPackManager.PACK_BLOB ||
            pack == EmojiPackManager.PACK_OPENMOJI

    val inlineContent = remember(parsed.emojis, pack, baseFontSizeSp) {
        if (parsed.emojis.isEmpty() || !isEmojiImagePack) {
            emptyMap()
        } else {
            parsed.emojis.associate { emoji ->
                "emoji_$emoji" to InlineTextContent(
                    placeholder = Placeholder(
                        width = emojiSizeSp,
                        height = emojiSizeSp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                    )
                ) {
                    AdaptiveUiIcon(
                        emoji = emoji,
                        size = emojiSizeDp
                    )
                }
            }
        }
    }

    Text(
        text = parsed.annotated,
        modifier = modifier,
        lineHeight = (baseFontSizeSp * 1.45f).sp,
        textAlign = textAlign,
        inlineContent = inlineContent
    )
}

private fun parseStandardMarkdown(
    rawText: String,
    textColor: Color,
    accentColor: Color,
    baseFontSizeSp: Int,
    iconPack: String = EmojiPackManager.PACK_SYSTEM,
    collectedEmojis: MutableSet<String>? = null
) = buildAnnotatedString {
    if (rawText.isEmpty()) return@buildAnnotatedString
    var index = 0
    val length = rawText.length

    while (index < length) {
        // Bold **bold text**
        if (index + 1 < length && rawText[index] == '*' && rawText[index + 1] == '*') {
            val endIndex = rawText.indexOf("**", index + 2)
            if (endIndex != -1) {
                val boldContent = rawText.substring(index + 2, endIndex)
                pushStyle(
                    SpanStyle(
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = baseFontSizeSp.sp
                    )
                )
                EmojiInlineHelper.appendWithEmojis(this, boldContent, iconPack, collectedEmojis)
                pop()
                index = endIndex + 2
                continue
            } else {
                pushStyle(SpanStyle(color = textColor, fontSize = baseFontSizeSp.sp))
                append("**")
                pop()
                index += 2
                continue
            }
        }

        // Inline code `code`
        if (rawText[index] == '`') {
            val endIndex = rawText.indexOf('`', index + 1)
            if (endIndex != -1) {
                val codeContent = rawText.substring(index + 1, endIndex)
                pushStyle(
                    SpanStyle(
                        color = accentColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = (baseFontSizeSp - 1f).sp
                    )
                )
                EmojiInlineHelper.appendWithEmojis(this, codeContent, iconPack, collectedEmojis)
                pop()
                index = endIndex + 1
                continue
            } else {
                pushStyle(SpanStyle(color = textColor, fontSize = baseFontSizeSp.sp))
                append("`")
                pop()
                index += 1
                continue
            }
        }

        val nextDoubleStar = rawText.indexOf("**", index + 1)
        val nextBacktick = rawText.indexOf('`', index + 1)

        var nextSpecial = length
        if (nextDoubleStar != -1 && nextDoubleStar < nextSpecial) nextSpecial = nextDoubleStar
        if (nextBacktick != -1 && nextBacktick < nextSpecial) nextSpecial = nextBacktick

        val plainText = rawText.substring(index, nextSpecial)
        pushStyle(
            SpanStyle(
                color = textColor,
                fontSize = baseFontSizeSp.sp
            )
        )
        EmojiInlineHelper.appendWithEmojis(this, plainText, iconPack, collectedEmojis)
        pop()
        index = nextSpecial
    }
}
