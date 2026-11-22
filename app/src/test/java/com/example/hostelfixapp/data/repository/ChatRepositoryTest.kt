package com.example.hostelfixapp.data.repository

import com.example.hostelfixapp.data.model.SyncState
import com.example.hostelfixapp.data.model.User
import com.example.hostelfixapp.data.model.UserRole
import com.example.hostelfixapp.fakes.FakeChatRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatRepositoryTest {

    private lateinit var repository: FakeChatRepository
    private val testUser = User("u1", "Student Alex", "alex@example.com", "ST1", UserRole.STUDENT, "Kenyatta", "Room 1")

    @Before
    fun setUp() {
        repository = FakeChatRepository()
    }

    @Test
    fun sendMessage_whenOnline_sendsMessageWithSyncedStatus() = runTest {
        repository.setOnline(true)

        val result = repository.sendMessage("REP_101", testUser, "Hello maintenance")
        assertTrue(result.isSuccess)

        val messages = repository.getChatMessagesFlow("REP_101").first()
        assertEquals(1, messages.size)
        assertEquals("Hello maintenance", messages[0].text)
        assertEquals(SyncState.SYNCED, messages[0].syncState)
    }

    @Test
    fun sendMessage_whenOffline_queuesMessageWithPendingSyncStatus() = runTest {
        repository.setOnline(false)

        val result = repository.sendMessage("REP_101", testUser, "Offline photo attached", photoUrl = "file://photo.jpg")
        assertTrue(result.isSuccess)

        val messages = repository.getChatMessagesFlow("REP_101").first()
        assertEquals(1, messages.size)
        assertEquals(SyncState.PENDING_SYNC, messages[0].syncState)
    }

    @Test
    fun syncPendingMessages_syncsQueuedMessagesWhenReconnected() = runTest {
        repository.setOnline(false)
        repository.sendMessage("REP_101", testUser, "Offline message 1")
        repository.sendMessage("REP_101", testUser, "Offline message 2")

        repository.setOnline(true)
        val syncedCount = repository.syncPendingMessages()

        assertEquals(2, syncedCount)
        val messages = repository.getChatMessagesFlow("REP_101").first()
        assertTrue(messages.all { it.syncState == SyncState.SYNCED })
    }
}
