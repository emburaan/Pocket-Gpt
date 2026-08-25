package com.sumit.pocketgpt.presentation.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sumit.pocketgpt.domain.inference.ModelState
import com.sumit.pocketgpt.domain.model.ChatMessage
import com.sumit.pocketgpt.domain.model.Role
import java.util.Date

@Composable
fun ChatRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        ChatScreen(
            messages = uiState.chatMessages + listOfNotNull(
                uiState.streamingReply?.let { reply ->
                    ChatMessage(role = Role.MODEL, content = reply, createdAt = 0L)
                }
            ),
            onSend = { typedText ->
                viewModel.sendMessage(
                    ChatMessage(role = Role.USER, content = typedText, createdAt = Date().time)
                )
            },
            onBack = onBack,
            title = uiState.conversationTitle,
            sendEnabled = uiState.modelState == ModelState.Ready && uiState.streamingReply == null,
        )

        if (uiState.modelState != ModelState.Ready) {
            ModelSetupDialog(
                modelState = uiState.modelState,
                downloadState = uiState.downloadState,
                onRetry = viewModel::retrySetup,
            )
        }
    }
}
