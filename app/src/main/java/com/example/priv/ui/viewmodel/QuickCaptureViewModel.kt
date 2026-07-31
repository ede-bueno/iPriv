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

class QuickCaptureViewModel(application: Application) : AndroidViewModel(application) {

    private val db = PrivDatabase.getInstance(application)
    private val memoryRepo = MemoryRepository(db.memoryDao(), db.tagDao(), db.syncQueueDao())
    private val personRepo = PersonRepository(db.personDao())
    private val momentRepo = MomentRepository(db.momentDao())
    private val collectionRepo = CollectionRepository(db.collectionDao())
    private val mediaStorage = MediaLocalStorage(application)

    val persons: StateFlow<List<PersonEntity>> = personRepo.allPersons
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val moments: StateFlow<List<MomentEntity>> = momentRepo.allMoments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val collections: StateFlow<List<CollectionEntity>> = collectionRepo.allCollections
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        viewModelScope.launch {
            val tags = tagsString.split(",").map { it.trim() }.filter { it.isNotBlank() }
            
            // Internal file saving attempt if valid URI
            var localPath = ""
            var checksum = ""
            var finalFileSize = fileSize

            if (audioUri.startsWith("content://") || audioUri.startsWith("file://")) {
                val parsedUri = Uri.parse(audioUri)
                val attachmentId = UUID.randomUUID().toString()
                val tempMemoryId = UUID.randomUUID().toString()
                val saveResult = mediaStorage.saveMediaFile(
                    sourceUri = parsedUri,
                    spaceId = "default_space",
                    memoryId = tempMemoryId,
                    attachmentId = attachmentId,
                    extension = "ogg"
                )
                if (saveResult != null) {
                    localPath = saveResult.localPath
                    checksum = saveResult.checksum
                    finalFileSize = saveResult.fileSize
                }
            }

            val id = memoryRepo.createQuickAudioMemory(
                title = title,
                note = note,
                audioUri = audioUri,
                localPath = localPath,
                checksum = checksum,
                durationMs = durationMs,
                fileSize = finalFileSize,
                waveformData = waveformData,
                transcription = transcription,
                primaryPersonId = primaryPersonId,
                momentId = momentId,
                collectionId = collectionId,
                tags = tags,
                source = source
            )
            onSuccess(id)
        }
    }
}
