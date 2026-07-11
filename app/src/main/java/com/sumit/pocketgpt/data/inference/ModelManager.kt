package com.sumit.pocketgpt.data.inference

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-wide holder for the on-device LLM engine.
 *
 * The wrapper is application-scoped so the multi-GB model is created at most
 * once per process, but the weights' lifetime is decoupled from the object's:
 * load/unload will be added in M3 so memory can be released under pressure
 * without losing the singleton.
 */
@Singleton
class ModelManager @Inject constructor() {

}