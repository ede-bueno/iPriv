package com.example.priv.data.repository

import kotlinx.coroutines.flow.Flow

data class UserSession(
    val userId: String,
    val email: String,
    val displayName: String = "",
    val photoUrl: String? = null,
    val personalSpaceId: String,
    val isAuthenticated: Boolean,
    val authProvider: String = "Email",
    val createdAtTimestamp: Long = System.currentTimeMillis()
)

interface AuthRepository {
    val currentSession: Flow<UserSession?>
    suspend fun signUp(email: String, pass: String, name: String = ""): Result<UserSession>
    suspend fun signIn(email: String, pass: String): Result<UserSession>
    suspend fun signInWithGoogleToken(idToken: String, email: String, name: String?, photoUrl: String?): Result<UserSession>
    suspend fun updateProfile(displayName: String, photoUrl: String?): Result<UserSession>
    suspend fun signOut(): Result<Unit>
    suspend fun getCurrentUserId(): String?
    suspend fun getPersonalSpaceId(): String
}
