package com.sumit.pocketgpt.presentation.chat

import com.sumit.pocketgpt.domain.inference.DownloadState
import com.sumit.pocketgpt.domain.inference.ModelState
import com.sumit.pocketgpt.domain.model.ChatMessage

data class ChatUIState(
    val modelState: ModelState,
    val chatMessages: List<ChatMessage>,
    val downloadState: DownloadState = DownloadState.NotStarted,
    val conversationTitle: String = "",
    val streamingReply: String? = null,
    val showError: Boolean = false,
    val isGenerating: Boolean = false,
)