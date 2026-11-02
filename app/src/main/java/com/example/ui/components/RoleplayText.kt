package com.example.ui.components

import androidx.collection.LruCache
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.ai.engine.RoleplayMarkdownParser

private val annotatedMarkdownCache = LruCache<String, AnnotatedString>(450)

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
    val annotated = remember(text, accentColor, textColor, baseFontSizeSp, isRoleplayFormatting) {
        val cacheKey = "${text.hashCode()}_${text.length}_${accentColor.toArgb()}_${textColor.toArgb()}_${baseFontSizeSp}_$isRoleplayFormatting"
        synchronized(annotatedMarkdownCache) {
            annotatedMarkdownCache.get(cacheKey)
        } ?: run {
            val parsed = if (isRoleplayFormatting) {
                RoleplayMarkdownParser.parseToAnnotatedString(
                    rawText = text,
                    accentColor = accentColor,
                    textColor = textColor,
                    baseFontSizeSp = baseFontSizeSp
                )
            } else {
                parseStandardMarkdown(text, textColor, accentColor, baseFontSizeSp)
            }
            synchronized(annotatedMarkdownCache) {
                annotatedMarkdownCache.put(cacheKey, parsed)
            }
            parsed
        }
    }

    Text(
        text = annotated,
        modifier = modifier,
        lineHeight = (baseFontSizeSp * 1.45f).sp,
        textAlign = textAlign
    )
}

private fun parseStandardMarkdown(
    rawText: String,
    textColor: Color,
    accentColor: Color,
    baseFontSizeSp: Int
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
                append(boldContent)
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
                append(codeContent)
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
        append(plainText)
        pop()
        index = nextSpecial
    }
}
