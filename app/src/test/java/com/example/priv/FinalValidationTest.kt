package com.example.priv

import com.example.priv.data.contacts.PhoneContactItem
import com.example.priv.data.contacts.ContactsHelper
import com.example.priv.data.entity.MemoryEntity
import com.example.priv.data.entity.PersonEntity
import com.example.priv.data.export.BackupManifest
import com.example.priv.data.export.RestoreResult
import com.example.priv.data.repository.UserSession
import org.junit.Assert.*
import org.junit.Test
import java.security.MessageDigest

class FinalValidationTest {

    @Test
    fun testBackupManifestChecksumCalculation() {
        val jsonSample = """{"app":"Priv","memories":[]}"""
        val digest = MessageDigest.getInstance("SHA-256")
        val sha256 = digest.digest(jsonSample.toByteArray()).fold("") { str, it -> str + "%02x".format(it) }

        val manifest = BackupManifest(
            formatVersion = "1.0.0-beta.3",
            appVersion = "1.0.0-beta.3",
            timestamp = System.currentTimeMillis(),
            memoryCount = 10,
            personCount = 5,
            checksumSha256 = sha256
        )

        assertEquals("1.0.0-beta.3", manifest.formatVersion)
        assertEquals(10, manifest.memoryCount)
        assertEquals(5, manifest.personCount)
        assertEquals(64, manifest.checksumSha256.length)
    }

    @Test
    fun testRestoreResultModel() {
        val result = RestoreResult(
            success = true,
            restoredMemories = 15,
            restoredPersons = 3,
            restoredMoments = 2,
            restoredMediaCount = 8,
            message = "Restauração efetuada com sucesso!"
        )

        assertTrue(result.success)
        assertEquals(15, result.restoredMemories)
        assertEquals(3, result.restoredPersons)
        assertEquals(8, result.restoredMediaCount)
    }

    @Test
    fun testMemoryEntityDefaultsAndDraftSave() {
        val memory = MemoryEntity(
            title = "Áudio Engraçado WhatsApp",
            note = "Resenha do grupo da praia",
            source = "WhatsApp",
            primaryPersonId = "p_elo_123"
        )

        assertNotNull(memory.id)
        assertEquals("default_space", memory.spaceId)
        assertEquals("Áudio Engraçado WhatsApp", memory.title)
        assertEquals("Resenha do grupo da praia", memory.note)
        assertEquals("WhatsApp", memory.source)
        assertFalse(memory.inTrash)
        assertFalse(memory.isFavorite)
    }

    @Test
    fun testSelectiveContactConversion() {
        val contact = PhoneContactItem(
            id = "c_1",
            name = "Juliana Silva",
            phoneNumber = "+55 11 99999-8888",
            photoUri = "content://contacts/photos/1"
        )

        val person = PersonEntity(
            name = contact.name,
            relationship = "Contato importado",
            avatarUri = contact.photoUri ?: "",
            bioNote = "Importado seletivamente da agenda do aparelho."
        )

        assertEquals("Juliana Silva", person.name)
        assertEquals("Contato importado", person.relationship)
        assertEquals("content://contacts/photos/1", person.avatarUri)
        assertEquals("Importado seletivamente da agenda do aparelho.", person.bioNote)
    }

    @Test
    fun testUserSessionState() {
        val session = UserSession(
            userId = "usr_google_123",
            email = "usuario@priv.app",
            displayName = "Usuário Teste",
            photoUrl = "https://example.com/avatar.jpg",
            personalSpaceId = "space_personal_usr",
            authProvider = "Google",
            isAuthenticated = true
        )

        assertTrue(session.isAuthenticated)
        assertEquals("usr_google_123", session.userId)
        assertEquals("Google", session.authProvider)
        assertEquals("space_personal_usr", session.personalSpaceId)
    }

    @Test
    fun testAiSuggestionResultDataClass() {
        val aiResult = com.example.priv.data.ai.AiSuggestionResult(
            title = "Áudio sobre futebol com Elô",
            summaryOrTranscription = "Áudio discutindo o jogo do fim de semana.",
            tags = listOf("futebol", "WhatsApp", "Elô"),
            suggestedCategory = "Conversa",
            emotionTag = "Empolgado"
        )

        assertEquals("Áudio sobre futebol com Elô", aiResult.title)
        assertEquals("Áudio discutindo o jogo do fim de semana.", aiResult.summaryOrTranscription)
        assertEquals(3, aiResult.tags.size)
        assertEquals("futebol", aiResult.tags[0])
        assertEquals("Empolgado", aiResult.emotionTag)
    }
}
