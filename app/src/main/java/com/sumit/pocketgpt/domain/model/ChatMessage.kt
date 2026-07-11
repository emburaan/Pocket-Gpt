package com.sumit.pocketgpt.domain.model

data class ChatMessage(
    val role: Role,
    val content: String,
    val createdAt: Long
)

enum class Role {
    USER, MODEL
}
