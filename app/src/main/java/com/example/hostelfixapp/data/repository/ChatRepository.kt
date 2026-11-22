package com.example.hostelfixapp.data.repository

import com.example.hostelfixapp.data.model.ChatMessage
import com.example.hostelfixapp.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ChatRepository {
    val isOnlineState: StateFlow<Boolean>
    fun getChatMessagesFlow(reportId: String): Flow<List<ChatMessage>>
    suspend fun sendMessage(
        reportId: String,
        sender: User,
        text: String,
        photoUrl: String? = null
    ): Result<ChatMessage>
    suspend fun syncPendingMessages(): Int
}
