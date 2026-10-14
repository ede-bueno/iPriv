package com.example.priv.data.supabase

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File

/**
 * Classe utilitária e gerenciadora singleton do SupabaseClient utilizando o SDK oficial
 * para Kotlin (io.github.jan.supabase) configurando os serviços de Autenticação, Banco de Dados (PostgREST),
 * Armazenamento (Storage) e Realtime com URLs e chaves definidas via BuildConfig (.env).
 */
class SupabaseClientProvider private constructor(context: Context) {

    private val applicationContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // URLs e chaves injetadas a partir do .env / Secrets Gradle Plugin
    val supabaseUrl: String = BuildConfig.SUPABASE_URL.let {
        if (it.isNotBlank() && (it.startsWith("http://") || it.startsWith("https://"))) it
        else "https://priv-memories-app.supabase.co"
    }

    val supabaseKey: String = BuildConfig.SUPABASE_ANON_KEY.let {
        if (it.isNotBlank() && it != "DEFAULT_SECRET_VALUE") it
        else "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InByaXYtbWVtb3JpZXMiLCJyb2xlIjoiYW5vbiIsImlhdCI6MTcwMDAwMDAwMCwiZXhwIjoyMDAwMDAwMDAwfQ.dummy_key"
    }

    /**
     * Verifica se as credenciais configuradas são reais ou padrão de desenvolvimento.
     */
    val isConfigured: Boolean
        get() = supabaseUrl.isNotBlank() && 
                !supabaseUrl.contains("YOUR_SUPABASE") && 
                supabaseKey.isNotBlank() && 
                supabaseKey != "DEFAULT_SECRET_VALUE"

