package com.example.priv.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.priv.data.model.AttachmentSyncStatus
import java.util.UUID

@Entity(
    tableName = "memory_attachments",
    indices = [Index(value = ["memoryId"])]
)
data class MemoryAttachmentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val memoryId: String,
    val type: String, // "AUDIO", "IMAGE", "VIDEO", "TEXT", "LINK"
    val uri: String, // Legacy or internal URI
    val localPath: String = "",
    val remotePath: String? = null,
    val fileName: String = "",
    val fileSize: Long = 0L,
    val mimeType: String = "audio/ogg",
    val checksum: String = "", // SHA-256
    val durationMs: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val waveformData: String = "",
    val transcription: String = "",
    val summary: String = "",
    val uploadStatus: AttachmentSyncStatus = AttachmentSyncStatus.LOCAL_ONLY,
    val localAvailability: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val serverVersion: Long? = null,
    val lastSyncedAt: Long? = null,
    val lastSyncError: String? = null,
    val syncAttempts: Int = 0,
    val lastSyncAttemptAt: Long? = null,
    val nextSyncAttemptAt: Long? = null
)
