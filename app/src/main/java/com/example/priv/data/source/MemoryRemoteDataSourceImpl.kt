package com.example.priv.data.source

import com.example.priv.data.entity.MemoryAttachmentEntity
import com.example.priv.data.entity.MemoryEntity
import com.example.priv.data.sync.SupabaseClientManager

class MemoryRemoteDataSourceImpl(
    private val supabaseManager: SupabaseClientManager
) : MemoryRemoteDataSource {

    override suspend fun pushMemory(memory: MemoryEntity): Result<Unit> {
        return supabaseManager.pushMemoryRemote(memory)
    }

    override suspend fun fetchMemory(memoryId: String): Result<MemoryEntity?> {
        val spaceId = supabaseManager.getActivePersonalSpaceId()
        val res = supabaseManager.fetchMemoriesRemote(spaceId)
        return res.map { list -> list.firstOrNull { it.id == memoryId } }
    }

    override suspend fun fetchMemoriesForSpace(spaceId: String): Result<List<MemoryEntity>> {
        return supabaseManager.fetchMemoriesRemote(spaceId)
    }

    override suspend fun pushAttachmentMetadata(attachment: MemoryAttachmentEntity): Result<Unit> {
        return supabaseManager.pushAttachmentMetadataRemote(attachment)
    }

    override suspend fun fetchAttachmentsForMemory(memoryId: String): Result<List<MemoryAttachmentEntity>> {
        // Return empty or fetch from PostgREST attachments table
        return Result.success(emptyList())
    }

    override suspend fun deleteMemoryRemote(memoryId: String): Result<Unit> {
        return Result.success(Unit)
    }
}
