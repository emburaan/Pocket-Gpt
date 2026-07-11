package com.sumit.pocketgpt.data.repository

import com.sumit.pocketgpt.data.inference.ModelManager
import com.sumit.pocketgpt.domain.repository.ChatRepository
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val modelManager: ModelManager
) : ChatRepository {

    override fun getModelStatus(): String {
        return modelManager.status
    }
}