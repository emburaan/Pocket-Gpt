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
    val isGenerating: Boolean = false,
) {
    val isModelReady: Boolean get() = modelState == ModelState.Ready

    /** Whether the input bar's send action should be enabled — the View renders this, doesn't decide it. */
    val sendEnabled: Boolean get() = isModelReady && streamingReply == null

    /** Whether the blocking setup dialog should show — the View renders this, doesn't decide it. */
    val showSetupDialog: Boolean get() = !isModelReady
}
