package com.sumit.pocketgpt.presentation.chat

/** User-initiated actions the chat screen can dispatch to the ViewModel. */
sealed interface ChatIntent {
    data class SendMessage(val text: String) : ChatIntent
    data object RetrySetup : ChatIntent
}
