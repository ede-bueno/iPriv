package com.example.priv.data.dao

import androidx.room.*
import com.example.priv.data.entity.*
import com.example.priv.data.model.MemoryWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {

    @Transaction
    @Query("SELECT * FROM memories WHERE inTrash = 0 AND deletedAt IS NULL ORDER BY timestamp DESC")
    fun getAllActiveMemories(): Flow<List<MemoryWithDetails>>

    @Transaction
    @Query("SELECT * FROM memories WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    fun getMemoryById(id: String): Flow<MemoryWithDetails?>

    @Transaction
    @Query("SELECT * FROM memories WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getMemoryByIdDirect(id: String): MemoryWithDetails?

    @Transaction
    @Query("SELECT * FROM memories WHERE inTrash = 0 AND deletedAt IS NULL AND (primaryPersonId = :personId OR id IN (SELECT memoryId FROM memory_person_cross_ref WHERE personId = :personId)) ORDER BY timestamp DESC")
    fun getMemoriesForPerson(personId: String): Flow<List<MemoryWithDetails>>

    @Transaction
    @Query("SELECT * FROM memories WHERE inTrash = 0 AND deletedAt IS NULL AND (momentId = :momentId OR id IN (SELECT memoryId FROM memory_moment_cross_ref WHERE momentId = :momentId)) ORDER BY timestamp DESC")
    fun getMemoriesForMoment(momentId: String): Flow<List<MemoryWithDetails>>

    @Transaction
    @Query("SELECT * FROM memories WHERE inTrash = 0 AND deletedAt IS NULL AND (collectionId = :collectionId OR id IN (SELECT memoryId FROM memory_collection_cross_ref WHERE collectionId = :collectionId)) ORDER BY timestamp DESC")
    fun getMemoriesForCollection(collectionId: String): Flow<List<MemoryWithDetails>>

    @Transaction
    @Query("SELECT * FROM memories WHERE inTrash = 0 AND deletedAt IS NULL AND isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteMemories(): Flow<List<MemoryWithDetails>>

    @Transaction
    @Query("SELECT * FROM memories WHERE inTrash = 0 AND deletedAt IS NULL AND primaryPersonId IS NULL AND momentId IS NULL ORDER BY timestamp DESC")
    fun getUnorganizedMemories(): Flow<List<MemoryWithDetails>>

    @Transaction
    @Query("SELECT * FROM memories WHERE inTrash = 1 AND deletedAt IS NULL ORDER BY trashedAt DESC")
    fun getTrashedMemories(): Flow<List<MemoryWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity)

    @Update
    suspend fun updateMemory(memory: MemoryEntity)

    @Query("UPDATE memories SET inTrash = 1, trashedAt = :trashedAt, updatedAt = :updatedAt WHERE id = :memoryId")
    suspend fun moveToTrash(memoryId: String, trashedAt: Long = System.currentTimeMillis(), updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE memories SET inTrash = 0, trashedAt = NULL, updatedAt = :updatedAt WHERE id = :memoryId")
    suspend fun restoreFromTrash(memoryId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE memories SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :memoryId")
    suspend fun softDelete(memoryId: String, deletedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM memories WHERE id = :memoryId")
    suspend fun deletePermanently(memoryId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: MemoryAttachmentEntity)

    @Update
    suspend fun updateAttachment(attachment: MemoryAttachmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachments(attachments: List<MemoryAttachmentEntity>)

    @Query("DELETE FROM memory_attachments WHERE memoryId = :memoryId")
    suspend fun deleteAttachmentsForMemory(memoryId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonCrossRef(crossRef: MemoryPersonCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMomentCrossRef(crossRef: MemoryMomentCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollectionCrossRef(crossRef: MemoryCollectionCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTagCrossRef(crossRef: MemoryTagCrossRef)

    @Query("DELETE FROM memory_person_cross_ref WHERE memoryId = :memoryId")
    suspend fun clearPersonCrossRefs(memoryId: String)

    @Query("DELETE FROM memory_tag_cross_ref WHERE memoryId = :memoryId")
    suspend fun clearTagCrossRefs(memoryId: String)
}
