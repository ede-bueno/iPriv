package com.example.priv.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.priv.data.model.OperationType
import com.example.priv.data.model.SyncStatus
import java.util.UUID

@Entity(tableName = "sync_queue")
data class SyncOperationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val entityType: String, // "MEMORY", "ATTACHMENT", "PERSON", "MOMENT", etc.
    val entityId: String,
    val operationType: OperationType,
    val payloadJson: String = "",
    val attempts: Int = 0,
    val status: SyncStatus = SyncStatus.PENDING_CREATE,
    val createdAt: Long = System.currentTimeMillis(),
    val nextAttemptAt: Long = System.currentTimeMillis(),
    val lastError: String? = null
)
