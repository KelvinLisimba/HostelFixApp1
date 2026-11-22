package com.example.hostelfixapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hostelfixapp.data.model.User
import com.example.hostelfixapp.data.model.UserRole
import com.example.hostelfixapp.data.repository.AuthRepository
import com.example.hostelfixapp.data.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val currentUser: User? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUserFlow.collect { user ->
                _uiState.value = _uiState.value.copy(currentUser = user)
            }
        }
    }

    fun login(email: String, pass: String, rememberMe: Boolean, onSuccess: (User) -> Unit) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter both email and password.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            val result = authRepository.login(email, pass, rememberMe)
            _uiState.value = _uiState.value.copy(isLoading = false)

            when (result) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(successMessage = result.successMessage)
                    onSuccess(result.user)
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(errorMessage = result.message)
                }
            }
        }
    }

    fun signUp(
        name: String,
        email: String,
        studentId: String,
        pass: String,
        role: UserRole,
        onSuccess: (User) -> Unit
    ) {
        if (name.isBlank() || email.isBlank() || pass.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please fill in all required fields.")
            return
        }

        if (pass.length < 8 || !pass.any { it.isUpperCase() } || !pass.any { it.isLowerCase() } || !pass.any { it.isDigit() || !it.isLetterOrDigit() }) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "⚠️ Password must be at least 8 characters long and contain uppercase, lowercase, and numbers or symbols."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            val result = authRepository.signUp(name, email, studentId, pass, role)
            _uiState.value = _uiState.value.copy(isLoading = false)

            when (result) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(successMessage = result.successMessage)
                    onSuccess(result.user)
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(errorMessage = result.message)
                }
            }
        }
    }

    fun sendPasswordResetCode(email: String) {
        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter your university email address.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            val result = authRepository.sendPasswordResetCode(email)
            _uiState.value = _uiState.value.copy(isLoading = false)

            when (result) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(successMessage = result.successMessage)
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(errorMessage = result.message)
                }
            }
        }
    }

    fun updateProfilePhoto(photoUrl: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            val result = authRepository.updateProfilePhoto(photoUrl)
            _uiState.value = _uiState.value.copy(isLoading = false)

            when (result) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(successMessage = result.successMessage)
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(errorMessage = result.message)
                }
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = AuthUiState(currentUser = null, successMessage = null, errorMessage = null)
            onLoggedOut()
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
