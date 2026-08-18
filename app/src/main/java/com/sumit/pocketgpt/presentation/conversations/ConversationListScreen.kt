package com.sumit.pocketgpt.presentation.conversations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sumit.pocketgpt.domain.model.Conversation
import com.sumit.pocketgpt.ui.theme.PocketGPTTheme

/**
 * Stateless list screen: renders whatever conversations it is given and reports
 * clicks. Owns no business state, same shape as ChatScreen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
    conversations: List<Conversation>,
    onConversationClick: (Long) -> Unit,
    onNewChatClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("PocketGPT") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewChatClick) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "New chat")
            }
        },
    ) { innerPadding ->
        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No chats yet — tap + to start one.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(conversations, key = { it.id }) { conversation ->
                    Card(
                        onClick = { onConversationClick(conversation.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        ListItem(headlineContent = { Text(conversation.title) })
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConversationListScreenPreview() {
    PocketGPTTheme {
        ConversationListScreen(
            conversations = listOf(
                Conversation(id = 1, title = "What runs the model on device?", createdAt = 0L, updatedAt = 0L),
                Conversation(id = 2, title = "New Chat", createdAt = 0L, updatedAt = 0L),
            ),
            onConversationClick = {},
            onNewChatClick = {},
        )
    }
}
