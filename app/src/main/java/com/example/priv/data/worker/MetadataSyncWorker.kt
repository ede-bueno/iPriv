package com.example.priv.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.priv.data.database.PrivDatabase
import com.example.priv.data.model.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MetadataSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val memoryId = inputData.getString("memory_id")
        val spaceId = inputData.getString("space_id") ?: "default_space"

        Log.i("MetadataSyncWorker", "Iniciando sincronizacao de metadados para memoriaId=$memoryId em spaceId=$spaceId")

        return@withContext try {
            val db = PrivDatabase.getInstance(applicationContext)
            val memoryDao = db.memoryDao()

            if (memoryId != null) {
                val memory = memoryDao.getMemoryByIdDirect(memoryId)
                if (memory != null) {
                    val updatedMemory = memory.memory.copy(
                        syncStatus = SyncStatus.SYNCING,
                        lastSyncAttemptAt = System.currentTimeMillis(),
                        syncAttempts = memory.memory.syncAttempts + 1
                    )
                    memoryDao.updateMemory(updatedMemory)

                    // Simulate remote push / response
                    val syncedMemory = updatedMemory.copy(
                        syncStatus = SyncStatus.SYNCED,
                        lastSyncedAt = System.currentTimeMillis(),
                        lastSyncError = null
                    )
                    memoryDao.updateMemory(syncedMemory)
                    Log.i("MetadataSyncWorker", "Metadados sincronizados com sucesso para memoriaId=$memoryId")
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("MetadataSyncWorker", "Erro na sincronizacao de metadados: ${e.localizedMessage}")
            Result.retry()
        }
    }
}
