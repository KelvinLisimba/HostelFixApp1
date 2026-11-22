package com.example.hostelfixapp.ui

import com.example.hostelfixapp.data.model.User
import com.example.hostelfixapp.data.model.UserRole
import com.example.hostelfixapp.fakes.FakeAuthRepository
import com.example.hostelfixapp.fakes.FakeChatRepository
import com.example.hostelfixapp.ui.chat.ChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeChatRepository: FakeChatRepository
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeChatRepository = FakeChatRepository()
        fakeAuthRepository = FakeAuthRepository(
            User("student1", "Alex Student", "alex@example.com", "ST123", UserRole.STUDENT, "Kenyatta", "Room 101")
        )
        viewModel = ChatViewModel(fakeChatRepository, fakeAuthRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun sendMessage_whenOnline_sendsAndNotifiesSynced() {
        viewModel.loadChatForReport("REP_001")
        viewModel.sendMessage("REP_001", "Hello maintenance team")

        val uiState = viewModel.uiState.value
        assertEquals("Message sent (Synced)", uiState.userMessage)
        assertEquals(1, uiState.messages.size)
        assertEquals("Hello maintenance team", uiState.messages[0].text)
    }

    @Test
    fun sendMessage_whenOffline_sendsAndQueuesOffline() {
        fakeChatRepository.setOnline(false)
        viewModel.loadChatForReport("REP_001")
        viewModel.sendMessage("REP_001", "Offline damage photo attached", photoUrl = "file://photo.jpg")

        val uiState = viewModel.uiState.value
        assertEquals("Message saved offline (Pending Sync on reconnect)", uiState.userMessage)
        assertEquals(1, uiState.messages.size)
    }

    @Test
    fun triggerSync_syncsOfflinePendingMessages() {
        fakeChatRepository.setOnline(false)
        viewModel.sendMessage("REP_001", "Message 1")

        fakeChatRepository.setOnline(true)
        viewModel.triggerSync()

        val uiState = viewModel.uiState.value
        assertTrue(uiState.userMessage?.contains("Synced 1 offline messages") == true)
    }
}
