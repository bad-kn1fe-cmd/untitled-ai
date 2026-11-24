package com.example.ai.rag

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Автономный локальный генератор семантических векторов (Local Feature Hashing Vectorizer).
 * Генерирует детерминированный L2-нормализованный вектор размерности 384.
 * Работает 100% офлайн, за 0.1 мс, без расхода трафика и без зависимости от внешних API.
 * Используется как мгновенный и надёжный fallback, если у удаленного провайдера нет embeddings API
 * (Groq, Cerebras, Anthropic, DeepSeek, Pollinations, бесплатный OpenRouter без баланса и т.д.)
 * или при сетевых сбоях/блокировках.
 */
object LocalEmbeddingGenerator : EmbeddingProvider {

    const val DIMENSION = 384

    override suspend fun getEmbedding(text: String): FloatArray? {
        val clean = text.trim()
        if (clean.isBlank()) return null
        return generateVector(clean)
    }

    fun generateVector(text: String): FloatArray {
        val clean = text.lowercase().trim()
        val vec = FloatArray(DIMENSION)
        if (clean.isBlank()) return vec

        // Токенизация на слова
        val words = clean.split(Regex("[^a-zа-я0-9_]+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return vec

        for (word in words) {
            val wordWeight = when {
                word.length <= 2 -> 0.5f
                word.length in 3..5 -> 1.0f
                word.length in 6..9 -> 1.6f
                else -> 2.2f
            }

            // Проекция целого слова
            projectToken(word, wordWeight * 2.0f, vec)

            // Стемминг корня (усечение окончаний для русского и английского)
            if (word.length >= 4) {
                val stemLen = (word.length * 0.7f).toInt().coerceAtLeast(3)
                val stem = word.substring(0, stemLen)
                projectToken(stem, wordWeight * 1.5f, vec)
            }

            // Символьные 3-граммы для устойчивости к опечаткам, склонениям и формам слов
            if (word.length >= 3) {
                for (i in 0..word.length - 3) {
                    val gram = word.substring(i, i + 3)
                    projectToken(gram, 0.7f, vec)
                }
            }
        }

        // Проекция биграмм смежных слов (для захвата словосочетаний и контекста)
        for (i in 0 until words.size - 1) {
            val bigram = "${words[i]}_${words[i + 1]}"
            projectToken(bigram, 2.0f, vec)
        }

        // L2-нормализация вектора на единичную сферу (|v| = 1.0)
        var sumSquares = 0.0
        for (v in vec) {
            sumSquares += (v * v).toDouble()
        }
        val norm = sqrt(sumSquares).toFloat()
        if (norm > 1e-6f) {
            for (i in vec.indices) {
                vec[i] /= norm
            }
        }

        return vec
    }

    private fun projectToken(token: String, weight: Float, vec: FloatArray) {
        // 64-битный хеш FNV-1a
        var hash = -3750763034362895579L
        for (ch in token) {
            hash = hash xor ch.code.toLong()
            hash *= 1099511628211L
        }

        val idx = (abs(hash) % DIMENSION).toInt()
        val sign = if ((hash shr 32 and 1L) == 0L) 1.0f else -1.0f
        vec[idx] += sign * weight
    }
}
