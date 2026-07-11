package com.sumit.pocketgpt.domain.inference

sealed interface ModelState {
    data object Unloaded : ModelState
    data object Loading : ModelState
    data object Ready : ModelState
    data class Error(val cause: Throwable) : ModelState
}