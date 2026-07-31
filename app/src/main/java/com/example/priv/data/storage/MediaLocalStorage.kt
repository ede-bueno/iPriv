package com.example.priv.data.storage

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.InputStream
import java.security.MessageDigest

class MediaLocalStorage(private val context: Context) {

    fun saveMediaFile(
        sourceUri: Uri,
        spaceId: String,
        memoryId: String,
        attachmentId: String,
        extension: String
    ): SavedMediaResult? {
        return try {
            val dir = File(context.filesDir, "memories_media/$spaceId/$memoryId").apply {
                if (!exists()) mkdirs()
            }
            val extClean = if (extension.startsWith(".")) extension else ".$extension"
            val destFile = File(dir, "$attachmentId$extClean")

            var bytesCopied = 0L
            val digest = MessageDigest.getInstance("SHA-256")

            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                destFile.outputStream().use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        digest.update(buffer, 0, bytesRead)
                        bytesCopied += bytesRead
                    }
                }
            } ?: return null

            val checksumHex = digest.digest().joinToString("") { "%02x".format(it) }

            SavedMediaResult(
                localPath = destFile.absolutePath,
                fileSize = bytesCopied,
                checksum = checksumHex
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    data class SavedMediaResult(
        val localPath: String,
        val fileSize: Long,
        val checksum: String
    )
}
