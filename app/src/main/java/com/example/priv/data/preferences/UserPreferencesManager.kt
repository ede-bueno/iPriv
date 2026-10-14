package com.example.priv.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val title: String) {
    SYSTEM("Padrão do Sistema"),
    LIGHT("Modo Claro"),
    DARK("Modo Escuro")
}

enum class AudioRecordingQuality(val title: String, val bitRateKbps: Int) {
    HIGH_AAC_128K("Alta Fidelidade (128 kbps AAC)", 128),
    STANDARD_AAC_64K("Compacto / Econômico (64 kbps AAC)", 64)
}

enum class AutoLockTimeout(val title: String, val minutes: Int) {
    IMMEDIATE("Imediatamente ao sair", 0),
    AFTER_1_MIN("Após 1 minuto em segundo plano", 1),
    AFTER_5_MIN("Após 5 minutos", 5),
    NEVER("Somente ao fechar ou bloquear manualmente", -1)
}

enum class MemoryDisplayLayout(val title: String) {
    CARDS("Cartões Detalhados"),
    COMPACT_GRID("Grade Visual Compacta"),
    TIMELINE("Linha do Tempo Contínua")
}

data class UserPreferences(
    // Perfil do Usuário
    val userName: String = "Você",
    val userBio: String = "Guardando momentos com afeto e privacidade",
    val userAvatarUrl: String = "",
    val userEmail: String = "",

    // Aparência e Tema
    val themeMode: ThemeMode = ThemeMode.DARK,
    val useDynamicColors: Boolean = false,
    val amoledPureBlack: Boolean = false,
    val memoryDisplayLayout: MemoryDisplayLayout = MemoryDisplayLayout.CARDS,

    // Áudio e Gravação de Voz
    val audioQuality: AudioRecordingQuality = AudioRecordingQuality.HIGH_AAC_128K,
    val autoTranscribeWithAi: Boolean = true,
    val showWaveformPreview: Boolean = true,
    val defaultPlaybackSpeed: Float = 1.0f,
    val saveAudioCopiesLocally: Boolean = true,

    // Segurança e Privacidade
    val autoLockTimeout: AutoLockTimeout = AutoLockTimeout.NEVER,
    val maskContentInRecentApps: Boolean = false,
    val discreetNotifications: Boolean = false,
    val contactsIntegrationEnabled: Boolean = true,

    // Cápsula do Tempo e Alertas
    val capsuleUnlockNotifications: Boolean = true,
    val dailyMemoryReminder: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,

    // Armazenamento & Dados
    val autoCleanTempAudioCache: Boolean = true,
    val confirmBeforeDelete: Boolean = true
)

/**
 * Gerenciador reativo e persistente de preferências do usuário para o GuardDay.
 */
class UserPreferencesManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _preferences = MutableStateFlow(loadFromPrefs())
    val preferences: StateFlow<UserPreferences> = _preferences.asStateFlow()

    private fun loadFromPrefs(): UserPreferences {
        val themeModeStr = prefs.getString(KEY_THEME_MODE, ThemeMode.DARK.name) ?: ThemeMode.DARK.name
        val audioQualityStr = prefs.getString(KEY_AUDIO_QUALITY, AudioRecordingQuality.HIGH_AAC_128K.name) ?: AudioRecordingQuality.HIGH_AAC_128K.name
        val autoLockStr = prefs.getString(KEY_AUTO_LOCK_TIMEOUT, AutoLockTimeout.NEVER.name) ?: AutoLockTimeout.NEVER.name
        val layoutStr = prefs.getString(KEY_MEMORY_LAYOUT, MemoryDisplayLayout.CARDS.name) ?: MemoryDisplayLayout.CARDS.name

        return UserPreferences(
            userName = prefs.getString(KEY_USER_NAME, "Você") ?: "Você",
            userBio = prefs.getString(KEY_USER_BIO, "Guardando momentos com afeto e privacidade") ?: "Guardando momentos com afeto e privacidade",
            userAvatarUrl = prefs.getString(KEY_USER_AVATAR, "") ?: "",
            userEmail = prefs.getString(KEY_USER_EMAIL, "") ?: "",

            themeMode = try { ThemeMode.valueOf(themeModeStr) } catch (_: Exception) { ThemeMode.DARK },
            useDynamicColors = prefs.getBoolean(KEY_DYNAMIC_COLORS, false),
            amoledPureBlack = prefs.getBoolean(KEY_AMOLED_BLACK, false),
            memoryDisplayLayout = try { MemoryDisplayLayout.valueOf(layoutStr) } catch (_: Exception) { MemoryDisplayLayout.CARDS },

            audioQuality = try { AudioRecordingQuality.valueOf(audioQualityStr) } catch (_: Exception) { AudioRecordingQuality.HIGH_AAC_128K },
            autoTranscribeWithAi = prefs.getBoolean(KEY_AUTO_TRANSCRIBE, true),
            showWaveformPreview = prefs.getBoolean(KEY_SHOW_WAVEFORMS, true),
            defaultPlaybackSpeed = prefs.getFloat(KEY_PLAYBACK_SPEED, 1.0f),
            saveAudioCopiesLocally = prefs.getBoolean(KEY_SAVE_AUDIO_COPIES, true),

            autoLockTimeout = try { AutoLockTimeout.valueOf(autoLockStr) } catch (_: Exception) { AutoLockTimeout.NEVER },
            maskContentInRecentApps = prefs.getBoolean(KEY_MASK_RECENT_APPS, false),
            discreetNotifications = prefs.getBoolean(KEY_DISCREET_NOTIFICATIONS, false),
            contactsIntegrationEnabled = prefs.getBoolean(KEY_CONTACTS_ENABLED, true),

            capsuleUnlockNotifications = prefs.getBoolean(KEY_CAPSULE_NOTIFICATIONS, true),
            dailyMemoryReminder = prefs.getBoolean(KEY_DAILY_REMINDER, false),
            reminderHour = prefs.getInt(KEY_REMINDER_HOUR, 20),
            reminderMinute = prefs.getInt(KEY_REMINDER_MINUTE, 0),

            autoCleanTempAudioCache = prefs.getBoolean(KEY_AUTO_CLEAN_CACHE, true),
            confirmBeforeDelete = prefs.getBoolean(KEY_CONFIRM_DELETE, true)
        )
    }

    fun updateProfile(name: String, bio: String, avatarUrl: String, email: String? = null) {
        prefs.edit().apply {
            putString(KEY_USER_NAME, name.trim())
            putString(KEY_USER_BIO, bio.trim())
            putString(KEY_USER_AVATAR, avatarUrl)
            if (email != null) putString(KEY_USER_EMAIL, email.trim())
            apply()
        }
        _preferences.value = _preferences.value.copy(
            userName = name.trim(),
            userBio = bio.trim(),
            userAvatarUrl = avatarUrl,
            userEmail = email?.trim() ?: _preferences.value.userEmail
        )
    }

    fun updateTheme(themeMode: ThemeMode, dynamicColors: Boolean, amoledPureBlack: Boolean) {
        prefs.edit().apply {
            putString(KEY_THEME_MODE, themeMode.name)
            putBoolean(KEY_DYNAMIC_COLORS, dynamicColors)
            putBoolean(KEY_AMOLED_BLACK, amoledPureBlack)
            apply()
        }
        _preferences.value = _preferences.value.copy(
            themeMode = themeMode,
            useDynamicColors = dynamicColors,
            amoledPureBlack = amoledPureBlack
        )
    }

    fun updateAudioSettings(
        quality: AudioRecordingQuality,
        autoTranscribe: Boolean,
        showWaveform: Boolean,
        defaultSpeed: Float,
        saveLocalCopy: Boolean
    ) {
        prefs.edit().apply {
            putString(KEY_AUDIO_QUALITY, quality.name)
            putBoolean(KEY_AUTO_TRANSCRIBE, autoTranscribe)
            putBoolean(KEY_SHOW_WAVEFORMS, showWaveform)
            putFloat(KEY_PLAYBACK_SPEED, defaultSpeed)
            putBoolean(KEY_SAVE_AUDIO_COPIES, saveLocalCopy)
            apply()
        }
        _preferences.value = _preferences.value.copy(
            audioQuality = quality,
            autoTranscribeWithAi = autoTranscribe,
            showWaveformPreview = showWaveform,
            defaultPlaybackSpeed = defaultSpeed,
            saveAudioCopiesLocally = saveLocalCopy
        )
    }

    fun updateSecuritySettings(
        autoLock: AutoLockTimeout,
        maskRecentApps: Boolean,
        discreetNotifications: Boolean,
        contactsEnabled: Boolean
    ) {
        prefs.edit().apply {
            putString(KEY_AUTO_LOCK_TIMEOUT, autoLock.name)
            putBoolean(KEY_MASK_RECENT_APPS, maskRecentApps)
            putBoolean(KEY_DISCREET_NOTIFICATIONS, discreetNotifications)
            putBoolean(KEY_CONTACTS_ENABLED, contactsEnabled)
            apply()
        }
        _preferences.value = _preferences.value.copy(
            autoLockTimeout = autoLock,
            maskContentInRecentApps = maskRecentApps,
            discreetNotifications = discreetNotifications,
            contactsIntegrationEnabled = contactsEnabled
        )
    }

    fun updateNotificationSettings(capsuleAlerts: Boolean, dailyReminder: Boolean, hour: Int, minute: Int) {
        prefs.edit().apply {
            putBoolean(KEY_CAPSULE_NOTIFICATIONS, capsuleAlerts)
            putBoolean(KEY_DAILY_REMINDER, dailyReminder)
            putInt(KEY_REMINDER_HOUR, hour)
            putInt(KEY_REMINDER_MINUTE, minute)
            apply()
        }
        _preferences.value = _preferences.value.copy(
            capsuleUnlockNotifications = capsuleAlerts,
            dailyMemoryReminder = dailyReminder,
            reminderHour = hour,
            reminderMinute = minute
        )
    }

    fun updateStorageSettings(autoCleanCache: Boolean, confirmDelete: Boolean) {
        prefs.edit().apply {
            putBoolean(KEY_AUTO_CLEAN_CACHE, autoCleanCache)
            putBoolean(KEY_CONFIRM_DELETE, confirmDelete)
            apply()
        }
        _preferences.value = _preferences.value.copy(
            autoCleanTempAudioCache = autoCleanCache,
            confirmBeforeDelete = confirmDelete
        )
    }

    fun updateDisplayLayout(layout: MemoryDisplayLayout) {
        prefs.edit().putString(KEY_MEMORY_LAYOUT, layout.name).apply()
        _preferences.value = _preferences.value.copy(memoryDisplayLayout = layout)
    }

    fun resetToDefaults() {
        prefs.edit().clear().apply()
        _preferences.value = UserPreferences()
    }

    companion object {
        private const val PREFS_NAME = "guardday_user_prefs"

        private const val KEY_USER_NAME = "pref_user_name"
        private const val KEY_USER_BIO = "pref_user_bio"
        private const val KEY_USER_AVATAR = "pref_user_avatar"
        private const val KEY_USER_EMAIL = "pref_user_email"

        private const val KEY_THEME_MODE = "pref_theme_mode"
        private const val KEY_DYNAMIC_COLORS = "pref_dynamic_colors"
        private const val KEY_AMOLED_BLACK = "pref_amoled_black"
        private const val KEY_MEMORY_LAYOUT = "pref_memory_layout"

        private const val KEY_AUDIO_QUALITY = "pref_audio_quality"
        private const val KEY_AUTO_TRANSCRIBE = "pref_auto_transcribe"
        private const val KEY_SHOW_WAVEFORMS = "pref_show_waveforms"
        private const val KEY_PLAYBACK_SPEED = "pref_playback_speed"
        private const val KEY_SAVE_AUDIO_COPIES = "pref_save_audio_copies"

        private const val KEY_AUTO_LOCK_TIMEOUT = "pref_auto_lock_timeout"
        private const val KEY_MASK_RECENT_APPS = "pref_mask_recent_apps"
        private const val KEY_DISCREET_NOTIFICATIONS = "pref_discreet_notifications"
        private const val KEY_CONTACTS_ENABLED = "pref_contacts_enabled"

        private const val KEY_CAPSULE_NOTIFICATIONS = "pref_capsule_notifications"
        private const val KEY_DAILY_REMINDER = "pref_daily_reminder"
        private const val KEY_REMINDER_HOUR = "pref_reminder_hour"
        private const val KEY_REMINDER_MINUTE = "pref_reminder_minute"

        private const val KEY_AUTO_CLEAN_CACHE = "pref_auto_clean_cache"
        private const val KEY_CONFIRM_DELETE = "pref_confirm_delete"

        @Volatile
        private var instance: UserPreferencesManager? = null

        fun getInstance(context: Context): UserPreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: UserPreferencesManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
