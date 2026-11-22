package com.example.hostelfixapp.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.hostelfixapp.data.local.HostelFixLocalDb
import com.example.hostelfixapp.data.model.User
import com.example.hostelfixapp.data.model.UserRole
import com.example.hostelfixapp.data.remote.HostelFixCloudRegistry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class AuthRepositoryImpl(private val context: Context) : AuthRepository {

    private val localDb = HostelFixLocalDb(context)
    private val prefs: SharedPreferences =
        context.getSharedPreferences("hostelfix_auth_prefs", Context.MODE_PRIVATE)

    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val _currentUserFlow = MutableStateFlow<User?>(null)
    override val currentUserFlow: StateFlow<User?> = _currentUserFlow.asStateFlow()

    init {
        // Auto restore user session and profile picture on app launch/refresh
        val savedUserId = prefs.getString("saved_user_id", null)
        if (savedUserId != null) {
            val user = localDb.getUserById(savedUserId)
            if (user != null) {
                _currentUserFlow.value = user
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    override suspend fun login(email: String, password: String, rememberMe: Boolean): AuthResult {
        val normalizedEmail = email.trim().lowercase()

        // 1. Offline / Local database check first
        val localPair = localDb.getUserByEmail(normalizedEmail)
        if (localPair != null) {
            if (localPair.second == password) {
                val user = localPair.first
                _currentUserFlow.value = user
                saveRememberMe(user.id, rememberMe)
                return AuthResult.Success(user, "Welcome back, ${user.name}!")
            } else {
                return AuthResult.Error("⚠️ Incorrect password. Please try again.")
            }
        }

        // 2. Check Cloud Registry fallback
        val cloudPair = HostelFixCloudRegistry.findCloudUserByEmail(normalizedEmail)
        if (cloudPair != null) {
            if (cloudPair.second == password) {
                val user = cloudPair.first
                localDb.saveUser(user, password)
                _currentUserFlow.value = user
                saveRememberMe(user.id, rememberMe)
                return AuthResult.Success(user, "Welcome back, ${user.name}!")
            } else {
                return AuthResult.Error("⚠️ Incorrect password. Please try again.")
            }
        }

        // 3. Cross-Device Firebase Authentication
        if (isNetworkAvailable()) {
            val firebaseResult = withTimeoutOrNull(5000L) {
                firebaseSignIn(normalizedEmail, password)
            }

            if (firebaseResult != null && firebaseResult.isSuccess) {
                val firebaseUid = firebaseResult.getOrNull()?.user?.uid ?: ""
                val cloudUser = fetchUserFromFirestore(firebaseUid) ?: User(
                    id = firebaseUid,
                    name = normalizedEmail.substringBefore("@").replace(".", " ").capitalizeWords(),
                    email = normalizedEmail,
                    studentId = if (normalizedEmail.endsWith("@unima.ac.mw")) normalizedEmail.substringBefore("@") else "",
                    role = if (normalizedEmail.contains("staff") || normalizedEmail.contains("maintenance")) UserRole.STAFF else UserRole.STUDENT
                )

                localDb.saveUser(cloudUser, password)
                HostelFixCloudRegistry.registerUserInCloud(cloudUser, password)

                _currentUserFlow.value = cloudUser
                saveRememberMe(cloudUser.id, rememberMe)
                return AuthResult.Success(cloudUser, "Welcome back, ${cloudUser.name}!")
            }
        }

        return AuthResult.Error("⚠️ Account not registered with email '$normalizedEmail'. Please sign up first.")
    }

    override suspend fun signUp(
        name: String,
        email: String,
        studentId: String,
        password: String,
        role: UserRole
    ): AuthResult {
        val normalizedEmail = email.trim().lowercase()

        if (name.isBlank()) {
            return AuthResult.Error("Name cannot be empty.")
        }
        if (normalizedEmail.isBlank() || !normalizedEmail.contains("@")) {
            return AuthResult.Error("Please enter a valid university email address.")
        }
        if (role == UserRole.STUDENT) {
            val cleanStudentId = studentId.trim().lowercase()
            if (cleanStudentId.isBlank()) {
                return AuthResult.Error("Student ID is required for student registration.")
            }
            if (!cleanStudentId.contains("-")) {
                return AuthResult.Error("Student ID must contain a hyphen (-) (e.g., bsc-17-21).")
            }
            val expectedEmail = "$cleanStudentId@unima.ac.mw"
            if (normalizedEmail != expectedEmail && !normalizedEmail.endsWith("@unima.ac.mw")) {
                return AuthResult.Error("Student email must match the format: <student_id>@unima.ac.mw (e.g. $cleanStudentId@unima.ac.mw)")
            }
        }
        if (password.length < 6) {
            return AuthResult.Error("Password must be at least 6 characters long.")
        }

        if (localDb.getUserByEmail(normalizedEmail) != null || HostelFixCloudRegistry.isEmailRegisteredInCloud(normalizedEmail)) {
            return AuthResult.Error("An account with this email address already exists.")
        }

        // Attempt Firebase User Creation with Timeout Guard
        val firebaseResult = if (isNetworkAvailable()) {
            withTimeoutOrNull(5000L) {
                firebaseSignUp(normalizedEmail, password)
            }
        } else null

        val uid = if (firebaseResult != null && firebaseResult.isSuccess) {
            firebaseResult.getOrNull()?.user?.uid ?: "usr_${System.currentTimeMillis()}"
        } else {
            "usr_${System.currentTimeMillis()}"
        }

        val newUser = User(
            id = uid,
            name = name.trim(),
            email = normalizedEmail,
            studentId = studentId.trim(),
            role = role,
            createdAt = System.currentTimeMillis()
        )

        // Save in Firebase Cloud Firestore & Global Registry
        saveUserToFirestore(newUser)
        HostelFixCloudRegistry.registerUserInCloud(newUser, password)

        // Save locally for offline login
        localDb.saveUser(newUser, password)
        _currentUserFlow.value = newUser

        return AuthResult.Success(newUser, "Account created successfully!")
    }

    override suspend fun sendPasswordResetCode(email: String): AuthResult {
        val normalizedEmail = email.trim().lowercase()
        if (normalizedEmail.isBlank() || !normalizedEmail.contains("@")) {
            return AuthResult.Error("Please enter a valid email address.")
        }

        if (isNetworkAvailable()) {
            withTimeoutOrNull(5000L) {
                suspendCancellableCoroutine<Unit> { continuation ->
                    try {
                        firebaseAuth.sendPasswordResetEmail(normalizedEmail)
                            .addOnCompleteListener { continuation.resume(Unit) }
                    } catch (e: Exception) {
                        continuation.resume(Unit)
                    }
                }
            }
        }

        return AuthResult.Success(
            user = User(email = normalizedEmail),
            successMessage = "Password reset verification code sent to $normalizedEmail! Please check your inbox."
        )
    }

    override suspend fun updateProfilePhoto(photoUrl: String?): AuthResult {
        val currentUser = _currentUserFlow.value ?: return AuthResult.Error("User session not found.")
        val updatedUser = currentUser.copy(profilePicUrl = photoUrl)

        localDb.updateProfilePicture(currentUser.id, photoUrl ?: "")
        HostelFixCloudRegistry.updateCloudProfilePic(currentUser.id, photoUrl ?: "")
        saveUserToFirestore(updatedUser)

        prefs.edit().putString("saved_user_id", currentUser.id).apply()
        _currentUserFlow.value = updatedUser

        val msg = if (photoUrl.isNullOrBlank()) "Profile photo removed successfully!" else "Profile picture updated successfully!"
        return AuthResult.Success(updatedUser, msg)
    }

    override suspend fun logout() {
        prefs.edit().clear().apply()
        try {
            firebaseAuth.signOut()
        } catch (_: Exception) {}
        _currentUserFlow.value = null
    }

    override fun getCurrentUser(): User? {
        return _currentUserFlow.value
    }

    private fun saveRememberMe(userId: String, rememberMe: Boolean) {
        if (rememberMe) {
            prefs.edit()
                .putBoolean("remember_me", true)
                .putString("saved_user_id", userId)
                .apply()
        } else {
            prefs.edit().clear().apply()
        }
    }

    private fun String.capitalizeWords(): String {
        return split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }

    // Firebase Helper Functions
    private suspend fun firebaseSignUp(email: String, pass: String): Result<com.google.firebase.auth.AuthResult> =
        suspendCancellableCoroutine { continuation ->
            try {
                firebaseAuth.createUserWithEmailAndPassword(email, pass)
                    .addOnSuccessListener { res -> continuation.resume(Result.success(res)) }
                    .addOnFailureListener { exc -> continuation.resume(Result.failure(exc)) }
            } catch (e: Exception) {
                continuation.resume(Result.failure(e))
            }
        }

    private suspend fun firebaseSignIn(email: String, pass: String): Result<com.google.firebase.auth.AuthResult> =
        suspendCancellableCoroutine { continuation ->
            try {
                firebaseAuth.signInWithEmailAndPassword(email, pass)
                    .addOnSuccessListener { res -> continuation.resume(Result.success(res)) }
                    .addOnFailureListener { exc -> continuation.resume(Result.failure(exc)) }
            } catch (e: Exception) {
                continuation.resume(Result.failure(e))
            }
        }

    private fun saveUserToFirestore(user: User) {
        try {
            val userMap = mapOf(
                "id" to user.id,
                "name" to user.name,
                "email" to user.email,
                "studentId" to user.studentId,
                "role" to user.role.name,
                "profilePicUrl" to (user.profilePicUrl ?: ""),
                "createdAt" to user.createdAt
            )
            firestore.collection("users").document(user.id).set(userMap)
        } catch (_: Exception) {}
    }

    private suspend fun fetchUserFromFirestore(uid: String): User? =
        withTimeoutOrNull(4000L) {
            suspendCancellableCoroutine { continuation ->
                try {
                    firestore.collection("users").document(uid).get()
                        .addOnSuccessListener { doc ->
                            if (doc.exists()) {
                                val user = User(
                                    id = doc.getString("id") ?: uid,
                                    name = doc.getString("name") ?: "",
                                    email = doc.getString("email") ?: "",
                                    studentId = doc.getString("studentId") ?: "",
                                    role = try {
                                        UserRole.valueOf(doc.getString("role") ?: UserRole.STUDENT.name)
                                    } catch (_: Exception) { UserRole.STUDENT },
                                    profilePicUrl = doc.getString("profilePicUrl"),
                                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                                )
                                continuation.resume(user)
                            } else {
                                continuation.resume(null)
                            }
                        }
                        .addOnFailureListener { continuation.resume(null) }
                } catch (e: Exception) {
                    continuation.resume(null)
                }
            }
        }
}
