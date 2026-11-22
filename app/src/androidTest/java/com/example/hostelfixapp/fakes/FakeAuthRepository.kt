package com.example.hostelfixapp.fakes

import com.example.hostelfixapp.data.model.User
import com.example.hostelfixapp.data.model.UserRole
import com.example.hostelfixapp.data.repository.AuthRepository
import com.example.hostelfixapp.data.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAuthRepository(
    initialUser: User? = User("user1", "John Doe", "john@example.com", "ST123", UserRole.STUDENT, "Kenyatta", "Room 101")
) : AuthRepository {

    private val _currentUserFlow = MutableStateFlow<User?>(initialUser)
    override val currentUserFlow: StateFlow<User?> = _currentUserFlow.asStateFlow()

    fun setCurrentUser(user: User?) {
        _currentUserFlow.value = user
    }

    override fun getCurrentUser(): User? = _currentUserFlow.value

    override suspend fun login(email: String, password: String, rememberMe: Boolean): AuthResult {
        return if (email.contains("error")) {
            AuthResult.Error("Invalid credentials")
        } else {
            val baseUser = _currentUserFlow.value ?: User("user1", "John Doe", email, "ST123", UserRole.STUDENT, "Kenyatta", "Room 101")
            val user = baseUser.copy(email = email)
            _currentUserFlow.value = user
            AuthResult.Success(user, "Login successful")
        }
    }

    override suspend fun signUp(
        name: String,
        email: String,
        studentId: String,
        password: String,
        role: UserRole
    ): AuthResult {
        val newUser = User("user_new", name, email, studentId, role, "Kenyatta", "Room 101")
        _currentUserFlow.value = newUser
        return AuthResult.Success(newUser, "Sign up successful")
    }

    override suspend fun sendPasswordResetCode(email: String): AuthResult {
        return AuthResult.Success(User("id", "Name", email, "", UserRole.STUDENT, "", ""), "Reset code sent")
    }

    override suspend fun updateProfilePhoto(photoUrl: String?): AuthResult {
        val updated = _currentUserFlow.value?.copy(profilePicUrl = photoUrl)
        _currentUserFlow.value = updated
        val msg = if (photoUrl.isNullOrBlank()) "Profile photo removed successfully!" else "Profile picture updated successfully!"
        return if (updated != null) AuthResult.Success(updated, msg) else AuthResult.Error("No user logged in")
    }

    override suspend fun logout() {
        _currentUserFlow.value = null
    }
}
