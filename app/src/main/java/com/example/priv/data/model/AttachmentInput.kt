package com.example.priv.data.model

data class AttachmentInput(
    val type: String, // "AUDIO", "IMAGE", "VIDEO", "TEXT", "LINK"
    val uri: String = "",
    val localPath: String = "",
    val fileName: String = "",
    val fileSize: Long = 0L,
    val mimeType: String = "image/jpeg",
    val checksum: String = "",
    val durationMs: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val waveformData: String = "",
    val textContent: String = "",
    val capturedTimestamp: Long? = null
)
