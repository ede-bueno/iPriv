package com.example.priv.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.priv.data.model.SyncStatus
import java.util.UUID

@Entity(
    tableName = "moments",
    indices = [Index(value = ["spaceId"])]
)
data class MomentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val spaceId: String = "default_space",
    val title: String,
    val description: String = "",
    val date: String = "",
    val category: String = "Geral",
    val coverUri: String = "",
    val colorHex: String = "#8E3BEE",
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
