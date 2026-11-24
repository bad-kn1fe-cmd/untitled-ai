package com.example.ai.rag

object ContextInjector {

    private const val DEFAULT_TOP_K = 3
    private const val MAX_TOTAL_CHARS = 3600
    private const val MIN_SCORE_THRESHOLD = 0.001

    fun injectRelevantLore(
        userQuery: String,
        allChunks: List<LoreChunk>,
        queryEmbedding: FloatArray? = null,
        retriever: HybridRetriever = HybridRetriever(),
        topK: Int = DEFAULT_TOP_K
    ): String? {
        if (allChunks.isEmpty()) return null

        val cleanQuery = userQuery.ifBlank { "контекст лор" }
        val scored = retriever.retrieve(cleanQuery, allChunks, queryEmbedding)
        val filtered = scored.filter { it.score >= MIN_SCORE_THRESHOLD }
        val relevant = if (filtered.isNotEmpty()) filtered.take(topK) else scored.take(topK)

        if (relevant.isEmpty()) return null

        val sb = StringBuilder()
        var currentChars = 0

        for (scoredChunk in relevant) {
            val text = scoredChunk.chunk.text.trim()
            if (currentChars + text.length > MAX_TOTAL_CHARS) break

            if (sb.isNotEmpty()) {
                sb.append("\n---\n")
            }
            sb.append(text)
            currentChars += text.length
        }

        if (sb.isEmpty()) return null

        return "<relevant_lore>\n$sb\n</relevant_lore>"
    }
}
