package com.sumit.pocketgpt.data.repository

import com.sumit.pocketgpt.data.local.ChatMessageDao
import com.sumit.pocketgpt.data.local.ConversationDao
import com.sumit.pocketgpt.data.local.ConversationEntity
import com.sumit.pocketgpt.data.local.toDomain
import com.sumit.pocketgpt.data.local.toEntity
import com.sumit.pocketgpt.domain.inference.InferenceEngine
import com.sumit.pocketgpt.domain.model.ChatMessage
import com.sumit.pocketgpt.domain.model.Conversation
import com.sumit.pocketgpt.domain.model.Role
import com.sumit.pocketgpt.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private const val TITLE_MAX_LENGTH = 40

class ChatRepositoryImpl @Inject constructor(
    private val inferenceEngine: InferenceEngine,
    private val chatMessageDao: ChatMessageDao,
    private val conversationDao: ConversationDao,
) : ChatRepository {

    override fun observeConversations(): Flow<List<Conversation>> {
        return conversationDao.observeConversations().map { entities -> entities.map { it.toDomain() } }
    }

    override fun observeConversation(conversationId: Long): Flow<Conversation?> {
        return conversationDao.observeConversation(conversationId).map { it?.toDomain() }
    }

    override suspend fun createConversation(): Long {
        val now = System.currentTimeMillis()
        return conversationDao.insert(
            ConversationEntity(title = "New Chat", createdAt = now, updatedAt = now)
        )
    }

    override fun observeMessages(conversationId: Long): Flow<List<ChatMessage>> {
        return chatMessageDao.observeMessages(conversationId).map { entities -> entities.map { it.toDomain() } }
    }

    override fun sendMessage(conversationId: Long, chatMessage: ChatMessage): Flow<String> = flow {
        val isFirstMessage = chatMessageDao.countMessages(conversationId) == 0
        chatMessageDao.insert(chatMessage.toEntity(conversationId))

        val now = System.currentTimeMillis()
        if (isFirstMessage) {
            conversationDao.updateTitleAndTimestamp(
                id = conversationId,
                title = chatMessage.content.take(TITLE_MAX_LENGTH),
                updatedAt = now,
            )
        } else {
            conversationDao.touch(conversationId, now)
        }

        val history = chatMessageDao.observeMessages(conversationId).first().map { it.toDomain() }

        val reply = StringBuilder()
        inferenceEngine.generate(history).collect { delta ->
            reply.append(delta)
            emit(delta)
        }

        chatMessageDao.insert(
            ChatMessage(
                role = Role.MODEL,
                content = reply.toString(),
                createdAt = System.currentTimeMillis(),
            ).toEntity(conversationId)
        )
    }
}
