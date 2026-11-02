package com.example.ai.engine

/**
 * Sanitizes and repairs AI generated posts that may have been cut off
 * due to low token limits (e.g. max_tokens / maxOutputTokens exhausted).
 *
 * Guarantees that posts are always logically and grammatically completed,
 * eliminating trailing dangling words, severed sentences, or unclosed formatting tags.
 */
object ResponseCutoffSanitizer {

    private val TERMINAL_CHARS = setOf('.', '!', '?', '…', '~')
    private val CLOSING_WRAPPERS = setOf('*', '>', '"', '»', '”', '\'', '’', ')', ']', '}')

    // Trailing conjunctions or prepositions that indicate a severed clause
    private val DANGLING_CONNECTORS = setOf(
        "и", "а", "но", "что", "где", "когда", "как", "в", "на", "с", "со", "к", "ко", "по", "о", "об",
        "за", "из", "под", "над", "для", "от", "до", "или", "бы", "ли", "же", "хотя", "если", "потому",
        "and", "or", "but", "the", "a", "an", "to", "in", "on", "at", "with", "that", "which", "because", "if"
    )

    /**
     * Inspects the generated text and ensures it concludes cleanly.
     */
    fun sanitizeAndComplete(rawText: String): String {
        var text = rawText.trim()
        if (text.isBlank()) return text

        // Check if text already ends cleanly with terminal punctuation
        if (!endsWithTerminalPunctuation(text)) {
            // Find the last terminal punctuation in the message
            var lastTerminalIdx = -1
            for (i in text.indices.reversed()) {
                if (TERMINAL_CHARS.contains(text[i])) {
                    lastTerminalIdx = i
                    break
                }
            }

            // If there is a complete sentence earlier in the message (after at least 20% of text)
            if (lastTerminalIdx >= 12 && lastTerminalIdx >= (text.length * 0.20f).toInt()) {
                // Advance past any immediately attached closing wrappers (e.g. .*, .", .», .>)
                var cutIndex = lastTerminalIdx + 1
                while (cutIndex < text.length && CLOSING_WRAPPERS.contains(text[cutIndex])) {
                    cutIndex++
                }
                text = text.substring(0, cutIndex).trim()
            } else {
                // The entire message was a single sentence that was cut off mid-way.
                // Clean dangling punctuation at end (commas, dashes, colons)
                text = text.trimEnd(' ', '\t', '\n', ',', '—', '-', '–', ':', ';')

                // Remove trailing dangling connectors (e.g., "...посмотрел на нее и")
                val lastWord = text.substringAfterLast(' ', "").lowercase()
                if (DANGLING_CONNECTORS.contains(lastWord)) {
                    val trimmedWord = text.substringBeforeLast(' ', "").trimEnd(' ', '\t', '\n', ',', '—', '-', '–', ':', ';')
                    if (trimmedWord.isNotBlank()) {
                        text = trimmedWord
                    }
                }

                // If still not ending in terminal punctuation, add terminal period
                if (text.isNotEmpty() && !endsWithTerminalPunctuation(text)) {
                    text = "$text."
                }
            }
        }

        // Balance any unclosed markdown and formatting wrappers
        text = balanceFormattingTags(text)

        return text
    }

    private fun endsWithTerminalPunctuation(str: String): Boolean {
        val trimmed = str.trimEnd { it.isWhitespace() || CLOSING_WRAPPERS.contains(it) }
        return trimmed.isNotEmpty() && TERMINAL_CHARS.contains(trimmed.last())
    }

    /**
     * Balances open asterisks, angle brackets, and quotation marks.
     */
    private fun balanceFormattingTags(input: String): String {
        var res = input

        // Balance asterisks: odd count means unclosed italic/bold tag
        val asteriskCount = res.count { it == '*' }
        if (asteriskCount % 2 != 0) {
            res = "$res*"
        }

        // Balance angle brackets <thought>
        val openAngles = res.count { it == '<' }
        val closeAngles = res.count { it == '>' }
        if (openAngles > closeAngles) {
            val needed = openAngles - closeAngles
            res += ">".repeat(needed)
        }

        // Balance Russian quotes « »
        val openRu = res.count { it == '«' }
        val closeRu = res.count { it == '»' }
        if (openRu > closeRu) {
            val needed = openRu - closeRu
            res += "»".repeat(needed)
        }

        // Balance straight quotes "
        val straightCount = res.count { it == '"' }
        if (straightCount % 2 != 0) {
            res = "$res\""
        }

        return res
    }
}
