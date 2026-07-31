package com.example.priv.data.source

import com.example.priv.data.entity.MemoryAttachmentEntity
import com.example.priv.data.entity.MemoryEntity
import com.example.priv.data.model.MemoryWithDetails
import kotlinx.coroutines.flow.Flow

interface MemoryLocalDataSource {
    fun getAllActiveMemories(): Flow<List<MemoryWithDetails>>
    fun getMemoryById(id: String): Flow<MemoryWithDetails?>
    suspend fun getMemoryByIdDirect(id: String): MemoryWithDetails?
    suspend fun insertMemory(memory: MemoryEntity)
    suspend fun insertAttachment(attachment: MemoryAttachmentEntity)
    suspend fun updateMemory(memory: MemoryEntity)
    suspend fun updateAttachment(attachment: MemoryAttachmentEntity)
    suspend fun moveToTrash(memoryId: String)
    suspend fun restoreFromTrash(memoryId: String)
    suspend fun deletePermanently(memoryId: String)
}
