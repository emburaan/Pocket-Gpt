@file:OptIn(ExperimentalAtomicApi::class)

package com.sumit.pocketgpt.data.inference

import com.sumit.pocketgpt.domain.inference.InferenceEngine
import com.sumit.pocketgpt.domain.inference.ModelState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

class FakeInferenceEngine @Inject constructor() : InferenceEngine {

    private val isGenerating = AtomicBoolean(false)
    private val _modelState = MutableStateFlow<ModelState>(
        ModelState.Unloaded
    )
    override val modelState: StateFlow<ModelState> = _modelState.asStateFlow()

    override fun generate(prompt: String): Flow<String> = flow {
        require(prompt.isNotBlank()) { "Prompt must not be blank" }

        check(modelState.value == ModelState.Ready) {
            "Model is not loaded — call load() before generate()"
        }
        check(isGenerating.compareAndSet(expectedValue = false, newValue = true)) {
            "a generation is already active on this engine"
        }
        val response = "This is a fake streamed reply from PocketGPT running fully on device."
        try {
            for (chunk in response.split(" ")) {
                emit("$chunk ")
                delay(50)
            }
        } finally {
            isGenerating.store(false)
        }
    }

    override suspend fun load() {
        _modelState.value = ModelState.Loading
        delay(2000)
        _modelState.value = ModelState.Ready
    }

    override suspend fun unload() {
        _modelState.value = ModelState.Unloaded
    }
}