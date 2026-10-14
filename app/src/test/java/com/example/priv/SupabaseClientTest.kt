package com.example.priv

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.priv.data.supabase.RemoteAudioMemoryDto
import com.example.priv.data.supabase.SupabaseClientProvider
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SupabaseClientTest {

    private lateinit var context: Context
    private lateinit var provider: SupabaseClientProvider

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        provider = SupabaseClientProvider.getInstance(context)
    }

    @Test
    fun testSupabaseClientInitializationAndPlugins() {
        assertNotNull(provider)
        assertNotNull(provider.client)
        assertNotNull(provider.auth)
        assertNotNull(provider.postgrest)
        assertNotNull(provider.storage)
        assertNotNull(provider.realtime)

        // URL e Key devem estar preenchidas com valor do BuildConfig ou fallback seguro
        assertTrue(provider.supabaseUrl.isNotBlank())
        assertTrue(provider.supabaseKey.isNotBlank())
    }

    @Test
    fun testRemoteAudioMemoryDtoSerialization() {
        val dto = RemoteAudioMemoryDto(
            id = "audio_remote_uuid_123",
            ownerUserId = "user_456",
            title = "Gravação de Voz na Nuvem",
            fileName = "audio_123.m4a",
            remoteStoragePath = "user_456/audio_123.m4a",
            mimeType = "audio/m4a",
            fileSizeBytes = 1024000L,
            durationMs = 60000L,
            transcriptionText = "Olá mundo, sincronizado via Supabase Kotlin SDK!",
            transcriptionStatus = "COMPLETED",
            speakerName = "Você",
            isFavorite = true
        )

        val jsonString = Json.encodeToString(dto)
        assertTrue(jsonString.contains("audio_remote_uuid_123"))
        assertTrue(jsonString.contains("Olá mundo, sincronizado via Supabase Kotlin SDK!"))

        val decoded = Json.decodeFromString<RemoteAudioMemoryDto>(jsonString)
        assertEquals(dto.id, decoded.id)
        assertEquals(dto.title, decoded.title)
        assertEquals(dto.transcriptionText, decoded.transcriptionText)
        assertEquals(dto.durationMs, decoded.durationMs)
        assertTrue(decoded.isFavorite)
    }
}
