package com.sumit.pocketgpt.data.inference

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FakeInferenceEngineTest {

    private val engine = FakeInferenceEngine()

    @Test
    fun `generate before load throws IllegalStateException at collection`() = runBlocking {
        val flow = engine.generate("Hi") // building the flow must NOT throw

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

        val tokens = engine.generate("Hi").toList()

        assertTrue(tokens.isNotEmpty())
    }

    @Test
    fun `blank prompt throws IllegalArgumentException at collection`() = runBlocking {
        engine.load() // loaded, so only the blank check can fire

        try {
            engine.generate("   ").collect { }
            fail("Expected IllegalArgumentException: prompt was blank")
        } catch (expected: IllegalArgumentException) {
        }
    }

    @Test
    fun `second collection while one is active throws IllegalStateException`() = runBlocking {
        engine.load()
        val first = launch { engine.generate("Hi").collect { } }
        delay(100) // let the first generation actually start streaming

        try {
            engine.generate("Hi").collect { }
            fail("Expected IllegalStateException: a generation is already active")
        } catch (expected: IllegalStateException) {
        }

        first.cancel()
    }

    @Test
    fun `engine is reusable after a completed generation`() = runBlocking {
        engine.load()
        engine.generate("first").toList()

        val second = engine.generate("second").toList()

        assertTrue(second.isNotEmpty())
    }
}