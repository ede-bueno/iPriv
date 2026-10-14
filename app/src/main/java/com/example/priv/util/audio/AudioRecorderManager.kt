package com.example.priv.util.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Status of the audio recording session.
 */
sealed interface AudioRecordingState {
    object Idle : AudioRecordingState
    
    data class Recording(
        val file: File,
        val elapsedDurationMs: Long,
        val currentAmplitude: Float, // Normalized 0.0f - 1.0f
        val waveformHistory: List<Float> = emptyList(),
        val isPaused: Boolean = false
    ) : AudioRecordingState
    
    data class Completed(
        val file: File,
        val durationMs: Long,
        val sizeBytes: Long,
        val mimeType: String,
        val waveformData: String
    ) : AudioRecordingState
    
    data class Error(val message: String, val throwable: Throwable? = null) : AudioRecordingState
}

/**
 * Represents a stored local audio file managed by GuardDay/Priv.
 */
data class LocalAudioFile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val file: File,
    val uri: Uri,
    val sizeBytes: Long,
    val durationMs: Long,
    val createdAt: Long,
    val mimeType: String = "audio/m4a",
    val waveformData: String = ""
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
                sizeBytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", sizeBytes.toDouble() / (1024 * 1024))
                sizeBytes >= 1024 -> String.format(Locale.US, "%.1f KB", sizeBytes.toDouble() / 1024)
                else -> "$sizeBytes B"
            }
        }
}

/**
 * Audio Recording and Local Audio File Management Utility.
 * 
 * Handles:
 * - Safe hardware microphone recording with MediaRecorder (API 31+ compatible)
 * - Real-time amplitude metering for dynamic waveform visualization
 * - Saving voice notes directly into internal app storage
 * - Extracting duration and audio metadata via MediaMetadataRetriever
 * - Managing, listing, deleting, and cleaning up local audio files
 * - Checking runtime RECORD_AUDIO permissions
 */
class AudioRecorderManager(private val context: Context) {

    private val audioDir: File by lazy {
        File(context.filesDir, "audio_recordings").apply {
            if (!exists()) mkdirs()
        }
    }

    private val tempDir: File by lazy {
        File(context.cacheDir, "temp_audio_recordings").apply {
            if (!exists()) mkdirs()
        }
    }

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var recordingStartTimeMs: Long = 0L
    private var pausedDurationAccumulatedMs: Long = 0L
    private var pauseStartTimeMs: Long = 0L
    private var isPausedInternal: Boolean = false

    private val waveformBuffer = mutableListOf<Float>()
    private var amplitudeJob: Job? = null
    private val recordingScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _recordingState = MutableStateFlow<AudioRecordingState>(AudioRecordingState.Idle)
    val recordingState: StateFlow<AudioRecordingState> = _recordingState.asStateFlow()

