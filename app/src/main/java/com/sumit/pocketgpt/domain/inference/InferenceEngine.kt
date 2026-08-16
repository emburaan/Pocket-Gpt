package com.sumit.pocketgpt.domain.inference

import com.sumit.pocketgpt.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface InferenceEngine {
    val modelState: StateFlow<ModelState>

/**
 * Streams the model's response to the conversation in [messages], emitting each
 * token as it is produced.
 *
 * [messages] is the whole conversation, oldest first, with the new user turn last:
 * every element before the last is context the model is given before it answers,
 * and the last element is the turn it answers. The engine holds no history of its
 * own between calls, so the caller passes the full conversation every time — and
 * therefore owns what stays in it. Cost grows with the conversation: every call
 * re-processes every message before the first token appears.
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
 * - [IllegalArgumentException] — [messages] is empty, its last element is not a
 *   [Role.USER] message, or that message's content is blank. The content of
 *   earlier messages is not validated.
 *
 * Chronological order is assumed and is *not* verified: a mis-ordered list does
 * not fail, it silently produces a wrong conversation. Strict user/model
 * alternation is not required. The engine neither mutates nor retains [messages].
 *
 * Cancelling the collecting coroutine stops the generation cooperatively, at
 * the next token boundary. The model stays loaded and usable afterwards.
 *
 * @param messages The conversation to continue: oldest first, new [Role.USER] turn
 *   last. Must not be empty.
 * @return A cold flow of token deltas. Completes normally when the response is
 *   finished, or terminates exceptionally — possibly after some tokens have
 *   already been emitted.
 */
    fun generate(messages: List<ChatMessage>): Flow<String>

    suspend fun load()

    suspend fun unload()
}