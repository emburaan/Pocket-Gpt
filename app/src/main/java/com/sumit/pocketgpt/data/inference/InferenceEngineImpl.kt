@file:OptIn(ExperimentalAtomicApi::class)

package com.sumit.pocketgpt.data.inference

import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.sumit.pocketgpt.di.CacheDir
import com.sumit.pocketgpt.di.ModelPath
import com.sumit.pocketgpt.domain.inference.InferenceEngine
import com.sumit.pocketgpt.domain.inference.ModelState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

class InferenceEngineImpl @Inject constructor(
    @ModelPath private val modelPath: String,
    @CacheDir private val cacheDir: String
) : InferenceEngine {

    private val isGenerating = AtomicBoolean(false)

    private val _modelState = MutableStateFlow<ModelState>(
        ModelState.Unloaded
    )

    private var engine: Engine? = null
    override val modelState: StateFlow<ModelState> = _modelState.asStateFlow()

    override fun generate(prompt: String): Flow<String> = flow {
        require(prompt.isNotBlank()) { "Prompt must not be blank" }

        check(modelState.value == ModelState.Ready) {
            "Model is not loaded — call load() before generate()"
        }

        check(isGenerating.compareAndSet(expectedValue = false, newValue = true)) {
            "a generation is already active on this engine"
        }
        val activeEngine = engine ?: error("engine is null despite Ready state")
        try {
            activeEngine.createConversation().use { conversation ->
                try {

                    conversation.sendMessageAsync(prompt).collect { message ->
                        val text = message.contents.contents
                            .filterIsInstance<Content.Text>()
                            .joinToString("") { it.text }
                        // LiteRT-LM emits deltas, not snapshots: each callback carries
                        // only the latest chunk, so collectors must concatenate. If a
                        // future version switched to cumulative snapshots this would still
                        // compile but duplicate text at runtime. See docs/api/kotlin.
                        emit(text)
                    }
                } catch (e: CancellationException) {
                    conversation.cancelProcess()
                    throw e
                }
            }
        } finally {
            isGenerating.store(false)
        }
    }

    override suspend fun load() {
        _modelState.value = ModelState.Loading
        try {
            withContext(Dispatchers.IO) {
                engine = Engine(EngineConfig(modelPath = modelPath, cacheDir = cacheDir)).also {
                    it.initialize()
                }
                _modelState.value = ModelState.Ready
            }
        } catch (e: CancellationException) {
            _modelState.value = ModelState.Unloaded
            throw e
        } catch (e: Exception) {
            _modelState.value = ModelState.Error(e)
            throw e
        }
    }

    override suspend fun unload() {
        _modelState.value = ModelState.Unloaded
        withContext(Dispatchers.IO) {
            engine?.close()
        }
        engine = null
    }

}