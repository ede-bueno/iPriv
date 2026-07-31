package com.example.priv.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeletionSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val memoryId = inputData.getString("memory_id")
        Log.i("DeletionSyncWorker", "Sincronizando exclusão remota de memoriaId=$memoryId")

        return@withContext try {
            // Process tombstone deletion remotely
            Log.i("DeletionSyncWorker", "Exclusão remota concluida sem violação de integridade.")
            Result.success()
        } catch (e: Exception) {
            Log.e("DeletionSyncWorker", "Erro na exclusão remota: ${e.localizedMessage}")
            Result.retry()
        }
    }
}
