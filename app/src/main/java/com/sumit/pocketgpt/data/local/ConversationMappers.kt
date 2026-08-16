package com.sumit.pocketgpt.data.local

import com.sumit.pocketgpt.domain.model.Conversation

internal fun ConversationEntity.toDomain(): Conversation {
    return Conversation(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
