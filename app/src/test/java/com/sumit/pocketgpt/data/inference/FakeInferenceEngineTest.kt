package com.sumit.pocketgpt.data.inference

import com.sumit.pocketgpt.domain.model.ChatMessage
import com.sumit.pocketgpt.domain.model.Role
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FakeInferenceEngineTest {

    private val engine = FakeInferenceEngine()

    /** One user turn, the smallest legal conversation. */
    private fun conversation(text: String = "Hi") =
        listOf(ChatMessage(role = Role.USER, content = text, createdAt = 0L))

    @Test
    fun `generate before load throws IllegalStateException at collection`() = runBlocking {
        val flow = engine.generate(conversation()) // building the flow must NOT throw

        try {
            flow.collect { }
            fail("Expected IllegalStateException: model was never loaded")
        } catch (expected: IllegalStateException) {
            // contract upheld: thrown at collection time, not call time
        }
    }

    @Test
    fun `generate after load streams tokens`() = runBlocking {
        engine.load()

        val tokens = engine.generate(conversation()).toList()

        assertTrue(tokens.isNotEmpty())
    }

    @Test
    fun `empty conversation throws IllegalArgumentException at collection`() = runBlocking {
        engine.load() // loaded, so only an argument check can fire

        try {
            engine.generate(emptyList()).collect { }
            fail("Expected IllegalArgumentException: messages was empty")
        } catch (expected: IllegalArgumentException) {
        }
    }

    @Test
    fun `conversation ending in a model turn throws IllegalArgumentException at collection`() =
        runBlocking {
            engine.load()
            val endsWithModel = conversation() +
                ChatMessage(role = Role.MODEL, content = "Hello!", createdAt = 1L)

            try {
                engine.generate(endsWithModel).collect { }
                fail("Expected IllegalArgumentException: last message was not from the user")
            } catch (expected: IllegalArgumentException) {
            }
        }

    @Test
    fun `blank last message throws IllegalArgumentException at collection`() = runBlocking {
        engine.load()

        try {
            engine.generate(conversation("   ")).collect { }
            fail("Expected IllegalArgumentException: last message content was blank")
        } catch (expected: IllegalArgumentException) {
        }
    }

    @Test
    fun `blank content earlier in the conversation is not rejected`() = runBlocking {
        engine.load()
        val blankHistory = listOf(
            ChatMessage(role = Role.USER, content = "   ", createdAt = 0L),
            ChatMessage(role = Role.MODEL, content = "   ", createdAt = 1L),
            ChatMessage(role = Role.USER, content = "what's my name?", createdAt = 2L),
        )

        val tokens = engine.generate(blankHistory).toList()

        assertTrue(tokens.isNotEmpty())
    }

    @Test
    fun `second collection while one is active throws IllegalStateException`() = runBlocking {
        engine.load()
        val first = launch { engine.generate(conversation()).collect { } }
        delay(100) // let the first generation actually start streaming

        try {
            engine.generate(conversation()).collect { }
            fail("Expected IllegalStateException: a generation is already active")
        } catch (expected: IllegalStateException) {
        }

        first.cancel()
    }

    @Test
    fun `engine is reusable after a completed generation`() = runBlocking {
        engine.load()
        engine.generate(conversation("first")).toList()

        val second = engine.generate(conversation("second")).toList()

        assertTrue(second.isNotEmpty())
    }

    @Test
    fun `the whole conversation reaches the engine, not just the last turn`() = runBlocking {
        engine.load()
        val threeTurns = listOf(
            ChatMessage(role = Role.USER, content = "my name is Sumit", createdAt = 0L),
            ChatMessage(role = Role.MODEL, content = "Nice to meet you!", createdAt = 1L),
            ChatMessage(role = Role.USER, content = "what's my name?", createdAt = 2L),
        )

        engine.generate(threeTurns).toList()

        assertEquals(threeTurns, engine.lastMessages)
    }
}