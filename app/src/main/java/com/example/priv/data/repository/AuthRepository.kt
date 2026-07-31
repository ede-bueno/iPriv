package com.example.priv.data.repository

import kotlinx.coroutines.flow.Flow

data class UserSession(
    val userId: String,
    val email: String,
    val personalSpaceId: String,
    val isAuthenticated: Boolean
)

interface AuthRepository {
    val currentSession: Flow<UserSession?>
    suspend fun signUp(email: String, pass: String): Result<UserSession>
    suspend fun signIn(email: String, pass: String): Result<UserSession>
    suspend fun signOut(): Result<Unit>
    suspend fun getCurrentUserId(): String?
    suspend fun getPersonalSpaceId(): String
}
