package com.sumit.pocketgpt.presentation.conversations

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ConversationListRoute(
    onConversationClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConversationListViewModel = hiltViewModel(),
) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigateToConversation.collect { conversationId ->
            onConversationClick(conversationId)
        }
    }

    ConversationListScreen(
        conversations = conversations,
        onConversationClick = onConversationClick,
        onNewChatClick = viewModel::onNewChatClick,
        modifier = modifier,
    )
}
