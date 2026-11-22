package com.example.hostelfixapp.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hostelfixapp.data.model.ChatMessage
import com.example.hostelfixapp.data.repository.AuthRepository
import com.example.hostelfixapp.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isOnline: Boolean = true,
    val isSending: Boolean = false,
    val userMessage: String? = null
)

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            chatRepository.isOnlineState.collect { online ->
                _uiState.value = _uiState.value.copy(isOnline = online)
            }
        }
    }

    fun loadChatForReport(reportId: String) {
        viewModelScope.launch {
            chatRepository.getChatMessagesFlow(reportId).collect { msgs ->
                _uiState.value = _uiState.value.copy(messages = msgs)
            }
        }
    }

    fun sendMessage(reportId: String, text: String, photoUrl: String? = null) {
        val currentUser = authRepository.getCurrentUser() ?: return
        if (text.isBlank() && photoUrl == null) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true)
            val result = chatRepository.sendMessage(
                reportId = reportId,
                sender = currentUser,
                text = text,
                photoUrl = photoUrl
            )
            _uiState.value = _uiState.value.copy(isSending = false)

            if (result.isSuccess) {
                val isOnline = _uiState.value.isOnline
                val statusMsg = if (isOnline) "Message sent (Synced)" else "Message saved offline (Pending Sync on reconnect)"
                _uiState.value = _uiState.value.copy(userMessage = statusMsg)
            }
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            val count = chatRepository.syncPendingMessages()
            if (count > 0) {
                _uiState.value = _uiState.value.copy(userMessage = "Synced $count offline messages successfully!")
            }
        }
    }

    fun clearUserMessage() {
        _uiState.value = _uiState.value.copy(userMessage = null)
    }
}
