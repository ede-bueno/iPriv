package com.example.priv.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.priv.data.database.PrivDatabase
import com.example.priv.data.entity.ScrapbookEntity
import com.example.priv.data.entity.TimeCapsuleEntity
import com.example.priv.data.model.CapsuleMediaItem
import com.example.priv.data.model.MemoryWithDetails
import com.example.priv.data.repository.MemoryRepository
import com.example.priv.data.repository.PersonRepository
import com.example.priv.data.repository.ScrapbookRepository
import com.example.priv.data.repository.TimeCapsuleRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TimelineScrapbookViewModel(application: Application) : AndroidViewModel(application) {

    private val db = PrivDatabase.getInstance(application)
    private val memoryRepo = MemoryRepository(db.memoryDao(), db.tagDao(), db.syncQueueDao())
    private val personRepo = PersonRepository(db.personDao())
    private val capsuleRepo = TimeCapsuleRepository(db.timeCapsuleDao())
    private val scrapbookRepo = ScrapbookRepository(db.scrapbookDao())

    val activeMemories: StateFlow<List<MemoryWithDetails>> = memoryRepo.activeMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val persons = personRepo.allPersons
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val timeCapsules: StateFlow<List<TimeCapsuleEntity>> = capsuleRepo.allCapsules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scrapbooks: StateFlow<List<ScrapbookEntity>> = scrapbookRepo.allScrapbooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createTimeCapsule(
        title: String,
        note: String,
        unlockDaysInFuture: Int,
        memoryIds: List<String>,
        mediaItems: List<CapsuleMediaItem> = emptyList(),
        targetPersonId: String? = null,
        themeColorHex: String = "#7C4DFF"
    ) {
        viewModelScope.launch {
            val unlockTime = System.currentTimeMillis() + (unlockDaysInFuture.toLong() * 24 * 60 * 60 * 1000L)
            capsuleRepo.createCapsule(
                title = title,
                note = note,
                unlockTimestamp = unlockTime,
                memoryIds = memoryIds,
                mediaItems = mediaItems,
                targetPersonId = targetPersonId,
                themeColorHex = themeColorHex
            )
        }
    }

    fun unlockCapsule(capsuleId: String) {
        viewModelScope.launch {
            capsuleRepo.unlockCapsule(capsuleId)
        }
    }

    fun deleteCapsule(capsuleId: String) {
        viewModelScope.launch {
            capsuleRepo.deleteCapsule(capsuleId)
        }
    }

    fun createScrapbook(
        title: String,
        description: String,
        styleTheme: String,
        coverEmoji: String,
        stickers: List<String>
    ) {
        viewModelScope.launch {
            scrapbookRepo.createScrapbook(
                title = title,
                description = description,
                styleTheme = styleTheme,
                coverEmojiOrIcon = coverEmoji,
                stickers = stickers
            )
        }
    }

    fun updateScrapbook(scrapbook: ScrapbookEntity) {
        viewModelScope.launch {
            scrapbookRepo.updateScrapbook(scrapbook)
        }
    }

    fun saveScrapbookItems(scrapbookId: String, items: List<com.example.priv.data.model.ScrapbookPageItem>) {
        viewModelScope.launch {
            scrapbookRepo.updateScrapbookItems(scrapbookId, items)
        }
    }

    fun deleteScrapbook(scrapbookId: String) {
        viewModelScope.launch {
            scrapbookRepo.deleteScrapbook(scrapbookId)
        }
    }
}
