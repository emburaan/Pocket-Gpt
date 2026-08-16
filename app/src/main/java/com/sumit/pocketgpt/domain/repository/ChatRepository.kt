package com.sumit.pocketgpt.domain.repository

import com.sumit.pocketgpt.domain.model.ChatMessage
import com.sumit.pocketgpt.domain.model.Conversation
import kotlinx.coroutines.flow.Flow

interface ChatRepository {

    fun observeConversations(): Flow<List<Conversation>>

    fun observeConversation(conversationId: Long): Flow<Conversation?>

    suspend fun createConversation(): Long

    fun observeMessages(conversationId: Long): Flow<List<ChatMessage>>

    fun sendMessage(conversationId: Long, chatMessage: ChatMessage): Flow<String>
}
