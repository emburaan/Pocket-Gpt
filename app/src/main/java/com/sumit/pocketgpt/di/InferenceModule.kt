package com.sumit.pocketgpt.di

import android.content.Context
import com.sumit.pocketgpt.data.inference.InferenceEngineImpl
import com.sumit.pocketgpt.data.inference.ModelDownloaderImpl
import com.sumit.pocketgpt.domain.inference.InferenceEngine
import com.sumit.pocketgpt.domain.inference.ModelDownloader
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InferenceModule {

    @Binds
    @Singleton
    abstract fun bindInferenceEngine(
        inferenceEngine: InferenceEngineImpl
    ): InferenceEngine

    @Binds
    @Singleton
    abstract fun bindModelDownloader(
        modelDownloader: ModelDownloaderImpl
    ): ModelDownloader

    companion object {
        private const val MODEL_FILE_NAME = "gemma-4-E2B-it.litertlm"

        /**
         * Public, ungated `litert-community` mirror on Hugging Face — verified
         * (curl -IL) to serve the file directly over an unauthenticated
         * request rather than redirecting to a license-gate page.
         */
        @Provides
        @ModelUrl
        fun provideModelUrl(): String =
            "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/$MODEL_FILE_NAME?download=true"

        /**
         * App-private download destination. Replaces the old `adb push`-only
         * sideload path so the app is self-sufficient on first run: see
         * [ModelDownloader.ensureModelDownloaded].
         */
        @Provides
        @ModelPath
        fun provideModelPath(@ApplicationContext context: Context): String =
            File(context.filesDir, "models/$MODEL_FILE_NAME").absolutePath

        /**
         * App-private, always-writable dir for the engine's compiled-weights
         * cache. Distinct from [provideModelPath]'s download destination.
         */
        @Provides
        @CacheDir
        fun provideCacheDir(@ApplicationContext context: Context): String =
            context.cacheDir.absolutePath
    }
}