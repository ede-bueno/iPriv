package com.example.priv.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Entidade Room para armazenar metadados detalhados de memórias de áudio
 * e suas transcrições de texto associadas de forma local e segura.
 */
@Entity(
    tableName = "audio_memories",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["linkedMemoryId"]),
        Index(value = ["isFavorite"]),
        Index(value = ["transcriptionStatus"])
    ]
)
data class AudioMemoryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    // Metadados de identificação
    val title: String,
    val fileName: String,
    val filePath: String,
    val fileUri: String,
    val mimeType: String = "audio/m4a",
    
    // Metadados técnicos do áudio
    val fileSize: Long = 0L,         // Tamanho em bytes
    val durationMs: Long = 0L,       // Duração em milissegundos
    val sampleRate: Int = 44100,     // Taxa de amostragem em Hz
    val channels: Int = 1,           // 1 = Mono, 2 = Stereo
    val bitrate: Int = 128000,       // Bitrate em bps
    val waveformData: String = "",   // Amplitudes normalizadas para desenho de onda
    val audioChecksum: String = "",  // Hash de verificação de integridade
    
    // Transcrição de texto associada
    val transcriptionText: String = "",
    val transcriptionStatus: String = "COMPLETED", // PENDING, TRANSCRIBING, COMPLETED, EDITED, FAILED
    val transcriptionLanguage: String = "pt-BR",
    val transcriptionConfidence: Float = 1.0f,
    val transcriptionEngine: String = "LOCAL_AI", // GEMINI_AI, LOCAL_AI, MANUAL_INPUT
    
    // Metadados contextuais
    val summary: String = "",
    val tags: String = "",
    val speakerName: String = "Você",
    val linkedMemoryId: String? = null, // Vínculo com uma memória geral existente, se houver
    val isFavorite: Boolean = false,
    val source: String = "MICROPHONE",  // MICROPHONE, WHATSAPP, FILE_IMPORT, SYSTEM
    
    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val formattedDuration: String
        get() {
            val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%02d:%02d".format(minutes, seconds)
        }

    val formattedSize: String
        get() {
            return when {
                fileSize >= 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f MB", fileSize.toDouble() / (1024 * 1024))
                fileSize >= 1024 -> String.format(java.util.Locale.US, "%.1f KB", fileSize.toDouble() / 1024)
                else -> "$fileSize B"
            }
        }
}