    /**
     * Instância oficial do cliente Supabase configurada com os módulos essenciais:
     * - Auth: Gerenciamento de sessões, login com Email/Senha e Google ID Token
     * - Postgrest: Acesso ao banco de dados relacional PostgreSQL
     * - Storage: Upload e download de áudios e arquivos multimídia
     * - Realtime: Escuta de alterações em tempo real via WebSockets
     */
    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = supabaseUrl,
        supabaseKey = supabaseKey
    ) {
        install(Auth) {
            sessionManager = io.github.jan.supabase.auth.MemorySessionManager()
            codeVerifierCache = io.github.jan.supabase.auth.MemoryCodeVerifierCache()
            alwaysAutoRefresh = true
            autoLoadFromStorage = false
        }
        install(Postgrest)
        install(Storage)
        install(Realtime)
    }

    // Acessos diretos tipados aos serviços oficiais
    val auth: Auth get() = client.auth
    val postgrest: Postgrest get() = client.postgrest
    val storage: Storage get() = client.storage
    val realtime: Realtime get() = client.realtime

    // Estado reativo da sessão do usuário
    private val _currentUser = MutableStateFlow<UserInfo?>(null)
    val currentUser: StateFlow<UserInfo?> = _currentUser.asStateFlow()

    private val _sessionStatus = MutableStateFlow<SessionStatus>(SessionStatus.NotAuthenticated(false))
    val sessionStatus: StateFlow<SessionStatus> = _sessionStatus.asStateFlow()

    init {
        // Monitora as alterações de autenticação
        scope.launch {
            try {
                auth.sessionStatus.collect { status ->
                    _sessionStatus.value = status
                    _currentUser.value = auth.currentUserOrNull()
                    Log.d(TAG, "Status da Sessão Supabase: $status")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Falha ao observar status da sessão Supabase", e)
            }
        }
    }

    val currentUserId: String?
        get() = auth.currentUserOrNull()?.id

    val isUserLoggedIn: Boolean
        get() = auth.currentUserOrNull() != null

    // =========================================================================
    // SERVIÇOS DE AUTENTICAÇÃO
    // =========================================================================

    /**
     * Cadastra um novo usuário com e-mail e senha.
     */
    suspend fun signUpWithEmail(emailInput: String, passwordInput: String): Result<UserInfo?> = withContext(Dispatchers.IO) {
        try {
            auth.signUpWith(Email) {
                email = emailInput.trim()
                password = passwordInput
            }
            val user = auth.currentUserOrNull()
            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Erro no signUp com email", e)
            Result.failure(e)
        }
    }

    /**
     * Realiza login de usuário existente com e-mail e senha.
     */
    suspend fun signInWithEmail(emailInput: String, passwordInput: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            auth.signInWith(Email) {
                email = emailInput.trim()
                password = passwordInput
            }
            _currentUser.value = auth.currentUserOrNull()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro no signIn com email", e)
            Result.failure(e)
        }
    }

    /**
     * Realiza login utilizando um Google ID Token obtido através do Credential Manager.
     */
    suspend fun signInWithGoogleIdToken(googleIdToken: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            auth.signInWith(IDToken) {
                provider = Google
                idToken = googleIdToken
            }
            _currentUser.value = auth.currentUserOrNull()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro no signIn com Google ID Token", e)
            Result.failure(e)
        }
    }

    /**
     * Encerra a sessão do usuário.
     */
    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            auth.signOut()
            _currentUser.value = null
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao encerrar sessão Supabase", e)
            Result.failure(e)
        }
    }

    // =========================================================================
    // SERVIÇOS DE BANCO DE DADOS (POSTGREST)
    // =========================================================================

    /**
     * Retorna referência a uma tabela PostgREST para consultas dinâmicas.
     */
    fun fromTable(tableName: String) = client.postgrest.from(tableName)

    /**
     * Consulta as memórias de áudio remotas do usuário autenticado.
     */
    suspend fun fetchRemoteAudioMemories(): Result<List<RemoteAudioMemoryDto>> = withContext(Dispatchers.IO) {
        try {
            val response = client.postgrest.from("audio_memories")
                .select()
                .decodeList<RemoteAudioMemoryDto>()
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar audio_memories remotas", e)
            Result.failure(e)
        }
    }

    /**
     * Insere ou atualiza (UPSERT) uma memória de áudio no Supabase PostgreSQL.
     */
    suspend fun upsertRemoteAudioMemory(audioDto: RemoteAudioMemoryDto): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.postgrest.from("audio_memories").upsert(audioDto)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao fazer upsert de audio_memory", e)
            Result.failure(e)
        }
    }

    /**
     * Remove uma memória de áudio remota pelo ID.
     */
    suspend fun deleteRemoteAudioMemory(audioId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.postgrest.from("audio_memories").delete {
                filter {
                    eq("id", audioId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao deletar audio_memory remota", e)
            Result.failure(e)
        }
    }

    // =========================================================================
    // SERVIÇOS DE ARMAZENAMENTO (SUPABASE STORAGE)
    // =========================================================================

    /**
     * Faz upload de arquivo de áudio binário para o bucket 'audio-vault'.
     */
    suspend fun uploadAudioFile(
        file: File,
        remotePath: String,
        mimeType: String = "audio/m4a"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val bucket = client.storage.from("audio-vault")
            val bytes = file.readBytes()
            bucket.upload(remotePath, bytes) {
                upsert = true
            }
            val publicUrl = bucket.publicUrl(remotePath)
            Result.success(publicUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao fazer upload de áudio para Supabase Storage", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "SupabaseClientProvider"

        @Volatile
        private var INSTANCE: SupabaseClientProvider? = null

        fun getInstance(context: Context): SupabaseClientProvider {
            return INSTANCE ?: synchronized(this) {
                val instance = SupabaseClientProvider(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

// =========================================================================
// DATA TRANSFER OBJECTS (DTOs SERIALIZÁVEIS)
// =========================================================================

@Serializable
data class RemoteAudioMemoryDto(
    @SerialName("id") val id: String,
    @SerialName("owner_user_id") val ownerUserId: String? = null,
    @SerialName("title") val title: String,
    @SerialName("file_name") val fileName: String,
    @SerialName("remote_storage_path") val remoteStoragePath: String? = null,
    @SerialName("mime_type") val mimeType: String = "audio/m4a",
    @SerialName("file_size_bytes") val fileSizeBytes: Long = 0L,
    @SerialName("duration_ms") val durationMs: Long = 0L,
    @SerialName("sample_rate") val sampleRate: Int = 44100,
    @SerialName("channels") val channels: Int = 1,
    @SerialName("bitrate") val bitrate: Int = 128000,
    @SerialName("waveform_data") val waveformData: String = "",
    @SerialName("audio_checksum") val audioChecksum: String = "",
    @SerialName("transcription_text") val transcriptionText: String = "",
    @SerialName("transcription_status") val transcriptionStatus: String = "COMPLETED",
    @SerialName("transcription_language") val transcriptionLanguage: String = "pt-BR",
    @SerialName("transcription_confidence") val transcriptionConfidence: Float = 1.0f,
    @SerialName("transcription_engine") val transcriptionEngine: String = "LOCAL_AI",
    @SerialName("summary") val summary: String? = null,
    @SerialName("tags") val tags: String? = null,
    @SerialName("speaker_name") val speakerName: String = "Você",
    @SerialName("is_favorite") val isFavorite: Boolean = false,
    @SerialName("source") val source: String = "MICROPHONE",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)
