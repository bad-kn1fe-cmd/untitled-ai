package com.example

import com.example.ai.engine.ResponseCutoffSanitizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponseCutoffSanitizerTest {

    @Test
    fun testTrimsDanglingTailWhenPreviousSentenceIsComplete() {
        val input = "**Алиса** *посмотрела на тебя и улыбнулась.* — Привет! Как твои дела? Я сегодня весь день думала о том, что"
        val output = ResponseCutoffSanitizer.sanitizeAndComplete(input)
        assertEquals("**Алиса** *посмотрела на тебя и улыбнулась.* — Привет! Как твои дела?", output)
    }

    @Test
    fun testCompletesSingleSeveredSentenceGracefully() {
        val input = "*Она тихо кивнула и подошла ближе, чувствуя сильное волнение и"
        val output = ResponseCutoffSanitizer.sanitizeAndComplete(input)
        assertTrue(output.endsWith(".*") || output.endsWith("."))
        assertTrue(!output.contains("волнение и."))
    }

    @Test
    fun testBalancesUnclosedFormattingTags() {
        val input = "*Она посмотрела в окно и сказала: — Скоро пойдет дождь."
        val output = ResponseCutoffSanitizer.sanitizeAndComplete(input)
        assertTrue(output.endsWith(".*"))

        val singleThought = "<Интересно, он вернется"
        val outputThought = ResponseCutoffSanitizer.sanitizeAndComplete(singleThought)
        assertTrue(outputThought.endsWith(">"))

        val danglingThoughtAfterComplete = "*Она вздохнула.* <Интересно, он вернется"
        val outputDangling = ResponseCutoffSanitizer.sanitizeAndComplete(danglingThoughtAfterComplete)
        assertEquals("*Она вздохнула.*", outputDangling)
    }

    @Test
    fun testLeavesFullyCompletedTextUnchanged() {
        val input = "**Герой** *сделал шаг вперед.* — Я готов идти до конца!"
        val output = ResponseCutoffSanitizer.sanitizeAndComplete(input)
        assertEquals(input, output)
    }
}
