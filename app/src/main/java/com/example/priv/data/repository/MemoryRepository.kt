package com.example.priv.data.repository

import com.example.priv.data.dao.MemoryDao
import com.example.priv.data.dao.SyncQueueDao
import com.example.priv.data.dao.TagDao
import com.example.priv.data.entity.*
import com.example.priv.data.model.AttachmentSyncStatus
import com.example.priv.data.model.MemoryWithDetails
import com.example.priv.data.model.OperationType
import com.example.priv.data.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class MemoryRepository(
    private val memoryDao: MemoryDao,
    private val tagDao: TagDao,
    private val syncQueueDao: SyncQueueDao? = null
) {
    val activeMemories: Flow<List<MemoryWithDetails>> = memoryDao.getAllActiveMemories()
    val favoriteMemories: Flow<List<MemoryWithDetails>> = memoryDao.getFavoriteMemories()
    val unorganizedMemories: Flow<List<MemoryWithDetails>> = memoryDao.getUnorganizedMemories()
    val trashedMemories: Flow<List<MemoryWithDetails>> = memoryDao.getTrashedMemories()

    fun getMemoryById(id: String): Flow<MemoryWithDetails?> = memoryDao.getMemoryById(id)
    suspend fun getMemoryByIdDirect(id: String): MemoryWithDetails? = memoryDao.getMemoryByIdDirect(id)

    fun getMemoriesForPerson(personId: String): Flow<List<MemoryWithDetails>> = memoryDao.getMemoriesForPerson(personId)
    fun getMemoriesForMoment(momentId: String): Flow<List<MemoryWithDetails>> = memoryDao.getMemoriesForMoment(momentId)
    fun getMemoriesForCollection(collectionId: String): Flow<List<MemoryWithDetails>> = memoryDao.getMemoriesForCollection(collectionId)

    suspend fun createQuickAudioMemory(
        title: String,
        note: String,
        audioUri: String,
        localPath: String = "",
        checksum: String = "",
        durationMs: Long,
        fileSize: Long,
        waveformData: String,
        transcription: String = "",
        primaryPersonId: String? = null,
        momentId: String? = null,
        collectionId: String? = null,
        tags: List<String> = emptyList(),
        source: String = "WhatsApp",
        spaceId: String = "default_space"
    ): String {
        val memoryId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val memory = MemoryEntity(
            id = memoryId,
            spaceId = spaceId,
            title = title.ifBlank { "Áudio do $source" },
            note = note,
            timestamp = now,
            createdAt = now,
            updatedAt = now,
            source = source,
            primaryPersonId = primaryPersonId,
            momentId = momentId,
            collectionId = collectionId,
            syncStatus = SyncStatus.LOCAL_ONLY
        )

        memoryDao.insertMemory(memory)

        val attachmentId = UUID.randomUUID().toString()
        val attachment = MemoryAttachmentEntity(
            id = attachmentId,
            memoryId = memoryId,
            type = "AUDIO",
            uri = audioUri,
            localPath = localPath,
            checksum = checksum,
            durationMs = durationMs,
            fileSize = fileSize,
            waveformData = waveformData,
            transcription = transcription,
            uploadStatus = AttachmentSyncStatus.LOCAL_ONLY
        )
        memoryDao.insertAttachment(attachment)

        if (primaryPersonId != null) {
            memoryDao.insertPersonCrossRef(MemoryPersonCrossRef(memoryId, primaryPersonId))
        }

        if (momentId != null) {
            memoryDao.insertMomentCrossRef(MemoryMomentCrossRef(memoryId, momentId))
        }

        if (collectionId != null) {
            memoryDao.insertCollectionCrossRef(MemoryCollectionCrossRef(memoryId, collectionId))
        }

        for (tagName in tags) {
            if (tagName.isNotBlank()) {
                val cleanTag = tagName.replace("#", "").trim()
                var tagEntity = tagDao.getTagByName(cleanTag)
                val tagId = tagEntity?.id ?: UUID.randomUUID().toString().also { newTagId ->
                    tagDao.insertTag(TagEntity(id = newTagId, spaceId = spaceId, name = cleanTag))
                }
                memoryDao.insertTagCrossRef(MemoryTagCrossRef(memoryId, tagId))
            }
        }

        // Enqueue sync operation for background worker
        syncQueueDao?.enqueueOperation(
            SyncOperationEntity(
                entityType = "MEMORY",
                entityId = memoryId,
                operationType = OperationType.CREATE_ENTITY,
                payloadJson = """{"id":"$memoryId","spaceId":"$spaceId"}""",
                status = SyncStatus.PENDING_CREATE
            )
        )

        return memoryId
    }

    suspend fun toggleFavorite(memory: MemoryEntity) {
        val updated = memory.copy(isFavorite = !memory.isFavorite, updatedAt = System.currentTimeMillis())
        memoryDao.updateMemory(updated)
    }

    suspend fun updateMemory(memory: MemoryEntity) {
        val updated = memory.copy(updatedAt = System.currentTimeMillis())
        memoryDao.updateMemory(updated)
    }

    suspend fun moveToTrash(memoryId: String) {
        memoryDao.moveToTrash(memoryId)
    }

    suspend fun restoreFromTrash(memoryId: String) {
        memoryDao.restoreFromTrash(memoryId)
    }

    suspend fun deletePermanently(memoryId: String) {
        memoryDao.deleteAttachmentsForMemory(memoryId)
        memoryDao.clearPersonCrossRefs(memoryId)
        memoryDao.clearTagCrossRefs(memoryId)
        memoryDao.deletePermanently(memoryId)
    }
}
