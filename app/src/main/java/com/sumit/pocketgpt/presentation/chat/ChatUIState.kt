package com.sumit.pocketgpt.presentation.chat

import com.sumit.pocketgpt.domain.inference.ModelState
import com.sumit.pocketgpt.domain.model.ChatMessage

data class ChatUIState(
    val modelState: ModelState,
    val chatMessages: List<ChatMessage>,
    val streamingReply: String? = null,
)