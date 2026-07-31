package com.example.priv.data.repository

import com.example.priv.data.entity.SyncOperationEntity
import com.example.priv.data.model.SyncStatus
import kotlinx.coroutines.flow.Flow

interface SyncQueueRepository {
    fun getPendingOperations(): Flow<List<SyncOperationEntity>>
    suspend fun enqueueOperation(operation: SyncOperationEntity)
    suspend fun updateOperationStatus(operationId: String, status: SyncStatus, error: String? = null)
    suspend fun removeOperation(operationId: String)
    suspend fun clearCompletedOperations()
}
