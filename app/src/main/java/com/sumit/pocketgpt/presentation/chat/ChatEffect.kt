package com.sumit.pocketgpt.presentation.chat

/**
 * One-time events the chat screen should react to once — never persisted in
 * [ChatUIState], since a state flag for a one-shot event either sticks
 * across recompositions or needs its own ad hoc reset logic.
 */
sealed interface ChatEffect {
    data object ShowSendError : ChatEffect
}
