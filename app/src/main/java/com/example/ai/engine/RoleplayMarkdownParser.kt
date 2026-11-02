package com.example.ai.engine

import androidx.collection.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

sealed class RoleplaySpanType {
    data class CharacterName(val name: String) : RoleplaySpanType()
    data class Action(val text: String) : RoleplaySpanType()
    data class Thought(val text: String) : RoleplaySpanType()
    data class Dialogue(val text: String) : RoleplaySpanType()
}

object RoleplayMarkdownParser {

    private val cache = LruCache<String, AnnotatedString>(300)

    private val THOUGHT_TAG_REGEX = Regex(
        """<(?:thought|thoughts|think|мысли|мысль)>([\s\S]*?)<\/(?:thought|thoughts|think|мысли|мысль)>""",
        RegexOption.IGNORE_CASE
    )
    private val BRACKET_THOUGHT_REGEX = Regex("""<([^<>\n]+)>|‹([^‹›\n]+)›""")

    /**
     * Robust parser for roleplay markdown:
     * - **Name / Bold** or __Name__ -> Dimmed Accent Color, Bold
     * - ***Bold Italic*** or ___Bold Italic___ -> Bold + Italic, Accent Color
     * - *Action* or _Action_ -> Italic, Action Color (subtle muted contrast), standard font size
     * - <Thought> or <thought>...</thought> -> Italic, Thought Accent Color, ‹...› format
     * - `code` -> Monospace font, Accent color
     * - Regular text / dialogue -> Normal font style, High contrast text color
     */
    fun parseToAnnotatedString(
        rawText: String,
        accentColor: Color,
        textColor: Color,
        baseFontSizeSp: Int = 15
    ): AnnotatedString {
        if (rawText.isEmpty()) return AnnotatedString("")

        val cacheKey = "${rawText.hashCode()}_${accentColor.value}_${textColor.value}_$baseFontSizeSp"
        val cached = cache.get(cacheKey)
        if (cached != null) return cached

        // Normalize XML-style thought tags first into clean bracketed thoughts
        var normalized = rawText
        normalized = THOUGHT_TAG_REGEX.replace(normalized) { matchResult ->
            val inner = matchResult.groupValues[1].trim()
            "‹$inner›"
        }

        val nameColor = accentColor.copy(alpha = 0.90f)
        val actionColor = textColor.copy(alpha = 0.72f)
        val thoughtColor = accentColor.copy(alpha = 0.85f)
        val codeColor = accentColor

        val result = buildAnnotatedString {
            // Process text by scanning character by character with lookahead tokens
            var i = 0
            val len = normalized.length

            while (i < len) {
                // 1. Check for Inline Code `code`
                if (normalized[i] == '`') {
                    val endCode = normalized.indexOf('`', i + 1)
                    val nextNewline = normalized.indexOf('\n', i + 1)
                    if (endCode != -1 && (nextNewline == -1 || endCode < nextNewline)) {
                        val codeContent = normalized.substring(i + 1, endCode)
                        pushStyle(
                            SpanStyle(
                                color = codeColor,
                                fontFamily = FontFamily.Monospace,
                                fontSize = (baseFontSizeSp - 1f).sp
                            )
                        )
                        append(codeContent)
                        pop()
                        i = endCode + 1
                        continue
                    }
                }

                // 2. Check for Thought <thought> or ‹thought›
                if (normalized[i] == '<' || normalized[i] == '‹') {
                    val closeChar = if (normalized[i] == '<') '>' else '›'
                    val endThought = normalized.indexOf(closeChar, i + 1)
                    val nextNewline = normalized.indexOf('\n', i + 1)
                    // Allow thoughts to span across short text, but avoid eating whole document if unclosed
                    if (endThought != -1 && (nextNewline == -1 || endThought < nextNewline + 200)) {
                        val thoughtContent = normalized.substring(i + 1, endThought).trim()
                        pushStyle(
                            SpanStyle(
                                color = thoughtColor,
                                fontStyle = FontStyle.Italic,
                                fontSize = (baseFontSizeSp - 0.5f).sp
                            )
                        )
                        append("‹$thoughtContent›")
                        pop()
                        i = endThought + 1
                        continue
                    }
                }

                // 3. Check for Bold + Italic ***text*** or ___text___
                if (i + 2 < len &&
                    ((normalized[i] == '*' && normalized[i + 1] == '*' && normalized[i + 2] == '*') ||
                     (normalized[i] == '_' && normalized[i + 1] == '_' && normalized[i + 2] == '_'))
                ) {
                    val delim = normalized.substring(i, i + 3)
                    val endBoldItalic = normalized.indexOf(delim, i + 3)
                    val nextNewline = normalized.indexOf("\n\n", i + 3)
                    if (endBoldItalic != -1 && (nextNewline == -1 || endBoldItalic < nextNewline)) {
                        val content = normalized.substring(i + 3, endBoldItalic)
                        pushStyle(
                            SpanStyle(
                                color = nameColor,
                                fontWeight = FontWeight.Bold,
                                fontStyle = FontStyle.Italic,
                                fontSize = baseFontSizeSp.sp
                            )
                        )
                        append(content)
                        pop()
                        i = endBoldItalic + 3
                        continue
                    }
                }

                // 4. Check for Bold / Name **text** or __text__
                if (i + 1 < len &&
                    ((normalized[i] == '*' && normalized[i + 1] == '*') ||
                     (normalized[i] == '_' && normalized[i + 1] == '_'))
                ) {
                    val delim = normalized.substring(i, i + 2)
                    val endBold = normalized.indexOf(delim, i + 2)
                    val nextNewline = normalized.indexOf("\n\n", i + 2)
                    if (endBold != -1 && (nextNewline == -1 || endBold < nextNewline)) {
                        val content = normalized.substring(i + 2, endBold)
                        pushStyle(
                            SpanStyle(
                                color = nameColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = (baseFontSizeSp + 0.5f).sp
                            )
                        )
                        append(content)
                        pop()
                        i = endBold + 2
                        continue
                    }
                }

                // 5. Check for Action / Italic *text* or _text_
                if (normalized[i] == '*' || normalized[i] == '_') {
                    val delim = normalized[i]
                    // Make sure it's not preceded/followed by alphanumeric for underscores inside words (e.g. some_variable_name)
                    val isUnderscoreInWord = delim == '_' && i > 0 && normalized[i - 1].isLetterOrDigit() && i + 1 < len && normalized[i + 1].isLetterOrDigit()
                    if (!isUnderscoreInWord) {
                        val endAction = findClosingDelimiter(normalized, delim, i + 1)
                        if (endAction != -1) {
                            val actionContent = normalized.substring(i + 1, endAction)
                            pushStyle(
                                SpanStyle(
                                    color = actionColor,
                                    fontStyle = FontStyle.Italic,
                                    fontSize = baseFontSizeSp.sp
                                )
                            )
                            append("*$actionContent*")
                            pop()
                            i = endAction + 1
                            continue
                        }
                    }
                }

                // 6. Regular Dialogue / Text chunk until next special delimiter
                val nextSpecial = findNextSpecialIndex(normalized, i + 1)
                val plainText = normalized.substring(i, nextSpecial)
                pushStyle(
                    SpanStyle(
                        color = textColor,
                        fontStyle = FontStyle.Normal,
                        fontSize = baseFontSizeSp.sp
                    )
                )
                append(plainText)
                pop()
                i = nextSpecial
            }
        }
        cache.put(cacheKey, result)
        return result
    }

    private fun findClosingDelimiter(text: String, delim: Char, startIndex: Int): Int {
        val len = text.length
        var curr = startIndex
        val nextDoubleNewline = text.indexOf("\n\n", startIndex)
        val limit = if (nextDoubleNewline != -1) nextDoubleNewline else len

        while (curr < limit) {
            if (text[curr] == delim) {
                // If it's part of a double delimiter (e.g., **), skip it
                if (curr + 1 < len && text[curr + 1] == delim) {
                    curr += 2
                    continue
                }
                // Valid closing single delimiter found
                return curr
            }
            curr++
        }
        return -1
    }

    private fun findNextSpecialIndex(text: String, startIndex: Int): Int {
        val len = text.length
        for (idx in startIndex until len) {
            val c = text[idx]
            if (c == '*' || c == '_' || c == '<' || c == '‹' || c == '`') {
                return idx
            }
        }
        return len
    }
}

