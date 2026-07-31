package com.example.priv.data.source

import java.io.File

interface MediaRemoteStorage {
    suspend fun uploadMedia(
        spaceId: String,
        memoryId: String,
        attachmentId: String,
        file: File,
        mimeType: String,
        checksum: String,
        onProgress: (bytesUploaded: Long, totalBytes: Long) -> Unit
    ): Result<String> // Returns remotePath: "spaces/{spaceId}/memories/{memoryId}/{attachmentId}.ext"

    suspend fun downloadMedia(
        remotePath: String,
        destinationFile: File,
        expectedChecksum: String,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit
    ): Result<File>

    suspend fun deleteMedia(remotePath: String): Result<Unit>
}
