package com.example.priv.data.util

import android.content.Context
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale

object MediaMetadataExtractor {

    data class ExtractedMediaMetadata(
        val capturedTimestamp: Long? = null,
        val width: Int = 0,
        val height: Int = 0,
        val fileName: String? = null,
        val fileSize: Long? = null,
        val mimeType: String? = null
    )

    /**
     * Extracts capture date/time and dimensions from an image Uri using ExifInterface and MediaStore.
     * Falls back to contentResolver queries or null if no EXIF header is present.
     */
    fun extractMetadata(context: Context, uri: Uri): ExtractedMediaMetadata {
        var capturedTimestamp: Long? = null
        var width = 0
        var height = 0
        var fileName: String? = null
        var fileSize: Long? = null
        val mimeType = context.contentResolver.getType(uri)

        // 1. Try reading ContentResolver projection (MediaStore)
        try {
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.SIZE,
                    MediaStore.MediaColumns.DATE_TAKEN,
                    MediaStore.MediaColumns.DATE_ADDED,
                    MediaStore.MediaColumns.WIDTH,
                    MediaStore.MediaColumns.HEIGHT
                ),
                null,
                null,
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIdx = it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    if (nameIdx >= 0) fileName = it.getString(nameIdx)

                    val sizeIdx = it.getColumnIndex(MediaStore.MediaColumns.SIZE)
                    if (sizeIdx >= 0) fileSize = it.getLong(sizeIdx)

                    val widthIdx = it.getColumnIndex(MediaStore.MediaColumns.WIDTH)
                    if (widthIdx >= 0) width = it.getInt(widthIdx)

                    val heightIdx = it.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
                    if (heightIdx >= 0) height = it.getInt(heightIdx)

                    val dateTakenIdx = it.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
                    if (dateTakenIdx >= 0) {
                        val dateTaken = it.getLong(dateTakenIdx)
                        if (dateTaken > 0L) {
                            capturedTimestamp = dateTaken
                        }
                    }

                    if (capturedTimestamp == null) {
                        val dateAddedIdx = it.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
                        if (dateAddedIdx >= 0) {
                            val dateAddedSec = it.getLong(dateAddedIdx)
                            if (dateAddedSec > 0L) {
                                capturedTimestamp = dateAddedSec * 1000L
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Content provider query may fail on some 3rd-party content Uris
        }

        // 2. Try reading EXIF headers if it is an image
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                val exif = ExifInterface(stream)

                val exifWidth = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
                val exifLength = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
                if (exifWidth > 0 && exifLength > 0) {
                    width = exifWidth
                    height = exifLength
                }

                val dateTimeStr = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)

                if (!dateTimeStr.isNullOrBlank()) {
                    val parsed = parseExifDate(dateTimeStr)
                    if (parsed != null && parsed > 0L) {
                        capturedTimestamp = parsed
                    }
                }
            }
        } catch (_: Exception) {
            // Stream not readable for EXIF or unsupported format
        }

        return ExtractedMediaMetadata(
            capturedTimestamp = capturedTimestamp,
            width = width,
            height = height,
            fileName = fileName,
            fileSize = fileSize,
            mimeType = mimeType
        )
    }

    private fun parseExifDate(dateStr: String): Long? {
        val formats = arrayOf(
            "yyyy:MM:dd HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy:MM:dd",
            "yyyy-MM-dd"
        )
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                val date = sdf.parse(dateStr)
                if (date != null) return date.time
            } catch (_: Exception) {
                // Continue to next format
            }
        }
        return null
    }
}