    /**
     * Checks if the app currently has runtime permission to record audio.
     */
    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Starts recording audio from the microphone into an AAC / M4A file.
     * @param customFileName Optional custom filename (without extension).
     * @param highQuality If true, uses 128 kbps bitrate, otherwise 64 kbps.
     */
    @Synchronized
    fun startRecording(customFileName: String? = null, highQuality: Boolean = true): Result<File> {
        if (!hasRecordPermission()) {
            val error = SecurityException("Permissão de gravação de áudio (RECORD_AUDIO) não concedida.")
            _recordingState.value = AudioRecordingState.Error(error.message ?: "Permissão negada", error)
            return Result.failure(error)
        }

        // Clean any active recording
        stopInternal(deleteFile = true)

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val name = if (!customFileName.isNullOrBlank()) {
            "${customFileName.replace("[^a-zA-Z0-9_-]".toRegex(), "_")}_$timestamp.m4a"
        } else {
            "rec_$timestamp.m4a"
        }

        val outputFile = File(tempDir, name)
        currentOutputFile = outputFile

        try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(if (highQuality) 128000 else 64000)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            recordingStartTimeMs = System.currentTimeMillis()
            pausedDurationAccumulatedMs = 0L
            pauseStartTimeMs = 0L
            isPausedInternal = false
            waveformBuffer.clear()

            _recordingState.value = AudioRecordingState.Recording(
                file = outputFile,
                elapsedDurationMs = 0L,
                currentAmplitude = 0f,
                waveformHistory = emptyList(),
                isPaused = false
            )

            startAmplitudeTicker()
            return Result.success(outputFile)

        } catch (e: Exception) {
            e.printStackTrace()
            outputFile.delete()
            mediaRecorder?.release()
            mediaRecorder = null
            _recordingState.value = AudioRecordingState.Error("Falha ao iniciar gravação: ${e.message}", e)
            return Result.failure(e)
        }
    }

    /**
     * Pauses the active recording session (Android 7.0+).
     */
    @Synchronized
    fun pauseRecording(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !isPausedInternal) {
            try {
                mediaRecorder?.pause()
                isPausedInternal = true
                pauseStartTimeMs = System.currentTimeMillis()
                
                val current = _recordingState.value
                if (current is AudioRecordingState.Recording) {
                    _recordingState.value = current.copy(isPaused = true)
                }
                return true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return false
    }

    /**
     * Resumes the paused recording session.
     */
    @Synchronized
    fun resumeRecording(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isPausedInternal) {
            try {
                mediaRecorder?.resume()
                if (pauseStartTimeMs > 0L) {
                    pausedDurationAccumulatedMs += (System.currentTimeMillis() - pauseStartTimeMs)
                    pauseStartTimeMs = 0L
                }
                isPausedInternal = false
                
                val current = _recordingState.value
                if (current is AudioRecordingState.Recording) {
                    _recordingState.value = current.copy(isPaused = false)
                }
                return true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return false
    }

    /**
     * Stops the recording session, saves the audio file permanently in the audio directory,
     * extracts audio duration and generated waveform data.
     */
    @Synchronized
    fun stopRecording(): LocalAudioFile? {
        amplitudeJob?.cancel()
        val file = currentOutputFile
        val recorder = mediaRecorder

        if (recorder == null || file == null || !file.exists()) {
            stopInternal(deleteFile = true)
            _recordingState.value = AudioRecordingState.Idle
            return null
        }

        return try {
            try {
                recorder.stop()
            } catch (e: RuntimeException) {
                // If stopped immediately without audio frames
                file.delete()
                recorder.release()
                mediaRecorder = null
                _recordingState.value = AudioRecordingState.Idle
                return null
            }
            recorder.release()
            mediaRecorder = null

            // Move from tempDir to permanent audioDir
            val finalFile = File(audioDir, file.name)
            file.copyTo(finalFile, overwrite = true)
            file.delete()

            // Calculate duration and metadata
            val durationMs = extractDurationMs(finalFile)
            val waveformString = compressWaveform(waveformBuffer)

            val localAudio = LocalAudioFile(
                id = UUID.randomUUID().toString(),
                name = finalFile.nameWithoutExtension,
                file = finalFile,
                uri = getFileUri(finalFile),
                sizeBytes = finalFile.length(),
                durationMs = durationMs,
                createdAt = finalFile.lastModified(),
                mimeType = "audio/m4a",
                waveformData = waveformString
            )

            _recordingState.value = AudioRecordingState.Completed(
                file = finalFile,
                durationMs = durationMs,
                sizeBytes = finalFile.length(),
                mimeType = "audio/m4a",
                waveformData = waveformString
            )

            localAudio
        } catch (e: Exception) {
            e.printStackTrace()
            _recordingState.value = AudioRecordingState.Error("Erro ao finalizar gravação: ${e.message}", e)
            null
        }
    }

    /**
     * Cancels the active recording and deletes the temporary file.
     */
    @Synchronized
    fun cancelRecording() {
        stopInternal(deleteFile = true)
        _recordingState.value = AudioRecordingState.Idle
    }

    /**
     * Resets recorder state to Idle.
     */
    fun resetState() {
        _recordingState.value = AudioRecordingState.Idle
    }

    private fun stopInternal(deleteFile: Boolean) {
        amplitudeJob?.cancel()
        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (_: Exception) {}
                release()
            }
        } catch (_: Exception) {}
        mediaRecorder = null

        if (deleteFile) {
            currentOutputFile?.let {
                if (it.exists()) it.delete()
            }
        }
        currentOutputFile = null
        isPausedInternal = false
    }

    private fun startAmplitudeTicker() {
        amplitudeJob?.cancel()
        amplitudeJob = recordingScope.launch {
            while (isActive) {
                delay(80)
                if (isPausedInternal) continue

                val recorder = mediaRecorder ?: break
                val rawAmp = try {
                    recorder.maxAmplitude
                } catch (_: Exception) {
                    0
                }

                // Max amplitude is 32767 for 16-bit PCM
                val normalizedAmp = (rawAmp.toFloat() / 32767f).coerceIn(0.05f, 1f)
                waveformBuffer.add(normalizedAmp)
                if (waveformBuffer.size > 80) {
                    waveformBuffer.removeAt(0)
                }

                val now = System.currentTimeMillis()
                val elapsed = (now - recordingStartTimeMs - pausedDurationAccumulatedMs).coerceAtLeast(0L)

                val file = currentOutputFile ?: break
                _recordingState.value = AudioRecordingState.Recording(
                    file = file,
                    elapsedDurationMs = elapsed,
                    currentAmplitude = normalizedAmp,
                    waveformHistory = waveformBuffer.toList(),
                    isPaused = isPausedInternal
                )
            }
        }
    }

    // ==========================================
    // Local Audio File Management
    // ==========================================

    /**
     * Lists all recorded audio files in the local audio vault.
     */
    fun listLocalAudioFiles(): List<LocalAudioFile> {
        val files = audioDir.listFiles { file ->
            file.isFile && (file.extension.equals("m4a", ignoreCase = true) ||
                            file.extension.equals("aac", ignoreCase = true) ||
                            file.extension.equals("ogg", ignoreCase = true) ||
                            file.extension.equals("mp3", ignoreCase = true))
        } ?: return emptyList()

        return files
            .sortedByDescending { it.lastModified() }
            .map { file ->
                val duration = extractDurationMs(file)
                LocalAudioFile(
                    id = file.name,
                    name = file.nameWithoutExtension,
                    file = file,
                    uri = getFileUri(file),
                    sizeBytes = file.length(),
                    durationMs = duration,
                    createdAt = file.lastModified(),
                    mimeType = "audio/${file.extension.lowercase()}",
                    waveformData = "0.3,0.7,0.4,0.9,0.5,0.8,0.6"
                )
            }
    }

    /**
     * Deletes a local audio file.
     */
    fun deleteAudioFile(file: File): Boolean {
        return try {
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Renames an existing local audio file.
     */
    fun renameAudioFile(file: File, newDisplayName: String): File? {
        if (!file.exists() || newDisplayName.isBlank()) return null
        val safeName = newDisplayName.replace("[^a-zA-Z0-9 _-]".toRegex(), "").trim()
        val ext = file.extension
        val newFile = File(file.parentFile, "$safeName.$ext")
        return if (file.renameTo(newFile)) newFile else null
    }

    /**
     * Calculates total bytes consumed by recorded audio files.
     */
    fun getTotalStorageSizeBytes(): Long {
        val audioSize = audioDir.listFiles()?.sumOf { it.length() } ?: 0L
        val tempSize = tempDir.listFiles()?.sumOf { it.length() } ?: 0L
        return audioSize + tempSize
    }

    /**
     * Cleans up orphaned or temporary recording cache files.
     */
    fun clearTemporaryCache(): Int {
        var count = 0
        tempDir.listFiles()?.forEach { file ->
            if (file != currentOutputFile && file.delete()) {
                count++
            }
        }
        return count
    }

    /**
     * Extracts playback duration in milliseconds using MediaMetadataRetriever.
     */
    fun extractDurationMs(file: File): Long {
        if (!file.exists()) return 0L
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            time?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun compressWaveform(samples: List<Float>): String {
        if (samples.isEmpty()) return "0.2,0.5,0.3,0.8,0.4,0.7"
        // Pick 20-30 representative points
        val step = (samples.size / 24).coerceAtLeast(1)
        val downsampled = samples.filterIndexed { index, _ -> index % step == 0 }.take(24)
        return downsampled.joinToString(",") { String.format(Locale.US, "%.2f", it) }
    }

    private fun getFileUri(file: File): Uri {
        return try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (_: Exception) {
            Uri.fromFile(file)
        }
    }
}
