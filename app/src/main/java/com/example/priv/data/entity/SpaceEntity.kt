package com.example.priv.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.priv.data.model.SpaceType
import com.example.priv.data.model.SyncStatus
import java.util.UUID

@Entity(tableName = "spaces")
data class SpaceEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val type: SpaceType = SpaceType.PERSONAL,
    val ownerUserId: String = "local_user",
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
