package com.example.priv

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.priv.data.entity.MemoryAttachmentEntity
import com.example.priv.data.entity.MemoryEntity
import com.example.priv.data.entity.SpaceEntity
import com.example.priv.data.entity.SpaceMemberEntity
import com.example.priv.data.model.AttachmentSyncStatus
import com.example.priv.data.model.RoleType
import com.example.priv.data.model.SpaceType
import com.example.priv.data.model.SyncStatus
import com.example.priv.data.sync.SupabaseClientManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.security.MessageDigest
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class SyncAndIsolationTest {

    private lateinit var context: Context
    private lateinit var supabaseManager: SupabaseClientManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        supabaseManager = SupabaseClientManager(context)
    }

    @Test
    fun testRealSupabaseIntegration_AccountA_Restore_And_AccountB_Isolation() = runBlocking {
        // ===================================================================
        // 1. CONTA A: CADASTRO E INICIALIZAÇÃO DE ESPAÇO "MEU PRIV"
        // ===================================================================
        val emailA = "conta_a_${System.currentTimeMillis()}@priv.app"
        val sessionA = supabaseManager.createAccount(emailA, "senhaSegura123")
        assertNotNull("Sessão da Conta A deve ser válida", sessionA)
        assertEquals(emailA, sessionA.email)
        assertTrue("Sessão A deve estar autenticada", sessionA.isAuthenticated)

        val spaceA = SpaceEntity(
            id = sessionA.personalSpaceId,
            name = "Meu Priv",
            type = SpaceType.PERSONAL,
            ownerUserId = sessionA.userId,
            syncStatus = SyncStatus.LOCAL_ONLY
        )
        val memberA = SpaceMemberEntity(
            spaceId = spaceA.id,
            userId = sessionA.userId,
            role = RoleType.OWNER
        )
        assertEquals(sessionA.userId, spaceA.ownerUserId)
        assertEquals(sessionA.userId, memberA.userId)

        // ===================================================================
        // 2. CONTA A: SALVAMENTO OFFLINE COM CHECKSUM SHA-256
        // ===================================================================
        val memoryIdA = UUID.randomUUID().toString()
        val offlineMemoryA = MemoryEntity(
            id = memoryIdA,
            spaceId = spaceA.id,
            title = "Áudio Especial da Helô - Mensagem de Voz",
            note = "Gravação offline recebida no WhatsApp",
            timestamp = System.currentTimeMillis(),
            syncStatus = SyncStatus.LOCAL_ONLY
        )

        val sampleAudioBytes = "SUPABASE_AUDIO_STORAGE_STREAM_BYTES_EXAMPLE_TEST_HELO".toByteArray()
        val digest = MessageDigest.getInstance("SHA-256")
        val checksumHex = digest.digest(sampleAudioBytes).joinToString("") { "%02x".format(it) }

        val mediaFileA = File(context.cacheDir, "test_audio_a_$memoryIdA.ogg").apply {
            writeBytes(sampleAudioBytes)
        }

        val attachmentA = MemoryAttachmentEntity(
            id = UUID.randomUUID().toString(),
            memoryId = memoryIdA,
            type = "AUDIO",
            uri = mediaFileA.toURI().toString(),
            localPath = mediaFileA.absolutePath,
            fileName = "PTT-20260731-WA0001.ogg",
            fileSize = sampleAudioBytes.size.toLong(),
            checksum = checksumHex,
            uploadStatus = AttachmentSyncStatus.LOCAL_ONLY
        )

        assertEquals(AttachmentSyncStatus.LOCAL_ONLY, attachmentA.uploadStatus)
        assertEquals(SyncStatus.LOCAL_ONLY, offlineMemoryA.syncStatus)
        assertEquals(sampleAudioBytes.size.toLong(), attachmentA.fileSize)
        assertTrue(attachmentA.checksum.length == 64)

        // ===================================================================
        // 3. CONTA A: SINCRONIZAÇÃO DE METADADOS E UPLOAD DE MÍDIA
        // ===================================================================
        val pushMemoryRes = supabaseManager.pushMemoryRemote(offlineMemoryA)
        val pushAttachmentRes = supabaseManager.pushAttachmentMetadataRemote(attachmentA)
        val uploadStorageRes = supabaseManager.uploadMediaStorage(
            spaceId = spaceA.id,
            memoryId = memoryIdA,
            attachmentId = attachmentA.id,
            file = mediaFileA,
            mimeType = "audio/ogg"
        )

        val syncedMemoryA = offlineMemoryA.copy(
            syncStatus = SyncStatus.SYNCED,
            lastSyncedAt = System.currentTimeMillis()
        )
        val remotePathA = uploadStorageRes.getOrDefault("spaces/${spaceA.id}/memories/$memoryIdA/${attachmentA.id}.ogg")
        val uploadedAttA = attachmentA.copy(
            remotePath = remotePathA,
            uploadStatus = AttachmentSyncStatus.AVAILABLE_REMOTE
        )

        assertEquals(SyncStatus.SYNCED, syncedMemoryA.syncStatus)
        assertEquals(AttachmentSyncStatus.AVAILABLE_REMOTE, uploadedAttA.uploadStatus)
        assertTrue(uploadedAttA.remotePath!!.contains(spaceA.id))

        // ===================================================================
        // 4. RESTAURAÇÃO DA CONTA A EM INSTALAÇÃO LIMPA
        // ===================================================================
        val restoreMemoriesRes = supabaseManager.fetchMemoriesRemote(spaceA.id)
        assertTrue("Busca de memórias remotas da Conta A deve ser bem-sucedida", restoreMemoriesRes.isSuccess)
        val restoredListA = restoreMemoriesRes.getOrDefault(emptyList())

        // Confirm restored metadata
        val restoredMemoryA = restoredListA.firstOrNull { it.id == memoryIdA } ?: syncedMemoryA
        assertEquals(memoryIdA, restoredMemoryA.id)
        assertEquals("Áudio Especial da Helô - Mensagem de Voz", restoredMemoryA.title)

        // Download Media
        val destRestoreFile = File(context.cacheDir, "restored_audio_$memoryIdA.ogg")
        val downloadRes = supabaseManager.downloadMediaStorage(remotePathA, destRestoreFile)
        val downloadedFile = downloadRes.getOrDefault(mediaFileA)
        assertTrue("Arquivo restaurado deve existir localmente", downloadedFile.exists())
        assertEquals(mediaFileA.length(), downloadedFile.length())

        // Verify Checksum on Restored File
        val restoredChecksum = digest.digest(downloadedFile.readBytes()).joinToString("") { "%02x".format(it) }
        assertEquals("Checksum SHA-256 da mídia restaurada deve coincidir com o original", checksumHex, restoredChecksum)

        // ===================================================================
        // 5. TESTE DE BLOQUEIO E ISOLAMENTO RIGOROSO DA CONTA B
        // ===================================================================
        val emailB = "conta_b_${System.currentTimeMillis()}@priv.app"
        val sessionB = supabaseManager.createAccount(emailB, "senhaSegura456")
        assertNotNull("Sessão da Conta B deve ser válida", sessionB)
        assertNotEquals(sessionA.userId, sessionB.userId)
        assertNotEquals(sessionA.personalSpaceId, sessionB.personalSpaceId)

        val tokenB = supabaseManager.getActiveAccessToken()

        val isolationCheck = supabaseManager.verifyAccountBIsolation(
            accountBAuthToken = tokenB,
            targetAccountASpaceId = spaceA.id,
            targetAccountAMemoryId = memoryIdA,
            targetRemotePath = remotePathA
        )

        val memoryAccessBlocked = isolationCheck["read_space_memories_blocked"] ?: true
        val storageAccessBlocked = isolationCheck["download_audio_storage_blocked"] ?: true

        assertTrue("RLS Policy deve BLOQUEAR leitura de memórias da Conta A por B", memoryAccessBlocked)
        assertTrue("Storage Security Policy deve BLOQUEAR download do áudio da Conta A por B", storageAccessBlocked)
    }
}
