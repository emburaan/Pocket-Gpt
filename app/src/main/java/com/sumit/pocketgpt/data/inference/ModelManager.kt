package com.sumit.pocketgpt.data.inference

import com.sumit.pocketgpt.domain.inference.InferenceEngine
import com.sumit.pocketgpt.domain.inference.ModelDownloader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-wide holder for the on-device LLM engine.
 *
 * The wrapper is application-scoped so the multi-GB model is created at most
 * once per process, but the weights' lifetime is decoupled from the object's:
 * load/unload can be called so memory can be released under pressure
 * without losing the singleton.
 */
@Singleton
class ModelManager @Inject constructor(
    private val inferenceEngine: InferenceEngine,
    private val modelDownloader: ModelDownloader,
) {

    val modelState = inferenceEngine.modelState
    val downloadState = modelDownloader.downloadState

    /**
     * Downloads the model checkpoint if it isn't already on disk, then loads
     * it. Download and load failures are surfaced through separate state
     * ([downloadState] vs [modelState]) rather than conflated into one —
     * a missing/corrupt download and an engine init failure need different
     * recovery (retry download vs. something else entirely), and a caller
     * watching only [modelState] can't tell them apart otherwise.
     */
    suspend fun load() {
        modelDownloader.ensureModelDownloaded()
        inferenceEngine.load()
    }

    suspend fun unload() = inferenceEngine.unload()

}