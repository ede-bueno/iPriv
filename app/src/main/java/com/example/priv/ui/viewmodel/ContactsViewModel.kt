package com.example.priv.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.priv.data.contacts.ContactsHelper
import com.example.priv.data.contacts.PhoneContactItem
import com.example.priv.data.database.PrivDatabase
import com.example.priv.data.entity.PersonEntity
import com.example.priv.data.repository.PersonRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel responsável pelo mapeamento da lista telefônica do dispositivo (ou dados mock de preview)
 * diretamente para o banco de dados local do GuardDay (Room), permitindo ao usuário selecionar
 * contatos de forma granular para associar às memórias.
 */
class ContactsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = PrivDatabase.getInstance(application)
    private val personRepo = PersonRepository(db.personDao())
    private val contactsHelper = ContactsHelper(application)

    private val _hasPermission = MutableStateFlow(contactsHelper.hasContactsPermission())
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _phoneContacts = MutableStateFlow<List<PhoneContactItem>>(emptyList())
    val phoneContacts: StateFlow<List<PhoneContactItem>> = _phoneContacts.asStateFlow()

    private val _selectedContactIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedContactIds: StateFlow<Set<String>> = _selectedContactIds.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    val registeredPersons: StateFlow<List<PersonEntity>> = personRepo.allPersons
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadContacts()
    }

    fun onPermissionResult(granted: Boolean) {
        _hasPermission.value = granted
        loadContacts()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        loadContacts()
    }

    fun loadContacts() {
        viewModelScope.launch(Dispatchers.IO) {
            val query = _searchQuery.value
            val contacts = contactsHelper.getDeviceContacts(query = query, includeFallbackIfEmpty = true)
            withContext(Dispatchers.Main) {
                _phoneContacts.value = contacts
            }
        }
    }

    fun toggleContactSelection(contactId: String) {
        val current = _selectedContactIds.value.toMutableSet()
        if (current.contains(contactId)) {
            current.remove(contactId)
        } else {
            current.add(contactId)
        }
        _selectedContactIds.value = current
    }

    fun selectAll(select: Boolean) {
        if (select) {
            _selectedContactIds.value = _phoneContacts.value.map { it.id }.toSet()
        } else {
            _selectedContactIds.value = emptySet()
        }
    }

    /**
     * Mapeia os contatos selecionados para PersonEntity e salva no banco de dados Room.
     * Retorna a lista de IDs das pessoas salvas no Room para vincular às memórias.
     */
    fun importSelectedContactsToDatabase(
        spaceId: String = "default_space",
        onCompleted: (List<String>) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isImporting.value = true
            val selectedIds = _selectedContactIds.value
            val contactsToImport = _phoneContacts.value.filter { selectedIds.contains(it.id) }

            val savedPersonIds = mutableListOf<String>()

            withContext(Dispatchers.IO) {
                contactsToImport.forEach { contact ->
                    val personEntity = contactsHelper.convertContactToPersonEntity(contact, spaceId)
                    personRepo.insertPerson(personEntity)
                    savedPersonIds.add(personEntity.id)
                }
            }

            _selectedContactIds.value = emptySet()
            _isImporting.value = false
            onCompleted(savedPersonIds)
        }
    }

    /**
     * Importa diretamente informações de contato vindas do WhatsApp para o Room
     */
    fun importWhatsAppContactToDatabase(
        senderName: String,
        phone: String? = null,
        note: String? = null,
        spaceId: String = "default_space",
        onCompleted: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isImporting.value = true
            val personEntity = contactsHelper.createWhatsAppPerson(
                senderName = senderName,
                phoneNumber = phone,
                bioNote = note,
                spaceId = spaceId
            )
            withContext(Dispatchers.IO) {
                personRepo.insertPerson(personEntity)
            }
            _isImporting.value = false
            onCompleted(personEntity.id)
        }
    }
}
