package com.sumit.pocketgpt.presentation.main

import com.sumit.pocketgpt.domain.inference.ModelState
import com.sumit.pocketgpt.domain.model.ChatMessage

data class MainUiState(
    val modelState: ModelState,
    val chatMessage: ChatMessage
)