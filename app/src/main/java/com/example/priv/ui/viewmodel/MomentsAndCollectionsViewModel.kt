package com.example.priv.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.priv.data.database.PrivDatabase
import com.example.priv.data.entity.CollectionEntity
import com.example.priv.data.entity.MomentEntity
import com.example.priv.data.model.MemoryWithDetails
import com.example.priv.data.repository.CollectionRepository
import com.example.priv.data.repository.MemoryRepository
import com.example.priv.data.repository.MomentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MomentsAndCollectionsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = PrivDatabase.getInstance(application)
    private val momentRepo = MomentRepository(db.momentDao())
    private val collectionRepo = CollectionRepository(db.collectionDao())
    private val memoryRepo = MemoryRepository(db.memoryDao(), db.tagDao(), db.syncQueueDao())

    val moments: StateFlow<List<MomentEntity>> = momentRepo.allMoments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val collections: StateFlow<List<CollectionEntity>> = collectionRepo.allCollections
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createMoment(
        title: String,
        description: String = "",
        date: String = "",
        category: String = "Geral",
        colorHex: String = "#FF4B72"
    ) {
        viewModelScope.launch {
            val newMoment = MomentEntity(
                title = title,
                description = description,
                date = date,
                category = category,
                colorHex = colorHex
            )
            momentRepo.insertMoment(newMoment)
        }
    }

    fun createCollection(
        title: String,
        description: String = "",
        iconName: String = "Favorite",
        colorHex: String = "#8E3BEE"
    ) {
        viewModelScope.launch {
            val newCollection = CollectionEntity(
                title = title,
                description = description,
                iconName = iconName,
                colorHex = colorHex
            )
            collectionRepo.insertCollection(newCollection)
        }
    }

    fun getMemoriesForMoment(momentId: String): Flow<List<MemoryWithDetails>> {
        return memoryRepo.getMemoriesForMoment(momentId)
    }

    fun getMemoriesForCollection(collectionId: String): Flow<List<MemoryWithDetails>> {
        return memoryRepo.getMemoriesForCollection(collectionId)
    }

    fun toggleFavorite(memory: MemoryWithDetails) {
        viewModelScope.launch {
            memoryRepo.toggleFavorite(memory.memory)
        }
    }

    fun moveToTrash(memoryId: String) {
        viewModelScope.launch {
            memoryRepo.moveToTrash(memoryId)
        }
    }
}
