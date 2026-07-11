package com.sumit.pocketgpt.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Application-scoped bindings. Interface-to-implementation bindings
 * (e.g. the InferenceEngine abstraction) land here from M1 onward.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule