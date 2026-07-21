package com.sumit.pocketgpt.di

import android.content.Context
import com.sumit.pocketgpt.data.inference.InferenceEngineImpl
import com.sumit.pocketgpt.domain.inference.InferenceEngine
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InferenceModule {

    @Binds
    @Singleton
    abstract fun bindInferenceEngine(
        inferenceEngine: InferenceEngineImpl
    ): InferenceEngine

    companion object {
        /**
         * Dev bring-up path: sideloaded via `adb push`. Replaced by a
         * downloads-to-filesDir source when in-app model management lands.
         */
        @Provides
        @ModelPath
        fun provideModelPath(): String = "/data/local/tmp/llm/gemma-4-e2b.litertlm"

        /**
         * App-private, always-writable dir for the engine's compiled-weights
         * cache. Distinct from [provideModelPath]'s read-only sideload location,
         * which the app cannot write to.
         */
        @Provides
        @CacheDir
        fun provideCacheDir(@ApplicationContext context: Context): String =
            context.cacheDir.absolutePath
    }
}