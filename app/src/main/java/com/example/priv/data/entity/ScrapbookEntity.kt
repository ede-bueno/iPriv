package com.example.priv.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "scrapbooks")
data class ScrapbookEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val styleTheme: String = "NEO_Y2K", // NEO_Y2K, POLAROID_VINTAGE, AESTHETIC_JOURNAL, MINIMAL_VAULT, FAMILY_HERITAGE
    val coverEmojiOrIcon: String = "✨",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val associatedPersonId: String? = null,
    val stickersJson: String = "[]", // Stickers decorativos: 💖, 🎸, ⚡, 🎧, 🍀, etc.
    val backgroundPattern: String = "PAPER_GRID" // GRADIENT, PAPER_GRID, DARK_NEBULA, PASTEL_CREAM
)
