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

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = PrivDatabase.getInstance(application)
    private val memoryRepo = MemoryRepository(db.memoryDao(), db.tagDao(), db.syncQueueDao())

    private val activeMemories = memoryRepo.activeMemories
    private val trashedMemories = memoryRepo.trashedMemories

    val searchQuery = MutableStateFlow("")
    val activeFilter = MutableStateFlow(LibraryFilter.ALL)

    val filteredMemories: StateFlow<List<MemoryWithDetails>> = combine(
        activeMemories,
        trashedMemories,
        searchQuery,
        activeFilter
    ) { active, trashed, query, filter ->
        val sourceList = if (filter == LibraryFilter.TRASH) trashed else active

        val filteredByTab = when (filter) {
            LibraryFilter.ALL -> sourceList
            LibraryFilter.FAVORITE -> sourceList.filter { it.memory.isFavorite }
            LibraryFilter.AUDIO -> sourceList.filter { m -> m.attachments.any { it.type == "AUDIO" } }
            LibraryFilter.UNORGANIZED -> sourceList.filter { it.memory.primaryPersonId == null && it.memory.momentId == null }
            LibraryFilter.TRASH -> sourceList
        }

        if (query.isBlank()) {
            filteredByTab
        } else {
            val q = query.trim().lowercase()
            filteredByTab.filter { m ->
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

    fun deletePermanently(memoryId: String) {
        viewModelScope.launch {
            memoryRepo.deletePermanently(memoryId)
        }
    }
}
