package com.example.priv.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.priv.data.database.PrivDatabase
import com.example.priv.data.entity.CollectionEntity
import com.example.priv.data.entity.MomentEntity
import com.example.priv.data.entity.PersonEntity
import com.example.priv.data.repository.*
import com.example.priv.data.storage.MediaLocalStorage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

import com.example.priv.data.ai.AiSuggestionResult
import com.example.priv.data.ai.PrivGeminiIntelligenceManager

class QuickCaptureViewModel(application: Application) : AndroidViewModel(application) {

    private val db = PrivDatabase.getInstance(application)
    private val memoryRepo = MemoryRepository(db.memoryDao(), db.tagDao(), db.syncQueueDao())
    private val personRepo = PersonRepository(db.personDao())
    private val groupRepo = GroupRepository(db.groupDao(), db.syncQueueDao())
    private val momentRepo = MomentRepository(db.momentDao())
    private val collectionRepo = CollectionRepository(db.collectionDao())
    private val mediaStorage = MediaLocalStorage(application)
    private val intelligenceManager = PrivGeminiIntelligenceManager(application)

    val persons: StateFlow<List<PersonEntity>> = personRepo.allPersons
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groups: StateFlow<List<com.example.priv.data.entity.GroupEntity>> = groupRepo.allGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val moments: StateFlow<List<MomentEntity>> = momentRepo.allMoments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val collections: StateFlow<List<CollectionEntity>> = collectionRepo.allCollections
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeMemories: StateFlow<List<com.example.priv.data.model.MemoryWithDetails>> = memoryRepo.activeMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun appendToExistingMemory(
        targetMemoryId: String,
        attachments: List<com.example.priv.data.model.AttachmentInput>,
        additionalNote: String = "",
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val processedAttachments = mutableListOf<com.example.priv.data.model.AttachmentInput>()
            for (att in attachments) {
                var localPath = att.localPath
                var checksum = att.checksum
                var finalFileSize = att.fileSize

                if (att.uri.isNotBlank() && att.uri.startsWith("content://")) {
                    val uri = Uri.parse(att.uri)
                    val ext = when (att.type) {
                        "AUDIO" -> "ogg"
                        "IMAGE" -> "jpg"
                        "VIDEO" -> "mp4"
                        else -> "bin"
                    }
                    val attachmentId = UUID.randomUUID().toString()
                    val saveResult = mediaStorage.saveMediaFile(
                        sourceUri = uri,
                        spaceId = "default_space",
                        memoryId = targetMemoryId,
                        attachmentId = attachmentId,
                        extension = ext
                    )
                    if (saveResult != null) {
                        localPath = saveResult.localPath
                        checksum = saveResult.checksum
                        finalFileSize = saveResult.fileSize
                    }
                }

                processedAttachments.add(
                    att.copy(
                        localPath = localPath,
                        checksum = checksum,
                        fileSize = finalFileSize
                    )
                )
            }

            memoryRepo.addAttachmentsToExistingMemory(
                memoryId = targetMemoryId,
                newAttachments = processedAttachments
            )

            if (additionalNote.isNotBlank()) {
                val existing = memoryRepo.getMemoryByIdDirect(targetMemoryId)
                if (existing != null) {
                    val updatedNote = if (existing.memory.note.isBlank()) {
                        additionalNote
                    } else {
                        "${existing.memory.note}\n\n[Anexo adicionado]: $additionalNote"
                    }
                    memoryRepo.updateMemory(existing.memory.copy(note = updatedNote))
                }
            }

            onSuccess()
        }
    }

    fun createPersonInline(name: String, rel: String, colorHex: String = "#FF4B72", onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val newPerson = PersonEntity(
                name = name,
                relationship = rel,
                colorHex = colorHex
            )
            personRepo.insertPerson(newPerson)
            onCreated(newPerson.id)
        }
    }

    fun createGroupInline(name: String, desc: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val id = groupRepo.createGroup(name = name, description = desc)
            onCreated(id)
        }
    }

    fun createMomentInline(title: String, desc: String, date: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val newMoment = MomentEntity(
                title = title,
                description = desc,
                date = date
            )
            momentRepo.insertMoment(newMoment)
            onCreated(newMoment.id)
        }
    }

    fun createCollectionInline(title: String, desc: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val newCol = CollectionEntity(
                title = title,
                description = desc
            )
            collectionRepo.insertCollection(newCol)
            onCreated(newCol.id)
        }
    }

    fun saveMultimediaMemory(
        title: String,
        note: String,
        attachments: List<com.example.priv.data.model.AttachmentInput>,
        personIds: List<String> = emptyList(),
        groupIds: List<String> = emptyList(),
        momentIds: List<String> = emptyList(),
        collectionIds: List<String> = emptyList(),
        tagsString: String,
        source: String = "Manual",
        customTimestamp: Long? = null,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            val tags = tagsString.split(",").map { it.trim() }.filter { it.isNotBlank() }
            val processedAttachments = mutableListOf<com.example.priv.data.model.AttachmentInput>()

            for (att in attachments) {
                var localPath = att.localPath
                var checksum = att.checksum
                var finalFileSize = att.fileSize

                if (att.uri.startsWith("content://") || att.uri.startsWith("file://")) {
                    val parsedUri = Uri.parse(att.uri)
                    val attachmentId = UUID.randomUUID().toString()
                    val tempMemoryId = UUID.randomUUID().toString()
                    val ext = when (att.type) {
                        "IMAGE" -> "jpg"
                        "VIDEO" -> "mp4"
                        "AUDIO" -> "ogg"
                        else -> "bin"
                    }
                    val saveResult = mediaStorage.saveMediaFile(
                        sourceUri = parsedUri,
                        spaceId = "default_space",
                        memoryId = tempMemoryId,
                        attachmentId = attachmentId,
                        extension = ext
                    )
                    if (saveResult != null) {
                        localPath = saveResult.localPath
                        checksum = saveResult.checksum
                        finalFileSize = saveResult.fileSize
                    }
                }

                processedAttachments.add(
                    att.copy(
                        localPath = localPath,
                        checksum = checksum,
                        fileSize = finalFileSize
                    )
                )
            }

            val id = memoryRepo.createMultimediaMemory(
                title = title,
                note = note,
                attachments = processedAttachments,
                personIds = personIds,
                groupIds = groupIds,
                momentIds = momentIds,
                collectionIds = collectionIds,
                tags = tags,
                source = source,
                customTimestamp = customTimestamp
            )
            onSuccess(id)
        }
    }

    fun generateAiSuggestions(
        noteText: String,
        personName: String?,
        audioDurationMs: Long = 0L,
        onResult: (AiSuggestionResult) -> Unit
    ) {
        viewModelScope.launch {
            val res = intelligenceManager.analyzeAndSuggest(
                noteText = noteText,
                personName = personName,
                audioDurationMs = audioDurationMs
            )
            onResult(res)
        }
    }

    fun transcribeAudio(
        noteText: String,
        personName: String?,
        durationMs: Long,
        onResult: (String) -> Unit
    ) {
        viewModelScope.launch {
            val transcript = intelligenceManager.transcribeAudio(noteText, personName, durationMs)
            onResult(transcript)
        }
    }

    fun saveMemory(
        title: String,
        note: String,
        audioUri: String,
        durationMs: Long,
        fileSize: Long,
        waveformData: String,
        transcription: String,
        primaryPersonId: String?,
        momentId: String?,
        collectionId: String?,
        tagsString: String,
        source: String = "WhatsApp",
        onSuccess: (String) -> Unit
    ) {
        val attachment = com.example.priv.data.model.AttachmentInput(
            type = "AUDIO",
            uri = audioUri,
            durationMs = durationMs,
            fileSize = fileSize,
            waveformData = waveformData,
            textContent = transcription,
            mimeType = "audio/ogg"
        )
        saveMultimediaMemory(
            title = title,
            note = note,
            attachments = listOf(attachment),
            personIds = listOfNotNull(primaryPersonId),
            momentIds = listOfNotNull(momentId),
            collectionIds = listOfNotNull(collectionId),
            tagsString = tagsString,
            source = source,
            onSuccess = onSuccess
        )
    }
}
