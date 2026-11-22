package com.example.hostelfixapp.data.model

enum class SyncState {
    SYNCED,
    PENDING_SYNC
}

data class ChatMessage(
    val messageId: String = "",
    val reportId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderRole: UserRole = UserRole.STUDENT,
    val text: String = "",
    val photoUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val syncState: SyncState = SyncState.SYNCED
)
