package com.example.hostelfixapp.fakes

import com.example.hostelfixapp.data.model.ChatMessage
import com.example.hostelfixapp.data.model.SyncState
import com.example.hostelfixapp.data.model.User
import com.example.hostelfixapp.data.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class FakeChatRepository : ChatRepository {

    private val _isOnlineState = MutableStateFlow(true)
    override val isOnlineState: StateFlow<Boolean> = _isOnlineState.asStateFlow()

    private val messagesList = mutableListOf<ChatMessage>()
    private val messagesFlow = MutableStateFlow<List<ChatMessage>>(emptyList())

    fun setOnline(online: Boolean) {
        _isOnlineState.value = online
    }

    override fun getChatMessagesFlow(reportId: String): Flow<List<ChatMessage>> {
        return messagesFlow.asStateFlow().map { msgs ->
            msgs.filter { it.reportId == reportId }
        }
    }

    override suspend fun sendMessage(
        reportId: String,
        sender: User,
        text: String,
        photoUrl: String?
    ): Result<ChatMessage> {
        val syncState = if (_isOnlineState.value) SyncState.SYNCED else SyncState.PENDING_SYNC
        val message = ChatMessage(
            messageId = "MSG_${System.currentTimeMillis()}",
            reportId = reportId,
            senderId = sender.id,
            senderName = sender.name,
            senderRole = sender.role,
            text = text,
            photoUrl = photoUrl,
            timestamp = System.currentTimeMillis(),
            syncState = syncState
        )
        messagesList.add(message)
        messagesFlow.value = messagesList.toList()
        return Result.success(message)
    }

    override suspend fun syncPendingMessages(): Int {
        var count = 0
        val updated = messagesList.map { msg ->
            if (msg.syncState == SyncState.PENDING_SYNC) {
                count++
                msg.copy(syncState = SyncState.SYNCED)
            } else {
                msg
            }
        }
        messagesList.clear()
        messagesList.addAll(updated)
        messagesFlow.value = messagesList.toList()
        return count
    }
}
