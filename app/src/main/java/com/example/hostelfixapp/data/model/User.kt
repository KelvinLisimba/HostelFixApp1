package com.example.hostelfixapp.data.model

enum class UserRole {
    STUDENT,
    STAFF
}

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val studentId: String = "",
    val role: UserRole = UserRole.STUDENT,
    val assignedHostel: String = "",
    val assignedRoom: String = "",
    val profilePicUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
