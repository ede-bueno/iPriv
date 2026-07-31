package com.example.priv.data.repository

import com.example.priv.data.sync.SupabaseClientManager
import kotlinx.coroutines.flow.Flow

class AuthRepositoryImpl(
    private val supabaseManager: SupabaseClientManager
) : AuthRepository {

    override val currentSession: Flow<UserSession?> = supabaseManager.currentSession

    override suspend fun signUp(email: String, pass: String): Result<UserSession> {
        return try {
            val session = supabaseManager.createAccount(email, pass)
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signIn(email: String, pass: String): Result<UserSession> {
        return try {
            val session = supabaseManager.authenticate(email, pass)
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        supabaseManager.logout()
        return Result.success(Unit)
    }

    override suspend fun getCurrentUserId(): String? {
        return supabaseManager.getActiveUserId()
    }

    override suspend fun getPersonalSpaceId(): String {
        return supabaseManager.getActivePersonalSpaceId()
    }
}
