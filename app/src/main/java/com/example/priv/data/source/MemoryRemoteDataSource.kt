package com.example.priv.data.source

import com.example.priv.data.entity.MemoryAttachmentEntity
import com.example.priv.data.entity.MemoryEntity

interface MemoryRemoteDataSource {
    suspend fun pushMemory(memory: MemoryEntity): Result<Unit>
    suspend fun fetchMemory(memoryId: String): Result<MemoryEntity?>
    suspend fun fetchMemoriesForSpace(spaceId: String): Result<List<MemoryEntity>>
    suspend fun pushAttachmentMetadata(attachment: MemoryAttachmentEntity): Result<Unit>
    suspend fun fetchAttachmentsForMemory(memoryId: String): Result<List<MemoryAttachmentEntity>>
    suspend fun deleteMemoryRemote(memoryId: String): Result<Unit>
}
