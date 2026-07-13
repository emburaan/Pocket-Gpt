package com.sumit.pocketgpt.presentation.main

import androidx.lifecycle.ViewModel
import com.sumit.pocketgpt.data.inference.ModelManager
import com.sumit.pocketgpt.domain.inference.ModelState
import com.sumit.pocketgpt.domain.model.ChatMessage
import com.sumit.pocketgpt.domain.model.Role
import com.sumit.pocketgpt.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val modelManager: ModelManager
) : ViewModel() {

    private val chatMessage: ChatMessage = ChatMessage(Role.USER, "Hello AI World", 11111111)
    private val _uiState =
        MutableStateFlow(MainUiState(chatMessage = chatMessage, modelState = ModelState.Unloaded))
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
}