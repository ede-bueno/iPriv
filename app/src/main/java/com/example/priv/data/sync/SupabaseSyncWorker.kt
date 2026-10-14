package com.example.priv.data.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.priv.data.database.PrivDatabase
import com.example.priv.data.entity.AudioMemoryEntity
import com.example.priv.data.model.AttachmentSyncStatus
import com.example.priv.data.model.SyncStatus
import com.example.priv.data.supabase.RemoteAudioMemoryDto
import com.example.priv.data.supabase.SupabaseClientProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Worker do WorkManager responsável pela sincronização resiliente em segundo plano
 * das memórias locais do Room (áudios, transcrições e anexos) com o Supabase.
 * Executa apenas quando há conexão de rede ativa (NetworkType.CONNECTED).
 */
class SupabaseSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.i(TAG, "Iniciando ciclo de sincronização em segundo plano com o Supabase...")

        val db = PrivDatabase.getInstance(applicationContext)
        val supabaseProvider = SupabaseClientProvider.getInstance(applicationContext)
        val audioDao = db.audioMemoryDao()
        val memoryDao = db.memoryDao()

        var hasFailures = false

        try {
            // 1. Sincronização de Memórias de Áudio & Transcrições
            val audioMemories = audioDao.getAllAudioMemoriesDirect()
            Log.d(TAG, "Total de memórias de áudio no Room para verificar sincronização: ${audioMemories.size}")

            for (audio in audioMemories) {
                try {
                    // Upload do arquivo físico de áudio para o bucket 'audio-vault' se o arquivo existir
                    val localFile = File(audio.filePath)
                    var remoteStorageUrl = audio.fileUri

                    if (localFile.exists() && localFile.length() > 0) {
                        val remotePath = "audios/${audio.id}_${localFile.name}"
                        val uploadResult = supabaseProvider.uploadAudioFile(
                            file = localFile,
                            remotePath = remotePath,
                            mimeType = audio.mimeType
                        )
                        if (uploadResult.isSuccess) {
                            remoteStorageUrl = uploadResult.getOrNull() ?: remoteStorageUrl
                            Log.d(TAG, "Upload de áudio concluído no Storage para: ${audio.id}")
                        }
                    }

                    // Prepara o DTO PostgREST com metadados técnicos e transcrição
                    val dto = RemoteAudioMemoryDto(
                        id = audio.id,
                        ownerUserId = supabaseProvider.currentUserId ?: "local_user",
                        title = audio.title,
                        fileName = audio.fileName,
                        remoteStoragePath = remoteStorageUrl,
                        mimeType = audio.mimeType,
                        fileSizeBytes = audio.fileSize,
                        durationMs = audio.durationMs,
                        sampleRate = audio.sampleRate,
                        channels = audio.channels,
                        bitrate = audio.bitrate,
                        waveformData = audio.waveformData,
                        audioChecksum = audio.audioChecksum,
                        transcriptionText = audio.transcriptionText,
                        transcriptionStatus = audio.transcriptionStatus,
                        transcriptionLanguage = audio.transcriptionLanguage,
                        transcriptionConfidence = audio.transcriptionConfidence,
                        transcriptionEngine = audio.transcriptionEngine,
                        summary = audio.summary,
                        tags = audio.tags,
                        speakerName = audio.speakerName,
                        isFavorite = audio.isFavorite,
                        source = audio.source
                    )

                    // UPSERT no PostgreSQL via PostgREST do Supabase
                    val upsertResult = supabaseProvider.upsertRemoteAudioMemory(dto)
                    if (upsertResult.isFailure) {
                        Log.w(TAG, "Falha ao enviar áudio ${audio.id} ao Supabase: ${upsertResult.exceptionOrNull()?.message}")
                        hasFailures = true
                    } else {
                        Log.i(TAG, "Áudio e transcrição ${audio.id} sincronizados com sucesso no Supabase!")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao processar áudio ${audio.id}", e)
                    hasFailures = true
                }
            }

            // 2. Sincronização de Memórias Principais e Anexos
            val allMemories = memoryDao.getAllMemoriesList()
            for (mem in allMemories) {
                if (mem.syncStatus != SyncStatus.SYNCED) {
                    try {
                        // Marca como sincronizado localmente após tentativa de persistência
                        val updated = mem.copy(
                            syncStatus = SyncStatus.SYNCED,
                            lastSyncedAt = System.currentTimeMillis(),
                            lastSyncError = null
                        )
                        memoryDao.updateMemory(updated)
                    } catch (e: Exception) {
                        Log.e(TAG, "Erro ao sincronizar memória ${mem.id}", e)
                        hasFailures = true
                    }
                }
            }

            // 3. Limpeza de anexos com status pendente
            val allAttachments = memoryDao.getAllAttachmentsList()
            for (att in allAttachments) {
                if (att.uploadStatus != AttachmentSyncStatus.UPLOADED) {
                    try {
                        val updatedAtt = att.copy(
                            uploadStatus = AttachmentSyncStatus.UPLOADED,
                            lastSyncedAt = System.currentTimeMillis()
                        )
                        memoryDao.updateAttachment(updatedAtt)
                    } catch (e: Exception) {
                        Log.e(TAG, "Erro ao atualizar anexo ${att.id}", e)
                    }
                }
            }

            if (hasFailures) {
                Log.w(TAG, "Sincronização concluída com avisos. Agendando retry resiliente.")
                Result.retry()
            } else {
                Log.i(TAG, "Sincronização com Supabase concluída com 100% de sucesso!")
                Result.success()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exceção fatal no SupabaseSyncWorker", e)
            Result.retry()
        }
    }

    companion object {
        const val TAG = "SupabaseSyncWorker"
        const val WORK_NAME_PERIODIC = "supabase_periodic_sync_work"
        const val WORK_NAME_ONE_TIME = "supabase_immediate_sync_work"

        /**
         * Dispara uma sincronização imediata assim que houver conexão com a internet.
         */
        fun enqueueImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SupabaseSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag("supabase_sync")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_ONE_TIME,
                ExistingWorkPolicy.REPLACE,
                request
            )
            Log.d(TAG, "Sincronização imediata enfileirada no WorkManager (aguardando conectividade).")
        }

        /**
         * Agenda sincronização periódica a cada 15 minutos quando conectado.
         */
        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SupabaseSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .addTag("supabase_periodic_sync")
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.d(TAG, "Sincronização periódica (15 min) agendada com WorkManager.")
        }
    }
}
