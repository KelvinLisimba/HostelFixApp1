package com.example.hostelfixapp.ui

import com.example.hostelfixapp.data.model.UserRole
import com.example.hostelfixapp.fakes.FakeAuthRepository
import com.example.hostelfixapp.ui.auth.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepository = FakeAuthRepository()
        viewModel = AuthViewModel(fakeAuthRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun login_withEmptyFields_showsValidationError() {
        viewModel.login(email = "", pass = "", rememberMe = false, onSuccess = {})

        val uiState = viewModel.uiState.value
        assertEquals("Please enter both email and password.", uiState.errorMessage)
    }

    @Test
    fun login_withValidData_succeeds() {
        var successCalled = false

        viewModel.login(email = "student@example.com", pass = "password123", rememberMe = true) {
            successCalled = true
        }

        val uiState = viewModel.uiState.value
        assertTrue(successCalled)
        assertEquals("Login successful", uiState.successMessage)
        assertNull(uiState.errorMessage)
    }

    @Test
    fun login_withErrorEmail_showsErrorMessage() {
        viewModel.login(email = "error_user@example.com", pass = "wrong", rememberMe = false) {}

        val uiState = viewModel.uiState.value
        assertEquals("Invalid credentials", uiState.errorMessage)
    }

    @Test
    fun signUp_createsNewUserAccount() {
        var successCalled = false

        viewModel.signUp(
            name = "New Student",
            email = "newstudent@example.com",
            studentId = "ST_99",
            pass = "Secret123!",
            role = UserRole.STUDENT,
            onSuccess = { successCalled = true }
        )

        val uiState = viewModel.uiState.value
        assertTrue(successCalled)
        assertEquals("Sign up successful", uiState.successMessage)
        assertEquals("New Student", uiState.currentUser?.name)
    }

    @Test
    fun logout_clearsCurrentUser() {
        var logoutCalled = false

        viewModel.logout { logoutCalled = true }

        val uiState = viewModel.uiState.value
        assertTrue(logoutCalled)
        assertNull(uiState.currentUser)
    }
}
