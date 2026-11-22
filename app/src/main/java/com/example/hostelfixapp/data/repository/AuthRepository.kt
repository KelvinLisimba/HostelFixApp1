package com.example.hostelfixapp.data.repository

import com.example.hostelfixapp.data.model.User
import com.example.hostelfixapp.data.model.UserRole
import kotlinx.coroutines.flow.StateFlow

sealed class AuthResult {
    data class Success(val user: User, val successMessage: String = "") : AuthResult()
    data class Error(val message: String) : AuthResult()
}

interface AuthRepository {
    val currentUserFlow: StateFlow<User?>
    suspend fun login(email: String, password: String, rememberMe: Boolean = false): AuthResult
    suspend fun signUp(
        name: String,
        email: String,
        studentId: String,
        password: String,
        role: UserRole
    ): AuthResult
    suspend fun sendPasswordResetCode(email: String): AuthResult
    suspend fun updateProfilePhoto(photoUrl: String?): AuthResult
    suspend fun logout()
    fun getCurrentUser(): User?
}
