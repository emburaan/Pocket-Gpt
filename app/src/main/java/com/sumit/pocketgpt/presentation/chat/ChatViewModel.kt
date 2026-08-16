package com.sumit.pocketgpt.presentation.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumit.pocketgpt.data.inference.ModelManager
import com.sumit.pocketgpt.domain.inference.ModelState
import com.sumit.pocketgpt.domain.model.ChatMessage
import com.sumit.pocketgpt.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
    private val modelManager: ModelManager,
) : ViewModel() {

    private val conversationId: Long = checkNotNull(savedStateHandle["conversationId"])

    private val _uiState =
        MutableStateFlow(
            ChatUIState(
                chatMessages = listOf(),
                modelState = ModelState.Unloaded,
            )
        )

    init {
        viewModelScope.launch {
            modelManager.modelState.collect { newState ->
                _uiState.update { it.copy(modelState = newState) }
            }
        }
        viewModelScope.launch {
            modelManager.load()
        }
        // Repo is the source of truth for message history — mirror it instead of
        // maintaining a second, drift-prone copy locally.
        viewModelScope.launch {
            chatRepository.observeMessages(conversationId).collect { messages ->
                _uiState.update { it.copy(chatMessages = messages) }
            }
        }
        viewModelScope.launch {
            chatRepository.observeConversation(conversationId).collect { conversation ->
                _uiState.update { it.copy(conversationTitle = conversation?.title ?: "") }
            }
        }
    }

    val uiState: StateFlow<ChatUIState> = _uiState.asStateFlow()

    fun sendMessage(chatMessage: ChatMessage) {
        // Guard: the engine's contract is one-generation-at-a-time — a concurrent
        // collection throws IllegalStateException and "does not queue". Drop-policy:
        // ignore a send issued while one is already in flight.
        if (_uiState.value.isGenerating) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGenerating = true,
                    showError = false,
                )
            }
            if (modelManager.modelState.value != ModelState.Ready) {
                modelManager.load()
            }
            try {
                // Per-token: append each delta to the in-flight reply. The persisted
                // user + model messages arrive via the observeMessages() mirror above.
                chatRepository.sendMessage(conversationId, chatMessage).collect { reply ->
                    _uiState.update {
                        it.copy(streamingReply = (it.streamingReply ?: "") + reply)
                    }
                }
            } catch (_: IllegalStateException) {
                // Documented failure (not loaded / concurrent). Surface, don't crash.
                _uiState.update { it.copy(showError = true) }
            } finally {
                // Cleanup only — runs on success, failure, and cancellation.
                _uiState.update {
                    it.copy(streamingReply = null, isGenerating = false)
                }
            }
        }
    }
}
