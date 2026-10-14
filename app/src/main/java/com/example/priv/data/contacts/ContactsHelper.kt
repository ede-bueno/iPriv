package com.example.priv.data.contacts

import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.example.priv.data.entity.PersonEntity
import java.util.UUID

data class PhoneContactItem(
    val id: String,
    val name: String,
    val phoneNumber: String?,
    val photoUri: String?,
    val isFromDevice: Boolean = true
)

class ContactsHelper(private val context: Context) {

    fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Retorna contatos do aparelho via ContentResolver quando há permissão.
     * Caso o aparelho não possua contatos (ex: emulador limpo ou modo de teste/preview),
     * fornece contatos simulados para teste imediato de importação.
     */
    fun getDeviceContacts(query: String = "", includeFallbackIfEmpty: Boolean = true): List<PhoneContactItem> {
        val list = mutableListOf<PhoneContactItem>()

        if (hasContactsPermission()) {
            val contentResolver = context.contentResolver
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI
            )

            val selection = if (query.isNotBlank()) {
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} LIKE ? OR ${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
            } else null

            val selectionArgs = if (query.isNotBlank()) {
                arrayOf("%$query%", "%$query%")
            } else null

            try {
                contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} ASC LIMIT 60"
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val nameCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
                    val phoneCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val photoCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

                    val seenIds = mutableSetOf<String>()

                    while (cursor.moveToNext()) {
                        val contactId = if (idCol >= 0) cursor.getString(idCol) else UUID.randomUUID().toString()
                        val name = if (nameCol >= 0) cursor.getString(nameCol) else "Contato"
                        val phone = if (phoneCol >= 0) cursor.getString(phoneCol) else null
                        val photoUri = if (photoCol >= 0) cursor.getString(photoCol) else null

                        if (!name.isNullOrBlank() && !seenIds.contains(contactId)) {
                            seenIds.add(contactId)
                            list.add(
                                PhoneContactItem(
                                    id = contactId,
                                    name = name,
                                    phoneNumber = phone,
                                    photoUri = photoUri,
                                    isFromDevice = true
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Em caso de restrição de segurança do SO
            }
        }

        // Se a agenda estiver vazia (típico de ambiente de emulador de desenvolvimento),
        // complementa com contatos de demonstração para testes e preview rápidos
        if (list.isEmpty() && includeFallbackIfEmpty) {
            val mockContacts = getSampleMockContacts().filter {
                if (query.isBlank()) true
                else it.name.contains(query, ignoreCase = true) || (it.phoneNumber?.contains(query) == true)
            }
            list.addAll(mockContacts)
        }

        return list
    }

    fun getSampleMockContacts(): List<PhoneContactItem> {
        return listOf(
            PhoneContactItem(
                id = "mock_c1",
                name = "Heloísa BFF",
                phoneNumber = "+55 (11) 98765-4321",
                photoUri = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200",
                isFromDevice = false
            ),
            PhoneContactItem(
                id = "mock_c2",
                name = "Lucas Silveira (Violão)",
                phoneNumber = "+55 (11) 97654-3210",
                photoUri = "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=200",
                isFromDevice = false
            ),
            PhoneContactItem(
                id = "mock_c3",
                name = "Gabriel Costa (Biel)",
                phoneNumber = "+55 (21) 99887-6655",
                photoUri = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200",
                isFromDevice = false
            ),
            PhoneContactItem(
                id = "mock_c4",
                name = "Dona Ana (Mãe)",
                phoneNumber = "+55 (11) 98111-2233",
                photoUri = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=200",
                isFromDevice = false
            ),
            PhoneContactItem(
                id = "mock_c5",
                name = "Mariana Lima (Faculdade)",
                phoneNumber = "+55 (31) 98222-3344",
                photoUri = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=200",
                isFromDevice = false
            ),
            PhoneContactItem(
                id = "mock_c6",
                name = "Tio Roberto (Família)",
                phoneNumber = "+55 (19) 99333-4455",
                photoUri = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200",
                isFromDevice = false
            )
        )
    }

    fun convertContactToPersonEntity(contact: PhoneContactItem, spaceId: String = "default_space"): PersonEntity {
        val colorPalette = listOf("#7C4DFF", "#00D2D3", "#FF4B72", "#FFB703", "#8E3BEE", "#00F5D4")
        val assignedColor = colorPalette[Math.abs(contact.name.hashCode()) % colorPalette.size]

        return PersonEntity(
            id = "person_imported_${contact.id}_${System.currentTimeMillis()}",
            spaceId = spaceId,
            name = contact.name,
            nickname = contact.name.split(" ").firstOrNull() ?: contact.name,
            relationship = if (contact.phoneNumber != null) "Contato (${contact.phoneNumber})" else "Contato da Agenda",
            avatarUri = contact.photoUri ?: "",
            colorHex = assignedColor,
            bioNote = "Importado da lista telefônica para o GuardDay.",
            createdAt = System.currentTimeMillis()
        )
    }

    fun createWhatsAppPerson(
        senderName: String,
        phoneNumber: String? = null,
        bioNote: String? = null,
        spaceId: String = "default_space"
    ): PersonEntity {
        val colorPalette = listOf("#25D366", "#128C7E", "#075E54", "#7C4DFF", "#00D2D3")
        val assignedColor = colorPalette[Math.abs(senderName.hashCode()) % colorPalette.size]

        return PersonEntity(
            id = "person_wa_${UUID.randomUUID()}",
            spaceId = spaceId,
            name = senderName.trim().ifBlank { "Contato WhatsApp" },
            nickname = senderName.trim().split(" ").firstOrNull() ?: senderName,
            relationship = "Contato WhatsApp",
            avatarUri = "",
            colorHex = assignedColor,
            bioNote = bioNote ?: (if (!phoneNumber.isNullOrBlank()) "Vindo direto do WhatsApp ($phoneNumber)" else "Vindo direto do WhatsApp."),
            createdAt = System.currentTimeMillis()
        )
    }
}
