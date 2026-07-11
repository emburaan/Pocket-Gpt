package com.sumit.pocketgpt.domain.inference

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface InferenceEngine {
    val modelState: StateFlow<ModelState>

    fun generate(prompt: String): Flow<String>

    suspend fun load()

    suspend fun unload()
}