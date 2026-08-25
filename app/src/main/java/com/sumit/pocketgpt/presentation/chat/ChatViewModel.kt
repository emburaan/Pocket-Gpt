package com.sumit.pocketgpt.presentation.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumit.pocketgpt.data.inference.ModelManager
import com.sumit.pocketgpt.domain.inference.ModelState
import com.sumit.pocketgpt.domain.model.ChatMessage
import com.sumit.pocketgpt.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
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
            modelManager.downloadState.collect { newState ->
                _uiState.update { it.copy(downloadState = newState) }
            }
        }
        // Guard: ModelManager is app-scoped and may already be Ready from a
        // previous chat screen — every conversation gets its own ChatViewModel,
        // so an unconditional load() here would re-init the multi-GB engine
        // (and re-show the setup dialog) on every navigation.
        viewModelScope.launch {
            if (modelManager.modelState.value != ModelState.Ready) {
                loadModel()
            }
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

    /** Re-runs setup (download + load) after a failure. */
    fun retrySetup() {
        viewModelScope.launch { loadModel() }
    }

    // Download/load failures already reach the UI via downloadState/modelState —
    // swallowing the exception here (after letting cancellation through) just
    // stops it from also crashing the app as an uncaught coroutine exception.
    private suspend fun loadModel() {
        try {
            modelManager.load()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // handled via state
        }
    }

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
                loadModel()
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
