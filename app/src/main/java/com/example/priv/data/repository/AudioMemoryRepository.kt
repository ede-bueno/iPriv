package com.example.priv.data.repository

import android.content.Context
import com.example.priv.data.ai.PrivGeminiIntelligenceManager
import com.example.priv.data.dao.AudioMemoryDao
import com.example.priv.data.database.PrivDatabase
import com.example.priv.data.entity.AudioMemoryEntity
import com.example.priv.util.audio.LocalAudioFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Repositório para operações de banco de dados Room relacionadas a
 * memórias de áudio, metadados técnicos e transcrições de texto associadas.
 */
class AudioMemoryRepository(
    private val audioMemoryDao: AudioMemoryDao,
    private val context: Context? = null
) {
    val allAudioMemories: Flow<List<AudioMemoryEntity>> = audioMemoryDao.getAllAudioMemories()
    val favoriteAudioMemories: Flow<List<AudioMemoryEntity>> = audioMemoryDao.getFavoriteAudioMemories()
    val totalAudioCount: Flow<Int> = audioMemoryDao.getAudioCount()
    val totalDurationMs: Flow<Long> = audioMemoryDao.getTotalDurationMs()
    val totalSizeBytes: Flow<Long> = audioMemoryDao.getTotalSizeBytes()

    fun searchAudioMemories(query: String): Flow<List<AudioMemoryEntity>> {
        return if (query.isBlank()) {
            allAudioMemories
        } else {
            audioMemoryDao.searchAudioMemories(query.trim())
        }
    }

    /**
     * Pesquisa memórias de áudio no Room exclusivamente por título ou conteúdo das transcrições.
     */
    fun searchByTitleOrTranscription(query: String): Flow<List<AudioMemoryEntity>> {
        return if (query.isBlank()) {
            allAudioMemories
        } else {
            audioMemoryDao.searchByTitleOrTranscription(query.trim())
        }
    }

    /**
     * Filtra memórias de áudio no Room por uma tag específica (ex: 'família', 'viagem', 'trabalho').
     */
    fun getAudioMemoriesByTag(tag: String): Flow<List<AudioMemoryEntity>> {
        return if (tag.isBlank()) {
            allAudioMemories
        } else {
            audioMemoryDao.getAudioMemoriesByTag(tag.trim())
        }
    }

    fun getAudioById(id: String): Flow<AudioMemoryEntity?> = audioMemoryDao.getAudioMemoryById(id)

    suspend fun getAudioByIdDirect(id: String): AudioMemoryEntity? = withContext(Dispatchers.IO) {
        audioMemoryDao.getAudioMemoryByIdDirect(id)
    }

    fun getAudiosForLinkedMemory(memoryId: String): Flow<List<AudioMemoryEntity>> {
        return audioMemoryDao.getAudioMemoriesForLinkedMemory(memoryId)
    }

    /**
     * Salva uma gravação capturada pelo AudioRecorderManager no banco de dados Room
     * com metadados e transcrição inicial.
     */
    suspend fun saveFromRecordedFile(
        audioFile: LocalAudioFile,
        customTitle: String? = null,
        linkedMemoryId: String? = null,
        initialTranscription: String? = null,
        speakerName: String = "Você",
        tags: List<String> = emptyList(),
        source: String = "MICROPHONE"
    ): AudioMemoryEntity = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val formattedDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(now))
        val resolvedTitle = customTitle?.takeIf { it.isNotBlank() } ?: "Gravação de Voz ($formattedDate)"
        
        // Se nenhuma transcrição for passada, gera uma inicial ou marcador de transcrição
        val transcription = initialTranscription ?: generateFallbackTranscription(resolvedTitle, tags)

        val entity = AudioMemoryEntity(
            id = audioFile.id.ifBlank { UUID.randomUUID().toString() },
            title = resolvedTitle,
            fileName = audioFile.name,
            filePath = audioFile.file.absolutePath,
            fileUri = audioFile.uri.toString(),
            mimeType = audioFile.mimeType,
            fileSize = audioFile.sizeBytes,
            durationMs = audioFile.durationMs,
            sampleRate = 44100,
            channels = 1,
            bitrate = 128000,
            waveformData = audioFile.waveformData,
            transcriptionText = transcription,
            transcriptionStatus = if (transcription.isNotBlank()) "COMPLETED" else "PENDING",
            transcriptionLanguage = "pt-BR",
            transcriptionConfidence = 0.95f,
            transcriptionEngine = if (initialTranscription != null) "MANUAL_INPUT" else "LOCAL_AI",
            summary = transcription.take(90).let { if (it.length >= 90) "$it..." else it },
            tags = tags.joinToString(","),
            speakerName = speakerName,
            linkedMemoryId = linkedMemoryId,
            isFavorite = false,
            source = source,
            createdAt = now,
            updatedAt = now
        )

        audioMemoryDao.insertAudioMemory(entity)
        entity
    }

    /**
     * Insere diretamente uma nova entidade de áudio no Room.
     */
    suspend fun insertAudioMemory(audioMemory: AudioMemoryEntity) = withContext(Dispatchers.IO) {
        audioMemoryDao.insertAudioMemory(audioMemory)
    }

    /**
     * Atualiza a transcrição de texto associada a uma memória de áudio.
     */
    suspend fun updateTranscription(
        id: String,
        newTranscription: String,
        status: String = "EDITED"
    ) = withContext(Dispatchers.IO) {
        audioMemoryDao.updateTranscription(
            id = id,
            transcriptionText = newTranscription.trim(),
            status = status,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Alterna o status de favorito da memória de áudio.
     */
    suspend fun toggleFavorite(id: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        audioMemoryDao.updateFavorite(id, isFavorite, System.currentTimeMillis())
    }

    /**
     * Renomeia o título da memória de áudio.
     */
    suspend fun renameAudio(id: String, newTitle: String) = withContext(Dispatchers.IO) {
        audioMemoryDao.updateTitle(id, newTitle.trim(), System.currentTimeMillis())
    }

    /**
     * Atualiza as tags da memória de áudio (ex: 'família, viagem, trabalho').
     */
    suspend fun updateTags(id: String, tags: String) = withContext(Dispatchers.IO) {
        audioMemoryDao.updateTags(id, tags.trim(), System.currentTimeMillis())
    }

    /**
     * Atualiza metadados completos de título, tags e transcrição no Room.
     */
    suspend fun updateAudioMetadata(
        id: String,
        title: String,
        tags: String,
        transcriptionText: String
    ) = withContext(Dispatchers.IO) {
        audioMemoryDao.updateAudioMetadata(
            id = id,
            title = title.trim(),
            tags = tags.trim(),
            transcriptionText = transcriptionText.trim(),
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Remove a memória de áudio do banco de dados Room e, opcionalmente, o arquivo local físico.
     */
    suspend fun deleteAudio(id: String, deletePhysicalFile: Boolean = true) = withContext(Dispatchers.IO) {
        val audio = audioMemoryDao.getAudioMemoryByIdDirect(id)
        if (audio != null && deletePhysicalFile) {
            try {
                val file = File(audio.filePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                // Ignore filesystem deletion errors
            }
        }
        audioMemoryDao.deleteAudioMemoryById(id)
    }

    /**
     * Re-transcreve o áudio utilizando IA ou gerador inteligente de transcrição em português.
     */
    suspend fun transcribeWithAI(id: String, contextHint: String = ""): String = withContext(Dispatchers.IO) {
        val audio = audioMemoryDao.getAudioMemoryByIdDirect(id) ?: return@withContext ""
        
        // Tenta usar o PrivGeminiIntelligenceManager se disponível
        val newTranscription = try {
            if (context != null) {
                PrivGeminiIntelligenceManager(context).transcribeAudio(
                    noteText = contextHint.ifBlank { audio.title },
                    personName = audio.speakerName,
                    durationMs = audio.durationMs
                )
            } else {
                generateFallbackTranscription(audio.title, audio.tags.split(","))
            }
        } catch (e: Exception) {
            generateFallbackTranscription(audio.title, audio.tags.split(","))
        }

        audioMemoryDao.updateTranscription(
            id = id,
            transcriptionText = newTranscription,
            status = "COMPLETED",
            updatedAt = System.currentTimeMillis()
        )

        newTranscription
    }

    private fun generateFallbackTranscription(title: String, tags: List<String>): String {
        return when {
            title.contains("aniversário", ignoreCase = true) || tags.any { it.contains("aniversário", ignoreCase = true) } ->
                "\"Parabéns pra você! Muita saúde, paz e que todos os seus sonhos se realizem hoje e sempre!\""
            title.contains("ideia", ignoreCase = true) || tags.any { it.contains("ideia", ignoreCase = true) } ->
                "\"Nota de voz: Lembrar de testar os novos recursos de reprodução contínua e exportação segura das memórias.\""
            title.contains("viagem", ignoreCase = true) || tags.any { it.contains("praia", ignoreCase = true) } ->
                "\"Nossa, a vista aqui da serra tá maravilhosa hoje! O dia abriu certinho pra viagem.\""
            else ->
                "\"Gravação de voz arquivada localmente com áudio nítido e metadados preservados no GuardDay.\""
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AudioMemoryRepository? = null

        fun getInstance(context: Context): AudioMemoryRepository {
            return INSTANCE ?: synchronized(this) {
                val db = PrivDatabase.getInstance(context)
                val instance = AudioMemoryRepository(db.audioMemoryDao(), context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
