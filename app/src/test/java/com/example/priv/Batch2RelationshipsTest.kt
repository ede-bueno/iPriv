package com.example.priv

import com.example.priv.data.entity.*
import com.example.priv.data.model.AttachmentInput
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class Batch2RelationshipsTest {

    @Test
    fun testAttachmentInputDataModel() {
        val input = AttachmentInput(
            type = "IMAGE",
            uri = "content://media/external/images/media/1",
            fileName = "photo.jpg",
            fileSize = 102400L,
            mimeType = "image/jpeg",
            width = 1080,
            height = 1920
        )

        assertEquals("IMAGE", input.type)
        assertEquals("content://media/external/images/media/1", input.uri)
        assertEquals("photo.jpg", input.fileName)
        assertEquals(102400L, input.fileSize)
        assertEquals(1080, input.width)
        assertEquals(1920, input.height)
    }

    @Test
    fun testJunctionEntities() {
        val memoryId = UUID.randomUUID().toString()
        val personId1 = UUID.randomUUID().toString()
        val personId2 = UUID.randomUUID().toString()
        val groupId = UUID.randomUUID().toString()

        val p1Ref = MemoryPersonCrossRef(memoryId, personId1)
        val p2Ref = MemoryPersonCrossRef(memoryId, personId2)
        val gRef = MemoryGroupCrossRef(memoryId, groupId)
        val groupPersonRef = GroupPersonCrossRef(groupId, personId1)

        assertEquals(memoryId, p1Ref.memoryId)
        assertEquals(personId1, p1Ref.personId)
        assertEquals(personId2, p2Ref.personId)
        assertEquals(groupId, gRef.groupId)
        assertEquals(groupId, groupPersonRef.groupId)
        assertEquals(personId1, groupPersonRef.personId)
    }

    @Test
    fun testGroupEntityCreation() {
        val group = GroupEntity(
            id = "g_123",
            spaceId = "default_space",
            name = "Turma do 3º Ano",
            description = "Resenha do colégio",
            colorHex = "#8E3BEE",
            iconName = "School"
        )

        assertEquals("g_123", group.id)
        assertEquals("Turma do 3º Ano", group.name)
        assertEquals("#8E3BEE", group.colorHex)
        assertNull(group.deletedAt)
    }
}
