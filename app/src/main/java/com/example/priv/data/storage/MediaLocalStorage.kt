package com.example.priv.data.storage

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.io.InputStream
import java.security.MessageDigest

class MediaLocalStorage(private val context: Context) {

    fun saveMediaFile(
        sourceUri: Uri,
        spaceId: String,
        memoryId: String,
        attachmentId: String,
        extension: String,
        mimeType: String = "*/*"
    ): SavedMediaResult? {
        return try {
            val dir = File(context.filesDir, "memories_media/$spaceId/$memoryId").apply {
                if (!exists()) mkdirs()
            }

            var detectedExt = extension.trim()
            if (detectedExt.isBlank() || detectedExt == "*/*" || detectedExt == "unknown") {
                val typeFromResolver = context.contentResolver.getType(sourceUri) ?: mimeType
                detectedExt = MimeTypeMap.getSingleton().getExtensionFromMimeType(typeFromResolver) ?: when {
                    mimeType.startsWith("audio/") -> "m4a"
                    mimeType.startsWith("image/") -> "jpg"
                    mimeType.startsWith("video/") -> "mp4"
                    else -> "bin"
                }
            }

            val extClean = if (detectedExt.startsWith(".")) detectedExt else ".$detectedExt"
            val destFile = File(dir, "$attachmentId$extClean")

            var bytesCopied = 0L
            val digest = MessageDigest.getInstance("SHA-256")

            val inputStream: InputStream = context.contentResolver.openInputStream(sourceUri) ?: return null
            inputStream.use { input ->
                destFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        digest.update(buffer, 0, bytesRead)
                        bytesCopied += bytesRead
                    }
                }
            }

            if (bytesCopied == 0L || !destFile.exists()) {
                destFile.delete()
                return null
            }

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
