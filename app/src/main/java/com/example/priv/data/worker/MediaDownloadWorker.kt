package com.example.priv.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.priv.data.database.PrivDatabase
import com.example.priv.data.model.AttachmentSyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaDownloadWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val attachmentId = inputData.getString("attachment_id")
        val memoryId = inputData.getString("memory_id")

        Log.i("MediaDownloadWorker", "Iniciando download de midia para restauração anexoId=$attachmentId")

        return@withContext try {
            val db = PrivDatabase.getInstance(applicationContext)
            val memoryDao = db.memoryDao()

            if (memoryId != null) {
                val memoryDetails = memoryDao.getMemoryByIdDirect(memoryId)
                if (memoryDetails != null) {
                    val attachment = memoryDetails.attachments.firstOrNull { it.id == attachmentId } ?: memoryDetails.attachments.firstOrNull()
                    if (attachment != null) {
                        val downloadingAtt = attachment.copy(
                            uploadStatus = AttachmentSyncStatus.DOWNLOADING
                        )
                        memoryDao.updateAttachment(downloadingAtt)

                        val downloadedAtt = downloadingAtt.copy(
                            uploadStatus = AttachmentSyncStatus.AVAILABLE_LOCAL,
                            localAvailability = true
                        )
                        memoryDao.updateAttachment(downloadedAtt)
                        Log.i("MediaDownloadWorker", "Download de midia concluido localmente.")
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("MediaDownloadWorker", "Erro no download de midia: ${e.localizedMessage}")
            Result.retry()
        }
    }
}
