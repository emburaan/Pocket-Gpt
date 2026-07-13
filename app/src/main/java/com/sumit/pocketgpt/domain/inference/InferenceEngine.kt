package com.sumit.pocketgpt.domain.inference

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface InferenceEngine {
    val modelState: StateFlow<ModelState>

    /**
     * Streams the model's response to [prompt], emitting each token as it is produced.
     *
     * The returned flow is **cold**: calling this function performs no inference and
     * throws nothing. All checks and the generation itself run when the flow is
     * collected, and each collection starts a fresh generation.
     *
     * Loading is the caller's job: [load] must have completed before collection, and
     * this function never loads implicitly. Loading is expensive — a multi-gigabyte,
     * memory-mapped checkpoint — and its failures (missing or corrupt model file,
     * insufficient RAM) are unrelated to generation failures, so the caller owns
     * when, and whether, it happens.
     *
     * Thrown at collection time, never at call time:
     * - [IllegalStateException] — the engine is not loaded ([load] was never called), or
     *   another generation is already active on this engine (one at a time; concurrent
     *   collection does not queue).
     * - [IllegalArgumentException] — [prompt] is blank.
     *
     * Cancelling the collecting coroutine stops the generation cooperatively, at
     * the next token boundary. The model stays loaded and usable afterwards.
     * @param prompt The user turn to complete. Must not be blank.
     * @return A cold flow of token deltas. Completes normally when the response is
     *   finished, or terminates exceptionally — possibly after some tokens have
     *   already been emitted.
     *
     */
    fun generate(prompt: String): Flow<String>

    suspend fun load()

    suspend fun unload()
}