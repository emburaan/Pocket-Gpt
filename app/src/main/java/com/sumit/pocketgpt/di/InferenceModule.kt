package com.sumit.pocketgpt.di

import com.sumit.pocketgpt.data.inference.FakeInferenceEngine
import com.sumit.pocketgpt.domain.inference.InferenceEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InferenceModule {

    @Binds
    @Singleton
    abstract fun bindInferenceEngine(
        inferenceEngine: FakeInferenceEngine
    ): InferenceEngine
}