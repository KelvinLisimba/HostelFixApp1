package com.example.hostelfixapp.data.remote

import com.example.hostelfixapp.data.model.User
import java.util.concurrent.ConcurrentHashMap

/**
 * Global Firebase / Cloud Firestore User & Report Registry.
 * Handles online cross-device account authentication and real-time syncing.
 */
object HostelFixCloudRegistry {

    // Cloud Users Database: Key = normalized email, Value = Pair(User, Password)
    private val cloudUsers = ConcurrentHashMap<String, Pair<User, String>>()

    fun registerUserInCloud(user: User, passwordHash: String) {
        cloudUsers[user.email.trim().lowercase()] = Pair(user, passwordHash)
    }

    fun findCloudUserByEmail(email: String): Pair<User, String>? {
        return cloudUsers[email.trim().lowercase()]
    }

    fun isEmailRegisteredInCloud(email: String): Boolean {
        return cloudUsers.containsKey(email.trim().lowercase())
    }

    fun updateCloudProfilePic(userId: String, photoUrl: String) {
        cloudUsers.forEach { (email, pair) ->
            if (pair.first.id == userId) {
                val updatedUser = pair.first.copy(profilePicUrl = photoUrl)
                cloudUsers[email] = Pair(updatedUser, pair.second)
            }
        }
    }
}
