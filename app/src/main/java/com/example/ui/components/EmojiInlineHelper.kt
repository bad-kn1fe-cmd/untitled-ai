package com.example.ui.components

import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.AnnotatedString

object EmojiInlineHelper {

    data class EmojiSpan(val start: Int, val end: Int, val emoji: String)

    fun mayContainEmoji(text: String): Boolean {
        for (i in 0 until text.length) {
            val c = text[i]
            if (c >= '\u2000') return true
        }
        return false
    }

    private fun isZwj(cp: Int): Boolean = cp == 0x200D

    private fun isModifierOrJoiner(cp: Int): Boolean {
        return cp == 0x200D || cp == 0xFE0F || cp == 0xFE0E ||
                (cp in 0x1F3FB..0x1F3FF) || // skin tones
                (cp in 0xE0020..0xE007F)    // tag characters for flags
    }

    private fun isContinuationEmoji(cp: Int, text: String, pos: Int): Boolean {
        if (pos > 0) {
            val prevCp = Character.codePointBefore(text, pos)
            if (prevCp == 0x200D) return true
        }
        if (cp in 0x1F1E6..0x1F1FF) {
            if (pos > 0) {
                val prevCp = Character.codePointBefore(text, pos)
                if (prevCp in 0x1F1E6..0x1F1FF) return true
            }
        }
        return false
    }

    private fun isBaseEmoji(cp: Int): Boolean {
        if (cp in 0x1F000..0x1FAFF) return true
        if (cp in 0x2600..0x27BF) return true
        if (cp in 0x2300..0x23FF) return true
        if (cp in 0x2B50..0x2B55) return true
        if (cp in 0x2190..0x21FF) return true
        if (cp in 0x2934..0x2935) return true
        if (cp in 0x3297..0x3299) return true
        if (cp == 0x3030 || cp == 0x303D || cp == 0x00A9 || cp == 0x00AE || cp == 0x203C || cp == 0x2049 || cp == 0x2122 || cp == 0x2139) return true
        return false
    }

    fun findEmojiSpans(text: String): List<EmojiSpan> {
        if (!mayContainEmoji(text)) return emptyList()
        val spans = mutableListOf<EmojiSpan>()
        val len = text.length
        var i = 0
        while (i < len) {
            val cp = text.codePointAt(i)
            if (isBaseEmoji(cp)) {
                val start = i
                var current = i + Character.charCount(cp)
                while (current < len) {
                    val nextCp = text.codePointAt(current)
                    if (isModifierOrJoiner(nextCp)) {
                        current += Character.charCount(nextCp)
                    } else if (current > start && isZwj(text.codePointAt(current - 1)) && isBaseEmoji(nextCp)) {
                        current += Character.charCount(nextCp)
                    } else if (isBaseEmoji(nextCp) && isContinuationEmoji(nextCp, text, current)) {
                        current += Character.charCount(nextCp)
                    } else {
                        break
                    }
                }
                val emojiStr = text.substring(start, current)
                spans.add(EmojiSpan(start, current, emojiStr))
                i = current
            } else {
                i += Character.charCount(cp)
            }
        }
        return spans
    }

    fun appendWithEmojis(
        builder: AnnotatedString.Builder,
        text: String,
        iconPack: String,
        collectedEmojis: MutableSet<String>?
    ) {
        if (text.isEmpty()) return
        val isEmojiImagePack = iconPack == EmojiPackManager.PACK_TWEMOJI ||
                iconPack == EmojiPackManager.PACK_NOTO ||
                iconPack == EmojiPackManager.PACK_BLOB ||
                iconPack == EmojiPackManager.PACK_OPENMOJI

        if (!isEmojiImagePack || !mayContainEmoji(text)) {
            builder.append(text)
            return
        }

        val spans = findEmojiSpans(text)
        if (spans.isEmpty()) {
            builder.append(text)
            return
        }

        var lastIndex = 0
        for (span in spans) {
            if (span.start > lastIndex) {
                builder.append(text.substring(lastIndex, span.start))
            }
            builder.appendInlineContent(id = "emoji_${span.emoji}", alternateText = span.emoji)
            collectedEmojis?.add(span.emoji)
            lastIndex = span.end
        }

        if (lastIndex < text.length) {
            builder.append(text.substring(lastIndex))
        }
    }
}
