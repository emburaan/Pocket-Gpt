package com.sumit.pocketgpt.data.repository

import com.sumit.pocketgpt.domain.model.ChatMessage
import com.sumit.pocketgpt.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor() : ChatRepository {

    override fun sendMessage(chatMessage: ChatMessage): Flow<String> {
        TODO("M2: delegate to InferenceEngine")
    }
}