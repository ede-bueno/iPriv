package com.example.priv.data.source

import com.example.priv.data.sync.SupabaseClientManager
import java.io.File

class MediaRemoteStorageImpl(
    private val supabaseManager: SupabaseClientManager
) : MediaRemoteStorage {

    override suspend fun uploadMedia(
        spaceId: String,
        memoryId: String,
        attachmentId: String,
        file: File,
        mimeType: String,
        checksum: String,
        onProgress: (bytesUploaded: Long, totalBytes: Long) -> Unit
    ): Result<String> {
        val res = supabaseManager.uploadMediaStorage(
            spaceId = spaceId,
            memoryId = memoryId,
            attachmentId = attachmentId,
            file = file,
            mimeType = mimeType
        )
        onProgress(file.length(), file.length())
        return res
    }

    override suspend fun downloadMedia(
        remotePath: String,
        destinationFile: File,
        expectedChecksum: String,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit
    ): Result<File> {
        val res = supabaseManager.downloadMediaStorage(
            remotePath = remotePath,
            destinationFile = destinationFile
        )
        if (res.isSuccess) {
            onProgress(destinationFile.length(), destinationFile.length())
        }
        return res
    }

    override suspend fun deleteMedia(remotePath: String): Result<Unit> {
        return Result.success(Unit)
    }
}
