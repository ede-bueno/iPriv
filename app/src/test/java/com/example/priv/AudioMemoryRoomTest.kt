package com.example.priv

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.priv.data.dao.AudioMemoryDao
import com.example.priv.data.database.PrivDatabase
import com.example.priv.data.entity.AudioMemoryEntity
import com.example.priv.data.repository.AudioMemoryRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AudioMemoryRoomTest {

    private lateinit var db: PrivDatabase
    private lateinit var audioMemoryDao: AudioMemoryDao
    private lateinit var repository: AudioMemoryRepository
    private lateinit var context: Context

    @Before
    fun createDb() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, PrivDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        audioMemoryDao = db.audioMemoryDao()
        repository = AudioMemoryRepository(audioMemoryDao, context)
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun testInsertAndRetrieveAudioMemoryWithTranscription() = runBlocking {
        val audio = AudioMemoryEntity(
            id = "test_audio_1",
            title = "Áudio de Teste da Família",
            fileName = "teste_audio_familia.m4a",
            filePath = "/data/user/0/com.example/files/local_audio_vault/teste.m4a",
            fileUri = "content://audio/test_1",
            fileSize = 512000L,
            durationMs = 45000L,
            sampleRate = 44100,
            channels = 1,
            bitrate = 128000,
            waveformData = "10,20,50,80,60,30",
            transcriptionText = "Oi família, chegamos bem de viagem e estamos almoçando agora!",
            transcriptionStatus = "COMPLETED",
            transcriptionLanguage = "pt-BR",
            speakerName = "Maria",
            isFavorite = true
        )

        audioMemoryDao.insertAudioMemory(audio)

        val retrievedList = audioMemoryDao.getAllAudioMemories().first()
        assertEquals(1, retrievedList.size)

        val retrieved = retrievedList.first()
        assertEquals("test_audio_1", retrieved.id)
        assertEquals("Áudio de Teste da Família", retrieved.title)
        assertEquals("Oi família, chegamos bem de viagem e estamos almoçando agora!", retrieved.transcriptionText)
        assertEquals("Maria", retrieved.speakerName)
        assertTrue(retrieved.isFavorite)
        assertEquals(45000L, retrieved.durationMs)
        assertEquals("00:45", retrieved.formattedDuration)
    }

    @Test
    fun testUpdateTranscriptionTextDirectly() = runBlocking {
        val audio = AudioMemoryEntity(
            id = "test_audio_edit",
            title = "Gravação de Reunião",
            fileName = "reuniao.m4a",
            filePath = "/path/reuniao.m4a",
            fileUri = "content://audio/reuniao",
            transcriptionText = "Texto inicial não revisado",
            transcriptionStatus = "PENDING"
        )
        audioMemoryDao.insertAudioMemory(audio)

        // Atualiza a transcrição
        repository.updateTranscription("test_audio_edit", "Texto corrigido e formatado pelo usuário")

        val updated = audioMemoryDao.getAudioMemoryById("test_audio_edit").first()
        assertNotNull(updated)
        assertEquals("Texto corrigido e formatado pelo usuário", updated?.transcriptionText)
        assertEquals("EDITED", updated?.transcriptionStatus)
    }

    @Test
    fun testSearchAudioByTranscriptionKeywords() = runBlocking {
        val audio1 = AudioMemoryEntity(
            id = "audio_churrasco",
            title = "Conversa de Sábado",
            fileName = "sabado.m4a",
            filePath = "/path/sabado.m4a",
            fileUri = "content://audio/sabado",
            transcriptionText = "O churrasco no sítio ficou maravilhoso com todo mundo junto"
        )
        val audio2 = AudioMemoryEntity(
            id = "audio_trabalho",
            title = "Ideia de Software",
            fileName = "software.m4a",
            filePath = "/path/software.m4a",
            fileUri = "content://audio/software",
            transcriptionText = "Precisamos refatorar o banco de dados Room para usar KSP e Flows"
        )
        audioMemoryDao.insertAudioMemories(listOf(audio1, audio2))

        val searchResult1 = repository.searchAudioMemories("churrasco").first()
        assertEquals(1, searchResult1.size)
        assertEquals("audio_churrasco", searchResult1.first().id)

        val searchResult2 = repository.searchAudioMemories("refatorar").first()
        assertEquals(1, searchResult2.size)
        assertEquals("audio_trabalho", searchResult2.first().id)
    }

    @Test
    fun testToggleFavoriteAndDeleteAudioMemory() = runBlocking {
        val audio = AudioMemoryEntity(
            id = "audio_fav_test",
            title = "Música do Pôr do Sol",
            fileName = "musica.m4a",
            filePath = "/path/musica.m4a",
            fileUri = "content://audio/musica",
            isFavorite = false
        )
        audioMemoryDao.insertAudioMemory(audio)

        repository.toggleFavorite("audio_fav_test", true)
        val favAudio = audioMemoryDao.getAudioMemoryById("audio_fav_test").first()
        assertTrue(favAudio?.isFavorite == true)

        repository.deleteAudio("audio_fav_test", deletePhysicalFile = false)
        val deletedAudio = audioMemoryDao.getAudioMemoryById("audio_fav_test").first()
        assertNull(deletedAudio)
    }
}
