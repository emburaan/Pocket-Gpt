package com.sumit.pocketgpt.di

import javax.inject.Qualifier

/**
 * Qualifies the remote URL the on-device model checkpoint is downloaded
 * from, so a plain [String] binding can't collide with any other string in
 * the graph.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ModelUrl
