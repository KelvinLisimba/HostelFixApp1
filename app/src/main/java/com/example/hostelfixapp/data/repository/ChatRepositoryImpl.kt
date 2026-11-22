package com.example.hostelfixapp.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.hostelfixapp.data.local.HostelFixLocalDb
import com.example.hostelfixapp.ui.components.HostelFixNotificationHelper
import com.example.hostelfixapp.data.model.ChatMessage
import com.example.hostelfixapp.data.model.SyncState
import com.example.hostelfixapp.data.model.User
import com.example.hostelfixapp.data.model.UserRole
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ChatRepositoryImpl(private val context: Context) : ChatRepository {

    private val localDb = HostelFixLocalDb(context)
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val _isOnlineState = MutableStateFlow(isNetworkAvailable(context))
    override val isOnlineState: StateFlow<Boolean> = _isOnlineState.asStateFlow()

    private val _messagesFlow = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        // Monitor network connectivity
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOnlineState.value = true
                scope.launch { syncPendingMessages() }
            }

            override fun onLost(network: Network) {
                _isOnlineState.value = false
            }
        })
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    override fun getChatMessagesFlow(reportId: String): Flow<List<ChatMessage>> {
        val list = localDb.getChatMessagesForReport(reportId)
        _messagesFlow.value = list

        // Real-Time Firebase Firestore Chat Listener for this report
        listenForRealtimeChat(reportId)

        return _messagesFlow.asStateFlow().map {
            localDb.getChatMessagesForReport(reportId)
        }
    }

    private fun listenForRealtimeChat(reportId: String) {
        try {
            firestore.collection("reports")
                .document(reportId)
                .collection("messages")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener

                    for (doc in snapshot.documents) {
                        try {
                            val senderId = doc.getString("senderId") ?: ""
                            val msg = ChatMessage(
                                messageId = doc.getString("messageId") ?: doc.id,
                                reportId = doc.getString("reportId") ?: reportId,
                                senderId = senderId,
                                senderName = doc.getString("senderName") ?: "",
                                senderRole = try {
                                    UserRole.valueOf(doc.getString("senderRole") ?: UserRole.STUDENT.name)
                                } catch (_: Exception) { UserRole.STUDENT },
                                text = doc.getString("text") ?: "",
                                photoUrl = doc.getString("photoUrl"),
                                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                                syncState = SyncState.SYNCED
                            )
                            localDb.saveChatMessage(msg)

                            val roleName = if (msg.senderRole == UserRole.STUDENT) "Student" else "Staff"
                            HostelFixNotificationHelper.showPushNotification(
                                context = context,
                                title = "💬 New Message from ${msg.senderName} ($roleName)",
                                messageText = msg.text.ifBlank { "Attached a damage photo" },
                                reportId = reportId
                            )
                        } catch (_: Exception) {}
                    }
                    notifyObservers(reportId)
                }
        } catch (_: Exception) {}
    }

    override suspend fun sendMessage(
        reportId: String,
        sender: User,
        text: String,
        photoUrl: String?
    ): Result<ChatMessage> {
        val isOnline = isNetworkAvailable(context)
        _isOnlineState.value = isOnline
        val syncState = if (isOnline) SyncState.SYNCED else SyncState.PENDING_SYNC

        val msg = ChatMessage(
            messageId = "msg_${System.currentTimeMillis()}_${sender.id}",
            reportId = reportId,
            senderId = sender.id,
            senderName = sender.name,
            senderRole = sender.role,
            text = text.trim(),
            photoUrl = photoUrl,
            timestamp = System.currentTimeMillis(),
            syncState = syncState
        )

        localDb.saveChatMessage(msg)

        if (isOnline) {
            localDb.markMessageAsSynced(msg.messageId)
            saveMessageToFirestore(msg)
        }

        notifyObservers(reportId)
        return Result.success(msg)
    }

    override suspend fun syncPendingMessages(): Int {
        val pendingList = localDb.getPendingSyncMessages()
        var syncedCount = 0

        for (msg in pendingList) {
            saveMessageToFirestore(msg)
            localDb.markMessageAsSynced(msg.messageId)
            syncedCount++
        }

        if (pendingList.isNotEmpty()) {
            notifyObservers(pendingList.first().reportId)
        }

        return syncedCount
    }

    private fun saveMessageToFirestore(msg: ChatMessage) {
        try {
            val msgMap = mapOf(
                "messageId" to msg.messageId,
                "reportId" to msg.reportId,
                "senderId" to msg.senderId,
                "senderName" to msg.senderName,
                "senderRole" to msg.senderRole.name,
                "text" to msg.text,
                "photoUrl" to (msg.photoUrl ?: ""),
                "timestamp" to msg.timestamp
            )
            firestore.collection("reports")
                .document(msg.reportId)
                .collection("messages")
                .document(msg.messageId)
                .set(msgMap)
        } catch (_: Exception) {}
    }

    private fun notifyObservers(reportId: String) {
        val updatedList = localDb.getChatMessagesForReport(reportId)
        _messagesFlow.value = updatedList
    }
}
