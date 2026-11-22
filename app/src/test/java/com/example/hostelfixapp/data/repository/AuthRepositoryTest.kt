package com.example.hostelfixapp.data.repository

import com.example.hostelfixapp.data.model.UserRole
import com.example.hostelfixapp.fakes.FakeAuthRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryTest {

    private lateinit var repository: FakeAuthRepository

    @Before
    fun setUp() {
        repository = FakeAuthRepository()
    }

    @Test
    fun login_withValidCredentials_returnsSuccessResult() = runTest {
        val result = repository.login("student@example.com", "pass123")
        assertTrue(result is AuthResult.Success)
        val success = result as AuthResult.Success
        assertEquals("student@example.com", success.user.email)
    }

    @Test
    fun login_withInvalidCredentials_returnsErrorResult() = runTest {
        val result = repository.login("error_user@example.com", "pass123")
        assertTrue(result is AuthResult.Error)
        val error = result as AuthResult.Error
        assertEquals("Invalid credentials", error.message)
    }

    @Test
    fun signUp_registersNewUserAccount() = runTest {
        val result = repository.signUp(
            name = "New User",
            email = "newuser@example.com",
            studentId = "ST_88",
            password = "password",
            role = UserRole.STUDENT
        )

        assertTrue(result is AuthResult.Success)
        val success = result as AuthResult.Success
        assertEquals("New User", success.user.name)
        assertEquals("newuser@example.com", success.user.email)
    }

    @Test
    fun updateProfilePhoto_updatesUserPhotoUrl() = runTest {
        val result = repository.updateProfilePhoto("file://new_photo.jpg")
        assertTrue(result is AuthResult.Success)
        assertEquals("file://new_photo.jpg", repository.getCurrentUser()?.profilePicUrl)
    }

    @Test
    fun logout_clearsCurrentUser() = runTest {
        repository.logout()
        assertNull(repository.getCurrentUser())
    }
}
