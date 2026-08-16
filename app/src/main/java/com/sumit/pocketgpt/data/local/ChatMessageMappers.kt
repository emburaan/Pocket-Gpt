package com.sumit.pocketgpt.data.local

import com.sumit.pocketgpt.domain.model.ChatMessage
import com.sumit.pocketgpt.domain.model.Role

internal fun ChatMessageEntity.toDomain(): ChatMessage {
    return ChatMessage(
        role = Role.valueOf(role),
        content = content,
        createdAt = createdAt,
    )
}

internal fun ChatMessage.toEntity(conversationId: Long): ChatMessageEntity {
    return ChatMessageEntity(
        conversationId = conversationId,
        role = role.name,
        content = content,
        createdAt = createdAt,
    )
}
