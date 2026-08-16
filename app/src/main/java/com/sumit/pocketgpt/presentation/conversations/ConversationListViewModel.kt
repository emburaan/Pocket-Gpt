package com.sumit.pocketgpt.presentation.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumit.pocketgpt.domain.model.Conversation
import com.sumit.pocketgpt.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
) : ViewModel() {

    val conversations: StateFlow<List<Conversation>> =
        chatRepository.observeConversations()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _navigateToConversation = Channel<Long>(Channel.BUFFERED)
    val navigateToConversation: Flow<Long> = _navigateToConversation.receiveAsFlow()

    fun onNewChatClick() {
        viewModelScope.launch {
            val conversationId = chatRepository.createConversation()
            _navigateToConversation.send(conversationId)
        }
    }
}
