package com.example.ai.rag

import kotlin.math.sqrt

class HybridRetriever(
    private val rrfK: Int = 60
) {

    fun retrieve(
        query: String,
        chunks: List<LoreChunk>,
        queryEmbedding: FloatArray? = null
    ): List<ScoredChunk> {
        if (chunks.isEmpty()) return emptyList()

        val cleanQuery = query.lowercase().replace(Regex("[^a-zа-я0-9\\s]"), " ").trim()
        val queryTerms = cleanQuery.split(Regex("\\s+")).filter { it.length >= 2 }.toSet()

        // 1. Lexical Search Ranking
        val lexicalScores = chunks.map { chunk ->
            val chunkTextLower = chunk.text.lowercase()
            var matches = 0.0
            for (term in queryTerms) {
                if (chunk.keywords.contains(term)) matches += 2.0
                if (chunkTextLower.contains(term)) matches += 1.0
                if (term.length >= 4 && chunkTextLower.contains(term.substring(0, term.length - 1))) {
                    matches += 0.5
                }
            }
            chunk to matches
        }.sortedByDescending { it.second }

        val rankedByKeywords = lexicalScores.map { it.first }

        // 2. Semantic Vector Search Ranking
        val vectorScores = if (queryEmbedding != null) {
            chunks.mapNotNull { chunk ->
                val vec = chunk.embedding
                if (vec != null) {
                    val sim = computeCosineSimilarity(queryEmbedding, vec)
                    chunk to sim
                } else null
            }.sortedByDescending { it.second }
        } else emptyList()

        val rankedByVector = vectorScores.map { it.first }

        // 3. Reciprocal Rank Fusion (RRF)
        val rrfScores = mutableMapOf<LoreChunk, Double>()

        // RRF for Keywords
        rankedByKeywords.forEachIndexed { rank, chunk ->
            val rrf = 1.0 / (rrfK + (rank + 1))
            rrfScores[chunk] = (rrfScores[chunk] ?: 0.0) + rrf
        }

        // RRF for Vectors
        rankedByVector.forEachIndexed { rank, chunk ->
            val rrf = 1.0 / (rrfK + (rank + 1))
            rrfScores[chunk] = (rrfScores[chunk] ?: 0.0) + rrf
        }

        val result = rrfScores.map { (chunk, score) ->
            ScoredChunk(chunk, score)
        }.sortedByDescending { it.score }

        return result.ifEmpty { chunks.map { ScoredChunk(it, 1.0) } }
    }

    private fun computeCosineSimilarity(a: FloatArray, b: FloatArray): Double {
        if (a.size != b.size || a.isEmpty()) return 0.0
        var dot = 0.0
        var normA = 0.0
        var normB = 0.0

        for (i in a.indices) {
            val va = a[i].toDouble()
            val vb = b[i].toDouble()
            dot += va * vb
            normA += va * va
            normB += vb * vb
        }

        val denominator = sqrt(normA) * sqrt(normB)
        if (denominator <= 0.00001) return 0.0
        return dot / denominator
    }
}
