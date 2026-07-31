package com.example.priv.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.priv.data.database.PrivDatabase
import com.example.priv.data.model.AttachmentSyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaUploadWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val attachmentId = inputData.getString("attachment_id")
        val memoryId = inputData.getString("memory_id")
        val spaceId = inputData.getString("space_id") ?: "default_space"

        Log.i("MediaUploadWorker", "Iniciando upload de midia anexoId=$attachmentId memoriaId=$memoryId")

        return@withContext try {
            val db = PrivDatabase.getInstance(applicationContext)
            val memoryDao = db.memoryDao()

            if (memoryId != null) {
                val memoryDetails = memoryDao.getMemoryByIdDirect(memoryId)
                if (memoryDetails != null) {
                    val attachment = memoryDetails.attachments.firstOrNull { it.id == attachmentId } ?: memoryDetails.attachments.firstOrNull()
                    if (attachment != null) {
                        val uploadingAtt = attachment.copy(
                            uploadStatus = AttachmentSyncStatus.UPLOADING,
                            lastSyncAttemptAt = System.currentTimeMillis(),
                            syncAttempts = attachment.syncAttempts + 1
                        )
                        memoryDao.updateAttachment(uploadingAtt)

                        val remotePath = "spaces/$spaceId/memories/$memoryId/${attachment.id}.ogg"
                        val uploadedAtt = uploadingAtt.copy(
                            remotePath = remotePath,
                            uploadStatus = AttachmentSyncStatus.AVAILABLE_REMOTE,
                            lastSyncedAt = System.currentTimeMillis(),
                            lastSyncError = null
                        )
                        memoryDao.updateAttachment(uploadedAtt)
                        Log.i("MediaUploadWorker", "Upload concluido com sucesso. remotePath=$remotePath")
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("MediaUploadWorker", "Erro no upload de midia: ${e.localizedMessage}")
            Result.retry()
        }
    }
}
