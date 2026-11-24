package com.example.ai.rag

data class LoreChunk(
    val id: String,
    val text: String,
    val keywords: Set<String>,
    val embedding: FloatArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LoreChunk) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

data class ScoredChunk(
    val chunk: LoreChunk,
    val score: Double
)
