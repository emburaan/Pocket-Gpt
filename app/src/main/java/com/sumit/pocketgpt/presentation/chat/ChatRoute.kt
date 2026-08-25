package com.sumit.pocketgpt.presentation.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sumit.pocketgpt.domain.model.ChatMessage
import com.sumit.pocketgpt.domain.model.Role

@Composable
fun ChatRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ChatEffect.ShowSendError -> snackbarHostState.showSnackbar("Couldn't send message")
            }
        }
    }

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
            onSend = { typedText -> viewModel.onIntent(ChatIntent.SendMessage(typedText)) },
            onBack = onBack,
            title = uiState.conversationTitle,
            sendEnabled = uiState.sendEnabled,
            snackbarHostState = snackbarHostState,
        )

        if (uiState.showSetupDialog) {
            ModelSetupDialog(
                modelState = uiState.modelState,
                downloadState = uiState.downloadState,
                onRetry = { viewModel.onIntent(ChatIntent.RetrySetup) },
            )
        }
    }
}
