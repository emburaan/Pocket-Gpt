package com.sumit.pocketgpt.di

import javax.inject.Qualifier

/**
 * Qualifies the app-writable directory the inference engine uses for its
 * compiled-weights cache, distinct from [ModelPath]'s model-file destination.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CacheDir
