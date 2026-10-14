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

    fun getMemoriesForGroup(groupId: String): Flow<List<MemoryWithDetails>> = memoryDao.getMemoriesForGroup(groupId)

    suspend fun createMultimediaMemory(
        title: String,
        note: String,
        attachments: List<com.example.priv.data.model.AttachmentInput>,
        personIds: List<String> = emptyList(),
        groupIds: List<String> = emptyList(),
        momentIds: List<String> = emptyList(),
        collectionIds: List<String> = emptyList(),
        tags: List<String> = emptyList(),
        source: String = "Manual",
        spaceId: String = "default_space",
        customTimestamp: Long? = null
    ): String {
        val memoryId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val effectiveTimestamp = customTimestamp ?: attachments.mapNotNull { it.capturedTimestamp }.minOrNull() ?: now

        val memory = MemoryEntity(
            id = memoryId,
            spaceId = spaceId,
            title = title.ifBlank { "Memória de $source" },
            note = note,
            timestamp = effectiveTimestamp,
            createdAt = now,
            updatedAt = now,
            source = source,
            primaryPersonId = personIds.firstOrNull(),
            momentId = momentIds.firstOrNull(),
            collectionId = collectionIds.firstOrNull(),
            tags = tags.filter { it.isNotBlank() }.joinToString(","),
            syncStatus = SyncStatus.LOCAL_ONLY
        )

        memoryDao.insertMemory(memory)

        for (att in attachments) {
            val attachmentId = UUID.randomUUID().toString()
            val attachment = MemoryAttachmentEntity(
                id = attachmentId,
                memoryId = memoryId,
                type = att.type,
                uri = att.uri,
                localPath = att.localPath,
                fileName = att.fileName,
                fileSize = att.fileSize,
                mimeType = att.mimeType,
                checksum = att.checksum,
                durationMs = att.durationMs,
                width = att.width,
                height = att.height,
                waveformData = att.waveformData,
                transcription = att.textContent,
                uploadStatus = AttachmentSyncStatus.LOCAL_ONLY
            )
            memoryDao.insertAttachment(attachment)
        }

        // Attach all tagged persons (Many-to-Many)
        for (pId in personIds.distinct()) {
            memoryDao.insertPersonCrossRef(MemoryPersonCrossRef(memoryId, pId))
        }

        // Attach all groups (Many-to-Many)
        for (gId in groupIds.distinct()) {
            memoryDao.insertGroupCrossRef(MemoryGroupCrossRef(memoryId, gId))
        }

        // Attach all moments (Many-to-Many)
        for (mId in momentIds.distinct()) {
            memoryDao.insertMomentCrossRef(MemoryMomentCrossRef(memoryId, mId))
        }

        // Attach all collections (Many-to-Many)
        for (cId in collectionIds.distinct()) {
            memoryDao.insertCollectionCrossRef(MemoryCollectionCrossRef(memoryId, cId))
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

    suspend fun addAttachmentsToExistingMemory(
        memoryId: String,
        newAttachments: List<com.example.priv.data.model.AttachmentInput>
    ) {
        val now = System.currentTimeMillis()
        for (att in newAttachments) {
            val attachmentId = UUID.randomUUID().toString()
            val attachment = MemoryAttachmentEntity(
                id = attachmentId,
                memoryId = memoryId,
                type = att.type,
                uri = att.uri,
                localPath = att.localPath,
                fileName = att.fileName,
                fileSize = att.fileSize,
                mimeType = att.mimeType,
                checksum = att.checksum,
                durationMs = att.durationMs,
                width = att.width,
                height = att.height,
                waveformData = att.waveformData,
                transcription = att.textContent,
                uploadStatus = AttachmentSyncStatus.LOCAL_ONLY,
                createdAt = now,
                updatedAt = now
            )
            memoryDao.insertAttachment(attachment)
        }

        val existing = memoryDao.getMemoryByIdDirect(memoryId)
        if (existing != null) {
            memoryDao.updateMemory(existing.memory.copy(updatedAt = now))
        }

        syncQueueDao?.enqueueOperation(
            SyncOperationEntity(
                entityType = "ATTACHMENT",
                entityId = memoryId,
                operationType = OperationType.UPDATE_ENTITY,
                payloadJson = """{"memoryId":"$memoryId","newAttachmentsCount":${newAttachments.size}}""",
                status = SyncStatus.PENDING_UPDATE
            )
        )
    }

    suspend fun updateMemoryFull(
        memoryId: String,
        newTitle: String,
        newNote: String,
        personIds: List<String> = emptyList(),
        tags: List<String> = emptyList(),
        newAttachments: List<com.example.priv.data.model.AttachmentInput> = emptyList(),
        deletedAttachmentIds: List<String> = emptyList()
    ) {
        val now = System.currentTimeMillis()
        val existingWithDetails = memoryDao.getMemoryByIdDirect(memoryId) ?: return

        val updated = existingWithDetails.memory.copy(
            title = newTitle.ifBlank { existingWithDetails.memory.title },
            note = newNote,
            primaryPersonId = personIds.firstOrNull() ?: existingWithDetails.memory.primaryPersonId,
            tags = tags.filter { it.isNotBlank() }.joinToString(","),
            updatedAt = now
        )
        memoryDao.updateMemory(updated)

        // Remover anexos deletados se houver
        if (deletedAttachmentIds.isNotEmpty()) {
            val remainingAttachments = existingWithDetails.attachments.filterNot { it.id in deletedAttachmentIds }
            memoryDao.deleteAttachmentsForMemory(memoryId)
            memoryDao.insertAttachments(remainingAttachments)
        }

        // Inserir novos anexos se houver
        if (newAttachments.isNotEmpty()) {
            for (att in newAttachments) {
                val attachmentId = UUID.randomUUID().toString()
                val attachment = MemoryAttachmentEntity(
                    id = attachmentId,
                    memoryId = memoryId,
                    type = att.type,
                    uri = att.uri,
                    localPath = att.localPath,
                    fileName = att.fileName,
                    fileSize = att.fileSize,
                    mimeType = att.mimeType,
                    checksum = att.checksum,
                    durationMs = att.durationMs,
                    width = att.width,
                    height = att.height,
                    waveformData = att.waveformData,
                    transcription = att.textContent,
                    uploadStatus = AttachmentSyncStatus.LOCAL_ONLY,
                    createdAt = now,
                    updatedAt = now
                )
                memoryDao.insertAttachment(attachment)
            }
        }

        // Atualizar relacionamentos de pessoas
        if (personIds.isNotEmpty()) {
            memoryDao.clearPersonCrossRefs(memoryId)
            for (pId in personIds.distinct()) {
                memoryDao.insertPersonCrossRef(MemoryPersonCrossRef(memoryId, pId))
            }
        }

        // Atualizar tags
        if (tags.isNotEmpty()) {
            memoryDao.clearTagCrossRefs(memoryId)
            for (tagName in tags) {
                if (tagName.isNotBlank()) {
                    val cleanTag = tagName.replace("#", "").trim()
                    var tagEntity = tagDao.getTagByName(cleanTag)
                    val tagId = tagEntity?.id ?: UUID.randomUUID().toString().also { newTagId ->
                        tagDao.insertTag(TagEntity(id = newTagId, spaceId = updated.spaceId, name = cleanTag))
                    }
                    memoryDao.insertTagCrossRef(MemoryTagCrossRef(memoryId, tagId))
                }
            }
        }

        syncQueueDao?.enqueueOperation(
            SyncOperationEntity(
                entityType = "MEMORY",
                entityId = memoryId,
                operationType = OperationType.UPDATE_ENTITY,
                payloadJson = """{"id":"$memoryId","action":"updateFull"}""",
                status = SyncStatus.PENDING_UPDATE
            )
        )
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
        memoryDao.clearGroupCrossRefs(memoryId)
        memoryDao.clearMomentCrossRefs(memoryId)
        memoryDao.clearCollectionCrossRefs(memoryId)
        memoryDao.clearTagCrossRefs(memoryId)
        memoryDao.deletePermanently(memoryId)
    }

    /**
     * Pesquisa memórias no Room por título, anotação ou tags associadas.
     */
    fun searchMemories(query: String) = memoryDao.searchMemories(query.trim())

    /**
     * Filtra memórias no Room por uma tag específica (ex: 'família', 'viagem', 'trabalho').
     */
    fun getMemoriesByTag(tag: String) = memoryDao.getMemoriesByTag(tag.trim())

    /**
     * Atualiza as tags da memória no Room.
     */
    suspend fun updateMemoryTags(memoryId: String, tags: List<String>) {
        val tagsStr = tags.filter { it.isNotBlank() }.joinToString(",")
        memoryDao.updateMemoryTags(memoryId, tagsStr)
    }
}
