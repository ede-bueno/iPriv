package com.example.priv.data.repository

import com.example.priv.data.dao.ScrapbookDao
import com.example.priv.data.entity.ScrapbookEntity
import kotlinx.coroutines.flow.Flow

class ScrapbookRepository(private val scrapbookDao: ScrapbookDao) {

    val allScrapbooks: Flow<List<ScrapbookEntity>> = scrapbookDao.getAllScrapbooks()

    suspend fun getScrapbookById(id: String): ScrapbookEntity? {
        return scrapbookDao.getScrapbookById(id)
    }

    suspend fun createScrapbook(
        title: String,
        description: String,
        styleTheme: String = "NEO_Y2K",
        coverEmojiOrIcon: String = "✨",
        associatedPersonId: String? = null,
        stickers: List<String> = listOf("💖", "⚡", "✨", "🎵"),
        backgroundPattern: String = "PAPER_GRID"
    ): ScrapbookEntity {
        val stickersJson = "[" + stickers.joinToString(",") { "\"$it\"" } + "]"
        val scrapbook = ScrapbookEntity(
            title = title,
            description = description,
            styleTheme = styleTheme,
            coverEmojiOrIcon = coverEmojiOrIcon,
            associatedPersonId = associatedPersonId,
            stickersJson = stickersJson,
            backgroundPattern = backgroundPattern
        )
        scrapbookDao.insertScrapbook(scrapbook)
        return scrapbook
    }

    suspend fun updateScrapbook(scrapbook: ScrapbookEntity) {
        scrapbookDao.updateScrapbook(scrapbook.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateScrapbookItems(id: String, items: List<com.example.priv.data.model.ScrapbookPageItem>) {
        val current = scrapbookDao.getScrapbookById(id) ?: return
        val serialized = com.example.priv.data.model.ScrapbookPageItem.serializeList(items)
        scrapbookDao.updateScrapbook(current.copy(itemsJson = serialized, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteScrapbook(id: String) {
        scrapbookDao.deleteScrapbook(id)
    }
}
