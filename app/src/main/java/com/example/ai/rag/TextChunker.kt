package com.example.ai.rag

object TextChunker {

    private const val MIN_CHUNK_SIZE = 150
    private const val MAX_CHUNK_SIZE = 600
    private const val TARGET_OVERLAP = 120

    fun chunkText(rawText: String, idPrefix: String = "chunk"): List<LoreChunk> {
        val clean = rawText.trim()
        if (clean.isBlank()) return emptyList()

        val chunks = mutableListOf<LoreChunk>()

        // 1. Split into natural paragraph blocks
        val paragraphs = clean.split(Regex("\\n\\n+")).asSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
        val blocks = mutableListOf<String>()

        for (p in paragraphs) {
            if (p.length <= MAX_CHUNK_SIZE) {
                blocks.add(p)
            } else {
                // Split long paragraphs into sentences
                val sentences = p.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotBlank() }
                blocks.addAll(sentences)
            }
        }

        // 2. Combine blocks into chunks with overlap
        var currentChunk = mutableListOf<String>()
        var currentLength = 0
        var chunkIndex = 0

        for (block in blocks) {
            val exceedsSize = currentLength + block.length > MAX_CHUNK_SIZE
            // Check if adding the next block exceeds max size
            if (exceedsSize && currentChunk.isNotEmpty()) {
                val chunkStr = currentChunk.joinToString(" ").trim()
                if (chunkStr.length >= MIN_CHUNK_SIZE) {
                    chunks.add(
                        LoreChunk(
                            id = "${idPrefix}_$chunkIndex",
                            text = chunkStr,
                            keywords = extractBasicKeywords(chunkStr),
                        ),
                    )
                    chunkIndex++
                }

                // Create overlap from the end of currentChunk
                var overlapLength = 0
                val overlapBlocks = mutableListOf<String>()
                for (i in currentChunk.indices.reversed()) {
                    val b = currentChunk[i]
                    val fits = overlapLength + b.length <= TARGET_OVERLAP * 2
                    if (fits) {
                        overlapBlocks.add(0, b)
                        overlapLength += b.length + 1
                    } else break
                }
                if (overlapBlocks.isEmpty() && currentChunk.isNotEmpty()) {
                    overlapBlocks.add(currentChunk.last())
                }

                currentChunk = overlapBlocks
                currentLength = currentChunk.sumOf { it.length + 1 }
            }

            // If a single block is still monstrously huge (e.g. no punctuation), split by spaces
            if (block.length > MAX_CHUNK_SIZE) {
                val words = block.split(Regex("\\s+"))
                var tempSentence = mutableListOf<String>()
                var tempLen = 0
                
                for (word in words) {
                    if (currentLength + tempLen + word.length > MAX_CHUNK_SIZE && (currentChunk.isNotEmpty() || tempSentence.isNotEmpty())) {
                        if (tempSentence.isNotEmpty()) currentChunk.add(tempSentence.joinToString(" "))
                        
                        val chunkStr = currentChunk.joinToString(" ").trim()
                        if (chunkStr.length >= MIN_CHUNK_SIZE) {
                            chunks.add(
                                LoreChunk(
                                    id = "${idPrefix}_$chunkIndex",
                                    text = chunkStr,
                                    keywords = extractBasicKeywords(chunkStr),
                                ),
                            )
                            chunkIndex++
                        }
                        
                        // Overlap fallback for raw words
                        val lastFewWords = tempSentence.takeLast(15)
                        currentChunk = mutableListOf(lastFewWords.joinToString(" "))
                        currentLength = currentChunk[0].length + 1
                        
                        tempSentence = mutableListOf()
                        tempLen = 0
                    }
                    tempSentence.add(word)
                    tempLen += word.length + 1
                }
                if (tempSentence.isNotEmpty()) {
                    currentChunk.add(tempSentence.joinToString(" "))
                    currentLength += tempLen
                }

            } else {
                currentChunk.add(block)
                currentLength += block.length + 1
            }
        }

        // Add any remaining tail
        if (currentChunk.isNotEmpty()) {
            val chunkStr = currentChunk.joinToString(" ").trim()
            if (chunkStr.length >= 30) {
                chunks.add(
                    LoreChunk(
                        id = "${idPrefix}_$chunkIndex",
                        text = chunkStr,
                        keywords = extractBasicKeywords(chunkStr),
                    ),
                )
            }
        }

        return chunks
    }

    private val STOP_WORDS = setOf(
        "что", "это", "как", "так", "для", "или", "чтобы", "если", "когда", "можно", "даже", "самое", "который",
        "ведь", "где", "там", "тут", "здесь", "его", "ее", "их", "нас", "вас", "мне", "тебе", "ему", "ей", "нам", "вам",
        "быть", "был", "была", "были", "будет", "есть", "нет", "тоже", "только", "уже", "еще", "все", "всё", "они", "мы", "вы",
        "под", "над", "при", "про", "без", "кто", "этот", "тот", "такой",
    )

    private fun extractBasicKeywords(text: String): Set<String> {
        val words = text.lowercase()
            .replace(Regex("[\\n\\r\\t]"), " ")
            .replace(Regex("[^a-zа-я0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length >= 3 && it !in STOP_WORDS }
        return words.toSet()
    }
}
