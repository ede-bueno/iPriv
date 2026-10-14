package com.example.priv.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.priv.data.database.PrivDatabase
import com.example.priv.data.model.MemoryWithDetails
import com.example.priv.data.repository.MemoryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class LibraryFilter {
    ALL, FAVORITE, AUDIO, UNORGANIZED, TRASH
}

data class LibraryFilterState(
    val query: String = "",
    val filter: LibraryFilter = LibraryFilter.ALL,
    val source: String = "TODOS",
    val emotion: String = "TODAS"
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = PrivDatabase.getInstance(application)
    private val memoryRepo = MemoryRepository(db.memoryDao(), db.tagDao(), db.syncQueueDao())

    private val activeMemories = memoryRepo.activeMemories
    private val trashedMemories = memoryRepo.trashedMemories

    val searchQuery = MutableStateFlow("")
    val activeFilter = MutableStateFlow(LibraryFilter.ALL)
    val selectedSource = MutableStateFlow("TODOS") // TODOS, WhatsApp, Gravador, Importação
    val selectedEmotionTag = MutableStateFlow("TODAS") // TODAS, engraçado, fofoca, rolê, conselho, familia

    private val filterState = combine(
        searchQuery,
        activeFilter,
        selectedSource,
        selectedEmotionTag
    ) { query, filter, source, emotion ->
        LibraryFilterState(query, filter, source, emotion)
    }

    val filteredMemories: StateFlow<List<MemoryWithDetails>> = combine(
        activeMemories,
        trashedMemories,
        filterState
    ) { active, trashed, fState ->
        val (query, filter, source, emotion) = fState
        val sourceList = if (filter == LibraryFilter.TRASH) trashed else active

        val filteredByTab = when (filter) {
            LibraryFilter.ALL -> sourceList
            LibraryFilter.FAVORITE -> sourceList.filter { it.memory.isFavorite }
            LibraryFilter.AUDIO -> sourceList.filter { m -> m.attachments.any { it.type == "AUDIO" } }
            LibraryFilter.UNORGANIZED -> sourceList.filter { it.memory.primaryPersonId == null && it.memory.momentId == null }
            LibraryFilter.TRASH -> sourceList
        }

        val filteredBySource = if (source == "TODOS") {
            filteredByTab
        } else {
            filteredByTab.filter { m ->
                val src = m.memory.source.lowercase()
                when (source) {
                    "WhatsApp" -> src.contains("whatsapp")
                    "Gravador" -> src.contains("gravador") || src.contains("captura")
                    "Importação" -> src.contains("import") || src.contains("arquivo") || src.contains("manual")
                    else -> true
                }
            }
        }

        val filteredByEmotion = if (emotion == "TODAS") {
            filteredBySource
        } else {
            val tagQuery = emotion.lowercase().removePrefix("#")
            filteredBySource.filter { m ->
                m.tags.any { it.name.lowercase().contains(tagQuery) } ||
                m.memory.note.lowercase().contains(tagQuery) ||
                m.memory.title.lowercase().contains(tagQuery)
            }
        }

        if (query.isBlank()) {
            filteredByEmotion
        } else {
            val q = query.trim().lowercase()
            filteredByEmotion.filter { m ->
                m.memory.title.lowercase().contains(q) ||
                m.memory.note.lowercase().contains(q) ||
                m.primaryPerson?.name?.lowercase()?.contains(q) == true ||
                m.tags.any { it.name.lowercase().contains(q) } ||
                m.attachments.any { it.transcription.lowercase().contains(q) }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleFavorite(memoryWithDetails: MemoryWithDetails) {
        viewModelScope.launch {
            memoryRepo.toggleFavorite(memoryWithDetails.memory)
        }
    }

    fun moveToTrash(memoryId: String) {
        viewModelScope.launch {
            memoryRepo.moveToTrash(memoryId)
        }
    }

    fun restoreFromTrash(memoryId: String) {
        viewModelScope.launch {
            memoryRepo.restoreFromTrash(memoryId)
        }
    }

    fun updateMemoryFull(
        memoryId: String,
        newTitle: String,
        newNote: String,
        tags: List<String> = emptyList(),
        newAttachments: List<com.example.priv.data.model.AttachmentInput> = emptyList(),
        deletedAttachmentIds: List<String> = emptyList(),
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            memoryRepo.updateMemoryFull(
                memoryId = memoryId,
                newTitle = newTitle,
                newNote = newNote,
                tags = tags,
                newAttachments = newAttachments,
                deletedAttachmentIds = deletedAttachmentIds
            )
            onSuccess()
        }
    }

    fun deletePermanently(memoryId: String) {
        viewModelScope.launch {
            memoryRepo.deletePermanently(memoryId)
        }
    }
}
