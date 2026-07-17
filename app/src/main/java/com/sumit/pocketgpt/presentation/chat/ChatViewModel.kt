package com.sumit.pocketgpt.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumit.pocketgpt.data.inference.ModelManager
import com.sumit.pocketgpt.domain.inference.ModelState
import com.sumit.pocketgpt.domain.model.ChatMessage
import com.sumit.pocketgpt.domain.model.Role
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
    private val chatRepository: ChatRepository,
    private val modelManager: ModelManager
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            ChatUIState(
                chatMessages = listOf(),
                modelState = ModelState.Unloaded
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
    }

    val uiState: StateFlow<ChatUIState> = _uiState.asStateFlow()

    fun sendMessage(chatMessage: ChatMessage) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    chatMessages = it.chatMessages + chatMessage
                )
            }
            if (modelManager.modelState.value != ModelState.Ready) {
                modelManager.load()
            }
            chatRepository.sendMessage(chatMessage).collect { reply ->
                _uiState.update { chatUIState ->
                    chatUIState.copy(
                        streamingReply = (chatUIState.streamingReply ?: "") + reply
                    )
                }
            }
            _uiState.update {
                it.copy(
                    chatMessages = it.chatMessages + ChatMessage(
                        role = Role.MODEL,
                        content = it.streamingReply ?: "",
                        createdAt = System.currentTimeMillis()
                    ),
                    streamingReply = null
                )
            }
        }
    }
}