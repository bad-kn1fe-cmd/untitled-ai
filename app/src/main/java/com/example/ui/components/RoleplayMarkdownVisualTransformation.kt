package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Live Markdown syntax highlighter for text fields.
 * Transforms roleplay asterisks (*action*), bold (**bold**), thoughts (<thought> or ‹thought›),
 * and inline code (`code`) into stylized formatted spans without altering string length.
 */
class RoleplayMarkdownVisualTransformation(
    private val accentColor: Color,
    private val textColor: Color
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val actionColor = textColor.copy(alpha = 0.72f)
        val thoughtColor = accentColor.copy(alpha = 0.85f)
        val codeColor = accentColor
        val boldColor = accentColor.copy(alpha = 0.90f)

        val annotated = buildAnnotatedString {
            append(raw)

            var i = 0
            val len = raw.length

            while (i < len) {
                // 1. Check for inline code `code`
                if (raw[i] == '`') {
                    val end = raw.indexOf('`', i + 1)
                    if (end != -1) {
                        addStyle(
                            SpanStyle(
                                color = codeColor,
                                fontFamily = FontFamily.Monospace
                            ),
                            i,
                            end + 1
                        )
                        i = end + 1
                        continue
                    }
                }

                // 2. Check for thoughts <thought> or ‹thought›
                if (raw[i] == '<' || raw[i] == '‹') {
                    val closeChar = if (raw[i] == '<') '>' else '›'
                    val end = raw.indexOf(closeChar, i + 1)
                    if (end != -1) {
                        addStyle(
                            SpanStyle(
                                color = thoughtColor,
                                fontStyle = FontStyle.Italic
                            ),
                            i,
                            end + 1
                        )
                        i = end + 1
                        continue
                    }
                }

                // 3. Check for Bold + Italic ***text*** or ___text___
                if (i + 2 < len &&
                    ((raw[i] == '*' && raw[i + 1] == '*' && raw[i + 2] == '*') ||
                     (raw[i] == '_' && raw[i + 1] == '_' && raw[i + 2] == '_'))
                ) {
                    val delim = raw.substring(i, i + 3)
                    val end = raw.indexOf(delim, i + 3)
                    if (end != -1) {
                        addStyle(
                            SpanStyle(
                                color = boldColor,
                                fontWeight = FontWeight.Bold,
                                fontStyle = FontStyle.Italic
                            ),
                            i,
                            end + 3
                        )
                        i = end + 3
                        continue
                    }
                }

                // 4. Check for bold **text** or __text__
                if (i + 1 < len &&
                    ((raw[i] == '*' && raw[i + 1] == '*') ||
                     (raw[i] == '_' && raw[i + 1] == '_'))
                ) {
                    val delim = raw.substring(i, i + 2)
                    val end = raw.indexOf(delim, i + 2)
                    if (end != -1) {
                        addStyle(
                            SpanStyle(
                                color = boldColor,
                                fontWeight = FontWeight.Bold
                            ),
                            i,
                            end + 2
                        )
                        i = end + 2
                        continue
                    }
                }

                // 5. Check for action *text* or _text_
                if (raw[i] == '*' || raw[i] == '_') {
                    val delim = raw[i]
                    val isUnderscoreInWord = delim == '_' && i > 0 && raw[i - 1].isLetterOrDigit() && i + 1 < len && raw[i + 1].isLetterOrDigit()
                    if (!isUnderscoreInWord) {
                        val end = findClosingDelim(raw, delim, i + 1)
                        if (end != -1) {
                            addStyle(
                                SpanStyle(
                                    color = actionColor,
                                    fontStyle = FontStyle.Italic
                                ),
                                i,
                                end + 1
                            )
                            i = end + 1
                            continue
                        }
                    }
                }

                i += 1
            }
        }

        return TransformedText(annotated, OffsetMapping.Identity)
    }

    private fun findClosingDelim(text: String, delim: Char, startIndex: Int): Int {
        val len = text.length
        var curr = startIndex
        val nextDoubleNewline = text.indexOf("\n\n", startIndex)
        val limit = if (nextDoubleNewline != -1) nextDoubleNewline else len

        while (curr < limit) {
            if (text[curr] == delim) {
                if (curr + 1 < len && text[curr + 1] == delim) {
                    curr += 2
                    continue
                }
                return curr
            }
            curr++
        }
        return -1
    }
}

