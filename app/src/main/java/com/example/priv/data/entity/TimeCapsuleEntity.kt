package com.example.priv.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "time_capsules")
data class TimeCapsuleEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val note: String = "",
    val unlockTimestamp: Long, // Quando a cápsula poderá ser aberta
    val isLocked: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val openedAt: Long? = null,
    val memoryIdsJson: String = "[]", // Lista de IDs de memórias incluídas
    val mediaItemsJson: String = "[]", // Lista de CapsuleMediaItem serializados (fotos, vídeos, textos, músicas, colagens)
    val targetPersonId: String? = null,
    val themeColorHex: String = "#7C4DFF"
)
