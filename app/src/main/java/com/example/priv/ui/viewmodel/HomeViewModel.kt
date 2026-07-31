package com.example.priv.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.priv.data.database.PrivDatabase
import com.example.priv.data.entity.CollectionEntity
import com.example.priv.data.entity.MomentEntity
import com.example.priv.data.entity.PersonEntity
import com.example.priv.data.model.MemoryWithDetails
import com.example.priv.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = PrivDatabase.getInstance(application)
    private val memoryRepo = MemoryRepository(db.memoryDao(), db.tagDao(), db.syncQueueDao())
    private val personRepo = PersonRepository(db.personDao())
    private val momentRepo = MomentRepository(db.momentDao())
    private val collectionRepo = CollectionRepository(db.collectionDao())

    val activeMemories: StateFlow<List<MemoryWithDetails>> = memoryRepo.activeMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteMemories: StateFlow<List<MemoryWithDetails>> = memoryRepo.favoriteMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unorganizedMemories: StateFlow<List<MemoryWithDetails>> = memoryRepo.unorganizedMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val persons: StateFlow<List<PersonEntity>> = personRepo.allPersons
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val moments: StateFlow<List<MomentEntity>> = momentRepo.allMoments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val collections: StateFlow<List<CollectionEntity>> = collectionRepo.allCollections
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _randomNostalgiaMemory = MutableStateFlow<MemoryWithDetails?>(null)
    val randomNostalgiaMemory: StateFlow<MemoryWithDetails?> = _randomNostalgiaMemory.asStateFlow()

    init {
        viewModelScope.launch {
            activeMemories.collect { list ->
                if (list.isNotEmpty() && _randomNostalgiaMemory.value == null) {
                    _randomNostalgiaMemory.value = list.shuffled().firstOrNull()
                }
            }
        }
    }

    fun pickNewNostalgiaMemory() {
        val list = activeMemories.value
        if (list.isNotEmpty()) {
            _randomNostalgiaMemory.value = list.shuffled().firstOrNull()
        }
    }

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
}
