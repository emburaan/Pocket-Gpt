package com.sumit.pocketgpt.di

import javax.inject.Qualifier

/**
 * Qualifies the filesystem path of the on-device LLM checkpoint, so a plain
 * [String] binding can't collide with any other string in the graph.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ModelPath