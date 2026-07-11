package com.sumit.pocketgpt.domain.repository

import com.sumit.pocketgpt.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {

    fun sendMessage(chatMessage: ChatMessage): Flow<String>
}