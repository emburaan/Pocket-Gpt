package com.sumit.pocketgpt.di

import javax.inject.Qualifier

/**
 * Qualifies the app-writable directory the inference engine uses for its
 * compiled-weights cache, distinct from where the (read-only) model lives.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CacheDir
