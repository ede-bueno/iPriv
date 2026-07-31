package com.example.priv.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.priv.data.model.SyncStatus
import java.util.UUID

@Entity(
    tableName = "memories",
    indices = [
        Index(value = ["spaceId"]),
        Index(value = ["primaryPersonId"]),
        Index(value = ["momentId"]),
        Index(value = ["collectionId"])
    ]
)
data class MemoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val spaceId: String = "default_space",
    val title: String,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val source: String = "WhatsApp",
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val inTrash: Boolean = false,
    val trashedAt: Long? = null,
    val primaryPersonId: String? = null,
    val momentId: String? = null,
    val collectionId: String? = null,
    
    // Ownership & Authoring
    val ownerUserId: String = "local_user",
    val createdByUserId: String = "local_user",
    val updatedByUserId: String = "local_user",
    val deviceId: String = "local_device",
    
    // Offline-first Sync attributes
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
    val serverVersion: Long? = null,
    val lastSyncedAt: Long? = null,
    val lastSyncError: String? = null,
    val syncAttempts: Int = 0,
    val lastSyncAttemptAt: Long? = null,
    val nextSyncAttemptAt: Long? = null
)
