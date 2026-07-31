package com.example.priv.data.dao

import androidx.room.*
import com.example.priv.data.entity.SyncOperationEntity
import com.example.priv.data.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE status = :status ORDER BY createdAt ASC")
    fun getPendingOperations(status: SyncStatus = SyncStatus.PENDING_UPLOAD): Flow<List<SyncOperationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueOperation(operation: SyncOperationEntity)

    @Update
    suspend fun updateOperation(operation: SyncOperationEntity)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun removeOperation(id: String)
}
